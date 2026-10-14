package com.orderhub.order.repository;

import com.orderhub.order.entity.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderStatusHistoryRepo extends JpaRepository<OrderStatusHistory, Long> {

}
