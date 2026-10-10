package com.orderhub.catalog;

import com.orderhub.catalog.DTO.CategoryResponse;
import com.orderhub.catalog.DTO.CreateCategoryRequest;
import com.orderhub.catalog.Repository.CategoryRepository;
import com.orderhub.common.exception.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional
    public CategoryResponse create(CreateCategoryRequest request) {
        Category category = new Category();
        category.setName(request.name().trim());

        if (request.parentId() != null) {
            Category parent = categoryRepository.findById(request.parentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", request.parentId()));
            category.setParent(parent);
        }

        Category saved = categoryRepository.save(category);
        return CategoryResponse.from(saved);
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<CategoryResponse> findAll() {
        return categoryRepository.findAll().stream()
                .map(category -> CategoryResponse.from(category))
                .toList();
    }
}