package com.orderhub.inventory;

import jakarta.validation.constraints.NotNull;

public record StockResponse(Long productId, int availableQty, int reservedQty) {
    public static StockResponse from(Inventory i) {
        return new StockResponse(i.getProductId(), i.getAvailableQty(), i.getReservedQty());
    }
}


