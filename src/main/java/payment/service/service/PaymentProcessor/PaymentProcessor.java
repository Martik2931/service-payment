package payment.service.service.PaymentProcessor;

import payment.service.domain.Payment;

import java.math.BigDecimal;
import java.util.UUID;

public interface PaymentProcessor {
    Payment processPayment(Payment payment, UUID orderId, UUID productId, UUID customerId, BigDecimal totalAmount);
}