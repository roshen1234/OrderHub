package com.orderhub.order;

import com.orderhub.cart.Cart;
import com.orderhub.cart.CartItem;
import com.orderhub.cart.Repository.CartRepository;
import com.orderhub.catalog.DTO.PageResponse;
import com.orderhub.catalog.Product;
import com.orderhub.common.exception.EmptyCartException;
import com.orderhub.common.exception.ResourceNotFoundException;
import com.orderhub.inventory.InventoryService;
import com.orderhub.order.DTO.OrderResponse;
import com.orderhub.order.DTO.OrderSummaryResponse;
import com.orderhub.order.entity.Order;
import com.orderhub.order.entity.OrderItem;
import com.orderhub.order.entity.OrderStatusHistory;
import com.orderhub.order.repository.OrderRepo;
import com.orderhub.order.repository.OrderStatusHistoryRepo;
import com.orderhub.user.User;
import com.orderhub.user.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderService {

    public final UserRepository userRepository;
    public final CartRepository cartRepository;
    public final InventoryService inventoryService;
    public final OrderRepo orderRepo;
    public final OrderStatusHistoryRepo orderStatusHistoryRepo;


    @Value("${orderhub.order.reservation-minutes}")
    private long reservationMinutes;

    @Transactional
    public OrderResponse checkout(String email)
    {
        User user=userRepository.findByEmail(email).orElseThrow(()->new UsernameNotFoundException("User not found"));

        Cart cart=cartRepository.findByUserIdForUpdate(user.getId()).orElseThrow(()->new EmptyCartException("Cart is empty"));

        if (cart.getItems().isEmpty()) {
            throw new EmptyCartException("Cart is empty");
        }

        //we sort to avoid deadlock when multiple request comes in
        List<CartItem> cartItem=cart.getItems().stream().sorted(Comparator.comparing(i->i.getProduct().getId())).toList();

        Order order = new Order();
        order.setUser(user);
        order.setStatus(OrderStatus.CREATED);
        order.setExpiresAt(LocalDateTime.now().plusMinutes(reservationMinutes));

        BigDecimal total = BigDecimal.ZERO;
        for(CartItem item:cartItem)
        {
            Product product = item.getProduct();
            if (!product.isActive()) {
                throw new ResourceNotFoundException("Product", product.getId());
            }

            inventoryService.reserve(product.getId(), item.getQuantity());

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(item.getQuantity());
            orderItem.setUnitPrice(product.getPrice());          // price frozen at purchase time
            order.getItems().add(orderItem);

            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        order.setTotalAmount(total);
        Order saved = orderRepo.save(order);

        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(saved);
        history.setFromStatus(null);
        history.setToStatus(OrderStatus.CREATED);
        orderStatusHistoryRepo.save(history);

        cart.getItems().clear();                                  // orphanRemoval deletes the cart items

        return OrderResponse.from(saved);


    }

    // OrderService
    @Transactional
    public void cancelExpired(Long orderId) {
        if (orderRepo.transition(orderId, OrderStatus.CREATED, OrderStatus.CANCELLED) == 1) {
            // only the request that won the status change releases stock
            Order order = orderRepo.findWithItemsById(orderId).orElseThrow();
            order.getItems().stream()
                    .sorted(Comparator.comparing(i -> i.getProduct().getId()))    // same lock order as checkout
                    .forEach(i -> inventoryService.release(i.getProduct().getId(), i.getQuantity()));

            // save a history row: CREATED → CANCELLED

            OrderStatusHistory orderStatusHistory=new OrderStatusHistory();
            orderStatusHistory.setFromStatus(OrderStatus.CREATED);
            orderStatusHistory.setToStatus(OrderStatus.CANCELLED);
            orderStatusHistory.setOrder(order);

            orderStatusHistoryRepo.save(orderStatusHistory);
        }
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> fetchMyOrders(String email, Pageable pageable) {
        return PageResponse.from(
                orderRepo.findByUserEmail(email, pageable).map(OrderSummaryResponse::from));
    }

    public OrderResponse fetchOrder(String email,Long id)
    {
        User user=userRepository.findByEmail(email).orElseThrow(()->new UsernameNotFoundException("User not found"));

        Order order=orderRepo.findAllByUserIdAndId(user.getId(),id).orElseThrow(()->new ResourceNotFoundException("Order",id));

        return OrderResponse.from(order);
    }

}
