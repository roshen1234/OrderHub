CREATE TABLE payments (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id        BIGINT        NOT NULL,
    user_id         BIGINT        NOT NULL,
    amount          DECIMAL(12,2) NOT NULL,
    status          VARCHAR(20)   NOT NULL,
    idempotency_key VARCHAR(100)  NOT NULL,
    provider_ref    VARCHAR(100)  NULL,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payment_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_payment_user  FOREIGN KEY (user_id)  REFERENCES users(id),
    CONSTRAINT uq_payment_idem UNIQUE (user_id, idempotency_key),
    CONSTRAINT chk_payment_status CHECK (status IN ('PENDING','SUCCESS','FAILED'))
) ENGINE=InnoDB;

CREATE TABLE processed_events (
    event_id     VARCHAR(100) PRIMARY KEY,
    processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;