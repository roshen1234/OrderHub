package com.orderhub.catalog;

import com.orderhub.catalog.DTO.CategoryResponse;
import com.orderhub.catalog.DTO.CreateCategoryRequest;
import com.orderhub.catalog.DTO.CreateProductRequest;
import com.orderhub.catalog.DTO.ProductResponse;
import com.orderhub.catalog.Repository.CategoryRepository;
import com.orderhub.catalog.Repository.ProductRepository;
import com.orderhub.common.exception.ResourceNotFoundException;
import com.orderhub.inventory.Inventory;
import com.orderhub.inventory.InventoryRepository;
import com.orderhub.inventory.InventoryService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;

    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        Category category=categoryRepository.findById(request.categoryId())
                .orElseThrow(()->new ResourceNotFoundException("Category",request.categoryId()));

        Product product=new Product();
        product.setCategory(category);
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        Product saved=productRepository.save(product);

        inventoryService.createStock(saved.getId(),request.initialStock());

        return ProductResponse.from(saved);


    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        Product product = productRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        return ProductResponse.from(product);
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<ProductResponse> findAll() {
        return productRepository.findByActiveTrue().stream()
                .map(ProductResponse::from)
                .toList();
    }

    @Transactional
    public void deactivate(Long id) {
        Product product = productRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        product.setActive(false);        // soft delete; the save happens when the transaction commits
    }
}
