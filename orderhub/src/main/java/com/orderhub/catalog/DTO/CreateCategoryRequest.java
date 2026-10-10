package com.orderhub.catalog.DTO;

import com.orderhub.catalog.Category;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCategoryRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must be at most 100 characters")
        String name,

        Long parentId            // optional: null means a top-level category
) {

}
