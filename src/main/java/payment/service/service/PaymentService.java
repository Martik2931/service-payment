package payment.service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import payment.service.domain.*;
import payment.service.repository.PaymentRepository;
import payment.service.service.PaymentProcessor.CreditCardPaymentProcessor;
import payment.service.service.PaymentProcessor.PayPalPaymentProcessor;
import payment.service.service.PaymentProcessor.PaymentProcessor;
import payment.service.service.PaymentProcessor.WalletPaymentProcessor;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentService {

    private static final String PAYMENT_SUCCESS_TOPIC = "payment-success";
    private static final Logger LOGGER = LogManager.getLogger(PaymentService.class);

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private CreditCardPaymentProcessor creditCardPaymentProcessor;

    @Autowired
    private WalletPaymentProcessor walletPaymentProcessor;

    @Autowired
    private PayPalPaymentProcessor payPalPaymentProcessor;

    private final Map<PaymentMode, PaymentProcessor> paymentProcessorMap = new HashMap<>();

    @PostConstruct
    public void init() {
        paymentProcessorMap.put(PaymentMode.CREDIT_CARD, creditCardPaymentProcessor);
        paymentProcessorMap.put(PaymentMode.WALLET, walletPaymentProcessor);
        paymentProcessorMap.put(PaymentMode.PAYPAL, payPalPaymentProcessor);
    }


    @Value("${inventory.service.url}")
    private String inventoryServiceUrl;

    @Value("${inventory.service.name}")
    private String inventoryServiceName;

    @Value("${inventory.service.endpoint}")
    private String inventoryServiceEndpoint;

    @Transactional
    public Payment processPayment(UUID orderId, UUID productId, UUID customerId, int quantity, BigDecimal totalAmount, PaymentMode paymentMode) {
        if (!validateStock(productId, quantity)) {
            throw new RuntimeException("Insufficient Stock");
        }

        Payment payment = createPayment(orderId, customerId, totalAmount);

        PaymentProcessor paymentProcessor = paymentProcessorMap.get(paymentMode);
        payment = paymentProcessor.processPayment(payment, orderId, productId, customerId, totalAmount);
        sendPaymentEvent(payment, productId, quantity);
        return payment;
    }

    public Optional<Payment> getPaymentDetails(UUID transactionId) {
        return paymentRepository.findById(transactionId);
    }

    private Payment createPayment(UUID orderId, UUID customerId, BigDecimal totalAmount) {
        Payment payment = new Payment(orderId, customerId, totalAmount, PaymentStatus.PENDING);
        return paymentRepository.save(payment);
    }

    private void sendPaymentEvent(Payment payment, UUID productId, int quantity) {
        String paymentEventJson = String.format("{\"productId\":\"%s\",\"quantity\":%d,\"transactionId\":\"%s\"}",
                productId.toString(), quantity, payment.getTransactionId());
        kafkaTemplate.send(PAYMENT_SUCCESS_TOPIC, paymentEventJson);
    }

    private boolean validateStock(UUID productId, int quantity) {
        String jwtToken = getJwtTokenFromContext();
        String url = buildInventoryServiceUrl();

        HttpHeaders headers = buildHeaders(jwtToken);
        StockValidationRequest requestBody = new StockValidationRequest(productId, quantity);
        HttpEntity<StockValidationRequest> entity = new HttpEntity<>(requestBody, headers);

        return sendStockValidationRequest(url, entity);
    }

    private String buildInventoryServiceUrl() {
        return "http://" + inventoryServiceName + inventoryServiceEndpoint;
    }

    private HttpHeaders buildHeaders(String jwtToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", jwtToken);
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        return headers;
    }

    private boolean sendStockValidationRequest(String url, HttpEntity<StockValidationRequest> entity) {
        try {
            ResponseEntity<Boolean> response = restTemplate.exchange(url, HttpMethod.POST, entity, Boolean.class);
            return Boolean.TRUE.equals(response.getBody());
        } catch (Exception e) {
            handleStockValidationException(e);
            return false;
        }
    }

    private void handleStockValidationException(Exception e) {
        if (e instanceof org.springframework.web.client.HttpClientErrorException.Forbidden) {
            LOGGER.error("Forbidden: {}", e.getMessage());
        } else if (e instanceof org.springframework.web.client.HttpClientErrorException.Unauthorized) {
            LOGGER.error("Unauthorized: {}", e.getMessage());
        } else {
            LOGGER.error("Error while communicating with Inventory Service: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "payment-status", groupId = "inventory-group")
    public void listenForPaymentStatus(String paymentEventJson) {
        LOGGER.info("Received payment status update JSON: {}", paymentEventJson);

        try {
            // Deserialize the JSON string to PaymentEvent
            ObjectMapper objectMapper = new ObjectMapper();
            PaymentEvent paymentEvent = objectMapper.readValue(paymentEventJson, PaymentEvent.class);

            // Process the payment status
            updatePaymentStatus(paymentEvent);
        } catch (Exception e) {
            LOGGER.error("Error deserializing PaymentEvent from JSON: {}", e.getMessage());
            throw new RuntimeException("Error processing payment status", e);
        }
    }


    public void updatePaymentStatus(PaymentEvent paymentEvent) {
        LOGGER.info("Updated payment status for Product ID: {}, Status: {}", paymentEvent.getProductId(), paymentEvent.getPaymentStatus());
        Payment payment = paymentRepository.findById(paymentEvent.getTransactionId()).orElseThrow();
        payment.setPaymentStatus(paymentEvent.getPaymentStatus());
        paymentRepository.save(payment);
    }

    private String getJwtTokenFromContext() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String jwtToken = request.getHeader("Authorization");

        if (jwtToken == null || !jwtToken.startsWith("Bearer ")) {
            throw new RuntimeException("Missing or invalid Authorization header");
        }
        return jwtToken;
    }
}
