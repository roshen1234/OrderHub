package com.orderhub.order;

import java.util.*;

public enum OrderStatus {
    CREATED, PAID, SHIPPED, DELIVERED, CANCELLED;

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED = new EnumMap<>(OrderStatus.class);

    static {
        ALLOWED.put(CREATED,   EnumSet.of(PAID, CANCELLED));
        ALLOWED.put(PAID,      EnumSet.of(SHIPPED, CANCELLED));
        ALLOWED.put(SHIPPED,   EnumSet.of(DELIVERED));
        ALLOWED.put(DELIVERED, EnumSet.noneOf(OrderStatus.class));
        ALLOWED.put(CANCELLED, EnumSet.noneOf(OrderStatus.class));
    }

    public boolean canMoveTo(OrderStatus next) {
        return ALLOWED.get(this).contains(next);
    }
}
