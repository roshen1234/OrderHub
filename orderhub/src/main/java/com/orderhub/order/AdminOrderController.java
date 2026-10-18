package com.orderhub.order;

import com.orderhub.catalog.DTO.PageResponse;
import com.orderhub.order.DTO.OrderResponse;
import com.orderhub.order.DTO.OrderSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")            // applies to every method in the class
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping
    public PageResponse<OrderResponse> getAllOrders(@PageableDefault(size = 10,sort ="createdAt", direction = Sort.Direction.DESC) Pageable pageable)
    {
        return orderService.getAllOrders(pageable);
    }

    @GetMapping("{id}")
    public OrderResponse getOrderById(@PathVariable("id") Long id)
    {
        return orderService.getOrderById(id);
    }

    @PostMapping("/{id}/ship")
    public ResponseEntity<Void> ship(@PathVariable Long id) {
        orderService.shipOrder(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/deliver")
    public ResponseEntity<Void> deliver(@PathVariable Long id) {
        orderService.deliverOrder(id);
        return ResponseEntity.noContent().build();
    }


}