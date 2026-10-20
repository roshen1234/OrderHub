CREATE TABLE addresses (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id        BIGINT       NOT NULL,
    label          VARCHAR(50)  NULL,
    recipient_name VARCHAR(100) NOT NULL,
    phone          VARCHAR(20)  NOT NULL,
    line1          VARCHAR(200) NOT NULL,
    line2          VARCHAR(200) NULL,
    city           VARCHAR(100) NOT NULL,
    state          VARCHAR(100) NOT NULL,
    postal_code    VARCHAR(20)  NOT NULL,
    country        VARCHAR(2)   NOT NULL,
    is_default     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_address_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_addresses_user ON addresses(user_id);

ALTER TABLE orders
    ADD COLUMN ship_name        VARCHAR(100) NULL,
    ADD COLUMN ship_phone       VARCHAR(20)  NULL,
    ADD COLUMN ship_line1       VARCHAR(200) NULL,
    ADD COLUMN ship_line2       VARCHAR(200) NULL,
    ADD COLUMN ship_city        VARCHAR(100) NULL,
    ADD COLUMN ship_state       VARCHAR(100) NULL,
    ADD COLUMN ship_postal_code VARCHAR(20)  NULL,
    ADD COLUMN ship_country     VARCHAR(2)   NULL;