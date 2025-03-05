package payment.service.domain;

import java.io.Serializable;
import java.util.UUID;

public class PaymentEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID productId;    // ID of the product being purchased
    private Integer quantity;  // Quantity of the product purchased
    private UUID transactionId;
    private PaymentStatus paymentStatus;

    // Default constructor
    public PaymentEvent() {
    }

    // Constructor with parameters
    public PaymentEvent(UUID productId, Integer quantity, UUID transactionId) {
        this.productId = productId;
        this.quantity = quantity;
        this.transactionId = transactionId;
    }

    // Getters and setters
    public UUID getProductId() {
        return productId;
    }

    public void setProductId(UUID productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(UUID transactionId) {
        this.transactionId = transactionId;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    @Override
    public String toString() {
        return "PaymentEvent{" +
                "productId=" + productId +
                ", quantity=" + quantity +
                '}';
    }
}
