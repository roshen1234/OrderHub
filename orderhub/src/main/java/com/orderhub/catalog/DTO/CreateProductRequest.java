package com.orderhub.catalog.DTO;

import com.orderhub.catalog.Category;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreateProductRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 200)
        String name,

        String description,

        @NotNull(message = "Price is required")
        @PositiveOrZero(message = "Price cannot be negative")
        @Digits(integer = 10, fraction = 2, message = "Price allows at most 2 decimal places")
        BigDecimal price,

        @NotNull(message = "Category is required")
        Long categoryId,

        @Min(value = 0, message = "Stock cannot be negative")
        int initialStock
) {
}
