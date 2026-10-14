package com.orderhub.common.exception;

import com.orderhub.order.OrderStatus;

public class InvalidOrderTransitionException extends RuntimeException {
    public InvalidOrderTransitionException(OrderStatus from, OrderStatus to) {
        super("Cannot move order from " + from + " to " + to);
    }
}
