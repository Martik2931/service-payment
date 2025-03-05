package payment.service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import payment.service.domain.Payment;
import payment.service.domain.PaymentMode;
import payment.service.service.PaymentService;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/payment")
@Tag(name = "Payment Service", description = "Handles Payment Processing")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping
    @Operation(summary = "Process Payment and Deduct Stock")
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    public ResponseEntity<Payment> processPayment(@RequestParam UUID orderId,
                                                  @RequestParam UUID productId,
                                                  @RequestParam UUID customerId,
                                                  @RequestParam int quantity,
                                                  @RequestParam BigDecimal totalAmount,
                                                  @RequestParam PaymentMode paymentMode) {

        Payment payment = paymentService.processPayment(orderId, productId, customerId, quantity, totalAmount, paymentMode);
        return ResponseEntity.ok(payment);
    }

    @Operation(summary = "Retrieve Payment Details by Transaction ID")
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    @GetMapping("/{transactionId}")
    public ResponseEntity<?> getPaymentDetails(@PathVariable UUID transactionId) {
        return paymentService.getPaymentDetails(transactionId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
