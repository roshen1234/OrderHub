CREATE TABLE categories (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    name      VARCHAR(100) NOT NULL,
    parent_id BIGINT NULL,
    CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES categories(id)
) ENGINE=InnoDB;

CREATE TABLE products (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(200)  NOT NULL,
    description TEXT,
    price       DECIMAL(12,2) NOT NULL,
    category_id BIGINT        NOT NULL,
    active      BOOLEAN       NOT NULL DEFAULT TRUE,
    version     BIGINT        NOT NULL DEFAULT 0,
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES categories(id),
    CONSTRAINT chk_product_price CHECK (price >= 0)
) ENGINE=InnoDB;

CREATE INDEX idx_products_name ON products(name);

CREATE TABLE inventory (
    product_id    BIGINT PRIMARY KEY,
    available_qty INT    NOT NULL DEFAULT 0,
    reserved_qty  INT    NOT NULL DEFAULT 0,
    version       BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_inventory_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT chk_inventory_available CHECK (available_qty >= 0),
    CONSTRAINT chk_inventory_reserved  CHECK (reserved_qty >= 0)
) ENGINE=InnoDB;