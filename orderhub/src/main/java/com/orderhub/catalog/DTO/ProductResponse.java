package com.orderhub.catalog.DTO;

import com.orderhub.catalog.Product;

import java.math.BigDecimal;

public record ProductResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        Long categoryId,
        String categoryName,
        boolean active
) {
    public static ProductResponse from(Product p) {
        return new ProductResponse(p.getId(), p.getName(), p.getDescription(), p.getPrice(),
                p.getCategory().getId(), p.getCategory().getName(), p.isActive());
    }
}