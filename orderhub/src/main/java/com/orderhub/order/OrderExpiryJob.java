package com.orderhub.order;

import com.orderhub.order.repository.OrderRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderExpiryJob {

    private final OrderRepo orderRepository;
    private final OrderService orderService;

    @Scheduled(fixedDelay = 60_000)          // runs a minute after the previous run finishes
    public void cancelExpiredOrders() {
        List<Long> ids = orderRepository.findExpiredIds(OrderStatus.CREATED, LocalDateTime.now());
        for (Long id : ids) {
            try {
                orderService.cancelExpired(id);          // one transaction per order
            } catch (Exception e) {
                log.warn("Could not expire order {}", id, e);   // one failure must not stop the rest
            }
        }
    }
}