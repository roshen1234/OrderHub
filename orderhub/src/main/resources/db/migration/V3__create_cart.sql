CREATE TABLE carts (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT    NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cart_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uq_cart_user UNIQUE (user_id)
) ENGINE=InnoDB;

CREATE TABLE cart_items (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    cart_id    BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity   INT    NOT NULL,
    CONSTRAINT fk_cart_item_cart    FOREIGN KEY (cart_id)    REFERENCES carts(id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_item_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT uq_cart_product UNIQUE (cart_id, product_id),
    CONSTRAINT chk_cart_quantity CHECK (quantity > 0)
) ENGINE=InnoDB;