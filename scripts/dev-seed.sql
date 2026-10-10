USE orderhub;

-- 1. Top-level categories
INSERT INTO categories (name, parent_id) VALUES
  ('Electronics', NULL),
  ('Books', NULL),
  ('Home & Kitchen', NULL);

-- 2. Child categories (parent looked up by name)
INSERT INTO categories (name, parent_id)
SELECT v.name, p.id
FROM (
  SELECT 'Phones' AS name,      'Electronics' AS parent
  UNION ALL SELECT 'Laptops',      'Electronics'
  UNION ALL SELECT 'Fiction',      'Books'
  UNION ALL SELECT 'Programming',  'Books'
  UNION ALL SELECT 'Cookware',     'Home & Kitchen'
) v
JOIN categories p ON p.name = v.parent;

-- 3. Products (category looked up by name)
INSERT INTO products (name, description, price, category_id, active)
SELECT v.name, v.description, v.price, c.id, v.active
FROM (
  SELECT 'iPhone 15' AS name, 'Apple smartphone, 128 GB' AS description, 799.00 AS price, 'Phones' AS cat, TRUE AS active
  UNION ALL SELECT 'Galaxy S24',          'Samsung smartphone, 256 GB',        749.00, 'Phones',      TRUE
  UNION ALL SELECT 'Pixel 8',             'Google smartphone, 128 GB',         599.00, 'Phones',      TRUE
  UNION ALL SELECT 'MacBook Air 13',      'Apple laptop, M3 chip, 16 GB RAM', 1099.00, 'Laptops',     TRUE
  UNION ALL SELECT 'ThinkPad X1 Carbon',  'Lenovo business laptop, 32 GB RAM',1599.00, 'Laptops',     TRUE
  UNION ALL SELECT 'The Pragmatic Programmer', 'Classic book on software craft', 39.99, 'Programming', TRUE
  UNION ALL SELECT 'Clean Code',          'A handbook of agile software craftsmanship', 34.50, 'Programming', TRUE
  UNION ALL SELECT 'The Hobbit',          'Fantasy novel by J.R.R. Tolkien',   12.99, 'Fiction',      TRUE
  UNION ALL SELECT 'Non-stick Frying Pan','28 cm pan with a glass lid',        24.90, 'Cookware',     TRUE
  UNION ALL SELECT 'Cast Iron Skillet',   '26 cm pre-seasoned skillet',        44.00, 'Cookware',     TRUE
  UNION ALL SELECT 'Old Model Phone',     'Discontinued item for soft-delete tests', 99.00, 'Phones', FALSE
) v
JOIN categories c ON c.name = v.cat;

-- 4. Inventory (one row per product, same id as the product)
INSERT INTO inventory (product_id, available_qty, reserved_qty)
SELECT p.id, v.qty, 0
FROM (
  SELECT 'iPhone 15' AS name, 25 AS qty
  UNION ALL SELECT 'Galaxy S24',               40
  UNION ALL SELECT 'Pixel 8',                  15
  UNION ALL SELECT 'MacBook Air 13',           10
  UNION ALL SELECT 'ThinkPad X1 Carbon',        5
  UNION ALL SELECT 'The Pragmatic Programmer', 100
  UNION ALL SELECT 'Clean Code',               80
  UNION ALL SELECT 'The Hobbit',                0
  UNION ALL SELECT 'Non-stick Frying Pan',     60
  UNION ALL SELECT 'Cast Iron Skillet',         1
  UNION ALL SELECT 'Old Model Phone',           3
) v
JOIN products p ON p.name = v.name;