package com.orderhub.cart;

import com.orderhub.cart.DTO.AddCartItemRequest;
import com.orderhub.cart.DTO.CartItemResponse;
import com.orderhub.cart.DTO.UpdateCartItemRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cart/items")
@RequiredArgsConstructor
public class CartItemController {

    private final CartItemService cartItemService;

    @PostMapping
    public ResponseEntity<CartItemResponse> add(@Valid @RequestBody AddCartItemRequest request,
                                                Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cartItemService.addItem(authentication.getName(), request));
    }

    @PatchMapping("/{itemId}")
    public CartItemResponse update(@PathVariable Long itemId,
                                   @Valid @RequestBody UpdateCartItemRequest request,
                                   Authentication authentication) {
        return cartItemService.updateItem(authentication.getName(), itemId, request);
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<Void> remove(@PathVariable Long itemId, Authentication authentication) {
        cartItemService.removeItem(authentication.getName(), itemId);
        return ResponseEntity.noContent().build();
    }
}
