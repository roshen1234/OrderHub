package com.orderhub.catalog.DTO;

import com.orderhub.catalog.Category;

public record CategoryResponse(Long id, String name, Long parentId) {

    public static CategoryResponse from(Category c) {
        return new CategoryResponse(c.getId(), c.getName(),
                c.getParent() == null ? null : c.getParent().getId());
    }
}
