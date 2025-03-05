package payment.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpServerErrorException;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import payment.service.domain.Payment;
import payment.service.domain.PaymentMode;
import payment.service.domain.PaymentStatus;
import payment.service.repository.PaymentRepository;
import payment.service.service.PaymentService;

import static org.mockito.Mockito.when;

@SpringBootTest
class ServiceApplicationTests {

	@Mock
	private PaymentRepository paymentRepository;

	@Mock
	private RestTemplate restTemplate;

	@InjectMocks
	private PaymentService paymentService;

	@Value("${inventory.service.url}")
	private String inventoryServiceUrl;

	private UUID orderId;
	private UUID productId;
	private UUID customerId;
	private UUID transactionId;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		orderId = UUID.randomUUID();
		productId = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");
		customerId = UUID.randomUUID();
		transactionId = UUID.randomUUID();
	}

	@Test
	void testProcessPayment_Success() {
		String requestUrl = inventoryServiceUrl + "/validate?productId=" + productId + "&quantity=5";

		when(restTemplate.postForObject(eq(requestUrl), isNull(), eq(Boolean.class))).thenReturn(true);
		when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Payment payment = paymentService.processPayment(orderId, productId, customerId, 5, BigDecimal.valueOf(200.00), PaymentMode.CREDIT_CARD);

		assertNotNull(payment);
		assertEquals(PaymentStatus.SUCCESS, payment.getPaymentStatus());
		assertEquals(BigDecimal.valueOf(100.00), payment.getTotalAmount());
		verify(paymentRepository, times(1)).save(any(Payment.class));
	}

	@Test
	void testProcessPayment_InsufficientStock() {
		String requestUrl = inventoryServiceUrl + "/validate?productId=" + productId + "&quantity=10";

		when(restTemplate.postForObject(eq(requestUrl), isNull(), eq(Boolean.class))).thenReturn(false);

		Exception exception = assertThrows(RuntimeException.class, () -> {
			paymentService.processPayment(orderId, productId, customerId, 10, BigDecimal.valueOf(200.00), PaymentMode.CREDIT_CARD);
		});

		assertEquals("Insufficient Stock", exception.getMessage());
		verify(paymentRepository, never()).save(any(Payment.class));
	}

	@Test
	void testProcessPayment_InventoryServiceDown() {
		String requestUrl = inventoryServiceUrl + "/validate?productId=" + productId + "&quantity=10";

		when(restTemplate.postForObject(eq(requestUrl), isNull(), eq(Boolean.class)))
				.thenThrow(new HttpServerErrorException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR));

		Exception exception = assertThrows(RuntimeException.class, () -> {
			paymentService.processPayment(orderId, productId, customerId, 10, BigDecimal.valueOf(200.00), PaymentMode.CREDIT_CARD);
		});

		assertTrue(exception.getMessage().contains("500 INTERNAL_SERVER_ERROR"));
		verify(paymentRepository, never()).save(any(Payment.class));
	}

	@Test
	void testGetPaymentDetails_Found() {
		Payment payment = new Payment();
		payment.setTransactionId(transactionId);
		payment.setOrderId(orderId);
		payment.setCustomerId(customerId);
		payment.setTotalAmount(BigDecimal.valueOf(200.00));
		payment.setPaymentStatus(PaymentStatus.SUCCESS);

		when(paymentRepository.findById(transactionId)).thenReturn(Optional.of(payment));

		Optional<Payment> result = paymentService.getPaymentDetails(transactionId);

		assertTrue(result.isPresent());
		assertEquals(PaymentStatus.SUCCESS, result.get().getPaymentStatus());
	}

	@Test
	void testGetPaymentDetails_NotFound() {
		when(paymentRepository.findById(transactionId)).thenReturn(Optional.empty());

		Optional<Payment> result = paymentService.getPaymentDetails(transactionId);

		assertFalse(result.isPresent());
	}

}
