CREATE TABLE external_identities (
     id         BIGINT AUTO_INCREMENT PRIMARY KEY,
     user_id    BIGINT       NOT NULL,
     provider   VARCHAR(20)  NOT NULL,
     subject    VARCHAR(255) NOT NULL,
     created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
     CONSTRAINT fk_identity_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
     CONSTRAINT uq_identity UNIQUE (provider, subject)
) ENGINE=InnoDB;