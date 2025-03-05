package payment.service.domain;

import java.util.UUID;

public class StockValidationRequest {

    private UUID productId;
    private int quantity;

    // Getters and Setters

    public StockValidationRequest(UUID productId, int quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }

    public UUID getProductId() {
        return productId;
    }

    public void setProductId(UUID productId) {
        this.productId = productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
