package com.orderhub.inventory;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @PatchMapping("/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    public StockResponse adjust(@PathVariable Long productId,
                                @Valid @RequestBody AdjustStockRequest request) {
        return inventoryService.adjust(productId, request.delta());
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<StockResponse> getStocks(@RequestParam List<Long> productIds) {
        return inventoryService.getStocks(productIds);
    }

}
