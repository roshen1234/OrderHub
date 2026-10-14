package com.orderhub.order;

import com.orderhub.catalog.DTO.PageResponse;
import com.orderhub.order.DTO.OrderResponse;
import com.orderhub.order.DTO.OrderSummaryResponse;
import com.orderhub.order.entity.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    public final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> checkout(Authentication authentication)
    {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.checkout(authentication.getName()));
    }

    @GetMapping
    public PageResponse<OrderSummaryResponse> myOrders(
            Authentication authentication,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return orderService.fetchMyOrders(authentication.getName(), pageable);
    }

    @GetMapping("{id}")
    public ResponseEntity<OrderResponse> fetchOrder(@PathVariable Long id,Authentication authentication)
    {
        return ResponseEntity.status(HttpStatus.OK).body(orderService.fetchOrder(authentication.getName(),id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancel(@PathVariable Long id, Authentication authentication) {
        orderService.cancelOrder(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }

}
