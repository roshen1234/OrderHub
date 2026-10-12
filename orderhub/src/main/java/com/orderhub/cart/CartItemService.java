package com.orderhub.cart;

import com.orderhub.cart.DTO.AddCartItemRequest;
import com.orderhub.cart.DTO.CartItemResponse;
import com.orderhub.cart.DTO.UpdateCartItemRequest;
import com.orderhub.cart.Repository.CartItemRepository;
import com.orderhub.cart.Repository.CartRepository;
import com.orderhub.catalog.Product;
import com.orderhub.catalog.Repository.ProductRepository;
import com.orderhub.common.exception.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartItemService {

    private final CartService cartService;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    @Transactional
    public CartItemResponse addItem(String email, AddCartItemRequest request) {
        Cart cart = cartService.getOrCreateCart(email);

        Product product = productRepository.findByIdAndActiveTrue(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", request.productId()));

        CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId())
                .orElseGet(() -> {
                    CartItem n = new CartItem();
                    n.setCart(cart);
                    n.setProduct(product);
                    n.setQuantity(0);
                    return n;
                });

        item.setQuantity(item.getQuantity() + request.quantity());   // add to the existing line
        return CartItemResponse.from(cartItemRepository.save(item));
    }

    @Transactional
    public CartItemResponse updateItem(String email, Long itemId, UpdateCartItemRequest request) {
        CartItem item = cartItemRepository.findByIdAndCartUserEmail(itemId, email)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item", itemId));
        item.setQuantity(request.quantity());
        return CartItemResponse.from(item);
    }

    @Transactional
    public void removeItem(String email, Long itemId) {
        CartItem item = cartItemRepository.findByIdAndCartUserEmail(itemId, email)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item", itemId));
        cartItemRepository.delete(item);
    }
}
