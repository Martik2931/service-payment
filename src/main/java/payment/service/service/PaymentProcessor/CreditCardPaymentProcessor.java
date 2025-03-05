package payment.service.service.PaymentProcessor;

import org.springframework.stereotype.Service;
import payment.service.domain.Payment;
import payment.service.domain.PaymentStatus;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class CreditCardPaymentProcessor implements PaymentProcessor {

    @Override
    public Payment processPayment(Payment payment, UUID orderId, UUID productId, UUID customerId, BigDecimal totalAmount) {
        // Logic for processing Credit Card payment
        payment.setPaymentStatus(PaymentStatus.SUCCESS); // Set the payment status as SUCCESS
        return payment;
    }
}

