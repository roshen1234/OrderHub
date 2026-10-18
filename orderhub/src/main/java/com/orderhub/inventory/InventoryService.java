package com.orderhub.inventory;

import com.orderhub.catalog.Product;
import com.orderhub.catalog.ProductService;
import com.orderhub.catalog.Repository.ProductRepository;
import com.orderhub.common.exception.InsufficientStockException;
import com.orderhub.common.exception.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    @Transactional
    public void createStock(Long productId, int qty) {
        Inventory inventory = new Inventory();
        inventory.setProductId(productId);
        inventory.setAvailableQty(qty);
        inventoryRepository.save(inventory);
    }

    @Transactional
    public StockResponse adjust(Long productId, int delta) {
        int updated = inventoryRepository.adjust(productId, delta);

        if (updated == 0) {
            // zero rows: either there is no stock row, or the change would make stock negative
            if (!inventoryRepository.existsById(productId)) {
                throw new ResourceNotFoundException("Inventory", productId);
            }
            throw new InsufficientStockException(productId);
        }

        Inventory inventory = inventoryRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory", productId));
        return StockResponse.from(inventory);
    }

    @Transactional
    public void reserve(Long productId, int qty) {
        if (inventoryRepository.reserve(productId, qty) == 0) {
            throw new InsufficientStockException(productId);
        }
    }

    @Transactional
    public void release(Long productId, int qty) {
        if (inventoryRepository.release(productId, qty) == 0) {
            throw new IllegalStateException(
                    "Cannot release " + qty + " units for product " + productId + ": not enough reserved stock");
        }
    }

    @Transactional
    public void confirmShipment(Long productId, int qty) {
        if (inventoryRepository.confirmShipment(productId, qty) == 0) {
            throw new IllegalStateException(
                    "Cannot release " + qty + " units for product " + productId + ": not enough reserved stock");
        }
    }

}
