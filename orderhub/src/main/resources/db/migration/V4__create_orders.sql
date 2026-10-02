CREATE TABLE orders (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT        NOT NULL,
    status       VARCHAR(20)   NOT NULL,
    total_amount DECIMAL(12,2) NOT NULL,
    expires_at   TIMESTAMP     NULL,
    created_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT chk_order_status CHECK (status IN ('CREATED','PAID','SHIPPED','DELIVERED','CANCELLED')),
    CONSTRAINT chk_order_total CHECK (total_amount >= 0)
) ENGINE=InnoDB;

CREATE INDEX idx_orders_user ON orders(user_id, created_at);
CREATE INDEX idx_orders_expiry ON orders(status, expires_at);

CREATE TABLE order_items (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id   BIGINT        NOT NULL,
    product_id BIGINT        NOT NULL,
    quantity   INT           NOT NULL,
    unit_price DECIMAL(12,2) NOT NULL,
    CONSTRAINT fk_order_item_order   FOREIGN KEY (order_id)   REFERENCES orders(id),
    CONSTRAINT fk_order_item_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT chk_order_item_qty CHECK (quantity > 0)
) ENGINE=InnoDB;

CREATE TABLE order_status_history (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id    BIGINT      NOT NULL,
    from_status VARCHAR(20) NULL,
    to_status   VARCHAR(20) NOT NULL,
    changed_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_history_order FOREIGN KEY (order_id) REFERENCES orders(id)
) ENGINE=InnoDB;