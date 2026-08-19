-- =============================================================================
--  E-Commerce Management System - MySQL schema
--
--  Hibernate creates these tables automatically at startup
--  (spring.jpa.hibernate.ddl-auto=update), so you do NOT need to run this file
--  to use the application.
--
--  It is kept as the readable, documented reference for the database design,
--  and for provisioning a schema manually (e.g. on a server where the app
--  should not have DDL rights).
--
--  To run it:
--      mysql -u root -p < database/schema.sql
--
--  Design notes worth reading before the DDL:
--
--   * Money is DECIMAL, never FLOAT/DOUBLE. Binary floating point cannot
--     represent 0.10 exactly, and rounding drift on currency is unacceptable.
--
--   * Orders SNAPSHOT their data. `order_items` copies the product name and
--     price, and `orders` copies the delivery address, at the moment of
--     checkout. If an admin later raises a price, renames a product, or the
--     customer deletes an address, past orders must still show what was
--     actually bought and where it was sent. This is deliberate duplication,
--     not a normalisation mistake.
--
--   * `order_status_history` is append-only. `orders.status` answers "where is
--     my order now"; this table answers "how did it get there and when", which
--     is what real order tracking requires.
--
--   * `products.average_rating` / `review_count` are derived values kept in
--     sync by the application whenever a review changes. They exist so that
--     listing pages can sort and filter by rating without aggregating the whole
--     reviews table on every request.
-- =============================================================================

CREATE DATABASE IF NOT EXISTS ecommerce_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE ecommerce_db;


-- -----------------------------------------------------------------------------
--  roles - the two authorities in the system
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS roles (
    id    BIGINT       NOT NULL AUTO_INCREMENT,
    name  VARCHAR(30)  NOT NULL COMMENT 'ROLE_CUSTOMER or ROLE_ADMIN',

    PRIMARY KEY (id),
    CONSTRAINT uk_roles_name UNIQUE (name)
) ENGINE=InnoDB;


-- -----------------------------------------------------------------------------
--  users - both customers and admins; capability is decided by user_roles
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    full_name     VARCHAR(120) NOT NULL,
    email         VARCHAR(180) NOT NULL,
    password      VARCHAR(100) NOT NULL COMMENT 'BCrypt hash - never plain text',
    phone_number  VARCHAR(20)  NOT NULL COMMENT 'E.164, e.g. +919876543210 (WhatsApp target)',
    enabled       BIT(1)       NOT NULL DEFAULT b'1',
    created_at    DATETIME(6)  NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email)
) ENGINE=InnoDB;


-- -----------------------------------------------------------------------------
--  user_roles - many-to-many join between users and roles
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_roles (
    user_id  BIGINT NOT NULL,
    role_id  BIGINT NOT NULL,

    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE
) ENGINE=InnoDB;


-- -----------------------------------------------------------------------------
--  categories - the six product sections (extensible by an admin)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS categories (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    name         VARCHAR(100) NOT NULL,
    slug         VARCHAR(120) NOT NULL COMMENT 'URL-safe id, e.g. kitchen-utensils',
    description  VARCHAR(500)     NULL,
    image_url    VARCHAR(500)     NULL,

    PRIMARY KEY (id),
    CONSTRAINT uk_categories_name UNIQUE (name),
    CONSTRAINT uk_categories_slug UNIQUE (slug)
) ENGINE=InnoDB;


-- -----------------------------------------------------------------------------
--  products
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS products (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    name            VARCHAR(200)  NOT NULL,
    description     TEXT              NULL,
    price           DECIMAL(10,2) NOT NULL,
    category_id     BIGINT        NOT NULL,
    brand           VARCHAR(100)      NULL,
    image_url       VARCHAR(500)      NULL,
    stock           INT           NOT NULL DEFAULT 0,
    average_rating  DECIMAL(3,2)  NOT NULL DEFAULT 0.00 COMMENT 'Derived from product_reviews',
    review_count    INT           NOT NULL DEFAULT 0    COMMENT 'Derived from product_reviews',
    date_added      DATETIME(6)   NOT NULL,
    active          BIT(1)        NOT NULL DEFAULT b'1' COMMENT 'Soft delete - keeps order history intact',

    PRIMARY KEY (id),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories (id),

    INDEX idx_products_category   (category_id),
    INDEX idx_products_name       (name),
    INDEX idx_products_date_added (date_added)
) ENGINE=InnoDB;


-- -----------------------------------------------------------------------------
--  addresses - a customer's saved delivery addresses
--  Orders do NOT reference this table for their shipping details; they copy it.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS addresses (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    user_id      BIGINT       NOT NULL,
    full_name    VARCHAR(120) NOT NULL,
    phone        VARCHAR(20)  NOT NULL,
    line1        VARCHAR(255) NOT NULL,
    line2        VARCHAR(255)     NULL,
    city         VARCHAR(100) NOT NULL,
    state        VARCHAR(100) NOT NULL,
    postal_code  VARCHAR(20)  NOT NULL,
    country      VARCHAR(100) NOT NULL DEFAULT 'India',
    is_default   BIT(1)       NOT NULL DEFAULT b'0',

    PRIMARY KEY (id),
    CONSTRAINT fk_addresses_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,

    INDEX idx_addresses_user (user_id)
) ENGINE=InnoDB;


-- -----------------------------------------------------------------------------
--  carts - exactly one per user
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS carts (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    user_id     BIGINT      NOT NULL,
    created_at  DATETIME(6) NOT NULL,
    updated_at  DATETIME(6) NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT uk_carts_user UNIQUE (user_id),
    CONSTRAINT fk_carts_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB;


-- -----------------------------------------------------------------------------
--  cart_items
--  The (cart_id, product_id) unique key is what makes "add the same product
--  again" increment a quantity instead of creating a duplicate line - enforced
--  by the database even if two requests race.
--  No price column: a cart always reflects the product's CURRENT price.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS cart_items (
    id          BIGINT NOT NULL AUTO_INCREMENT,
    cart_id     BIGINT NOT NULL,
    product_id  BIGINT NOT NULL,
    quantity    INT    NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT uk_cart_items_cart_product UNIQUE (cart_id, product_id),
    CONSTRAINT fk_cart_items_cart    FOREIGN KEY (cart_id)    REFERENCES carts (id)    ON DELETE CASCADE,
    CONSTRAINT fk_cart_items_product FOREIGN KEY (product_id) REFERENCES products (id)
) ENGINE=InnoDB;


-- -----------------------------------------------------------------------------
--  orders
--  The delivery_* columns are a SNAPSHOT taken at checkout. address_id is only a
--  soft breadcrumb to the saved address that was used, and may dangle.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS orders (
    id                    BIGINT        NOT NULL AUTO_INCREMENT,
    order_number          VARCHAR(40)   NOT NULL COMMENT 'Customer-facing reference, e.g. ORD-20260817-4821',
    user_id               BIGINT        NOT NULL,
    status                VARCHAR(30)   NOT NULL COMMENT 'ORDER_PLACED | ORDER_CONFIRMED | PROCESSING | DISPATCHED | OUT_FOR_DELIVERY | DELIVERED | CANCELLED',
    total_amount          DECIMAL(12,2) NOT NULL COMMENT 'Computed server-side; clients never send prices',

    address_id            BIGINT            NULL COMMENT 'Soft reference only - not a FK constraint',
    delivery_full_name    VARCHAR(120)  NOT NULL,
    delivery_phone        VARCHAR(20)   NOT NULL,
    delivery_line1        VARCHAR(255)  NOT NULL,
    delivery_line2        VARCHAR(255)      NULL,
    delivery_city         VARCHAR(100)  NOT NULL,
    delivery_state        VARCHAR(100)  NOT NULL,
    delivery_postal_code  VARCHAR(20)   NOT NULL,
    delivery_country      VARCHAR(100)  NOT NULL,

    placed_at             DATETIME(6)   NOT NULL,
    updated_at            DATETIME(6)   NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT uk_orders_order_number UNIQUE (order_number),
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users (id),

    INDEX idx_orders_user      (user_id),
    INDEX idx_orders_status    (status),
    INDEX idx_orders_placed_at (placed_at)
) ENGINE=InnoDB;


-- -----------------------------------------------------------------------------
--  order_items
--  product_name / unit_price are frozen at purchase time. product_id is
--  nullable so history survives a product being removed entirely.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS order_items (
    id                 BIGINT        NOT NULL AUTO_INCREMENT,
    order_id           BIGINT        NOT NULL,
    product_id         BIGINT            NULL,
    product_name       VARCHAR(200)  NOT NULL COMMENT 'Snapshot at purchase time',
    product_image_url  VARCHAR(500)      NULL,
    unit_price         DECIMAL(10,2) NOT NULL COMMENT 'Snapshot - price actually paid',
    quantity           INT           NOT NULL,
    line_total         DECIMAL(12,2) NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT fk_order_items_order   FOREIGN KEY (order_id)   REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE SET NULL,

    INDEX idx_order_items_order   (order_id),
    INDEX idx_order_items_product (product_id)
) ENGINE=InnoDB;


-- -----------------------------------------------------------------------------
--  order_status_history - append-only tracking timeline
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS order_status_history (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    order_id    BIGINT       NOT NULL,
    status      VARCHAR(30)  NOT NULL,
    note        VARCHAR(500)     NULL,
    changed_at  DATETIME(6)  NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT fk_osh_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,

    INDEX idx_osh_order (order_id)
) ENGINE=InnoDB;


-- -----------------------------------------------------------------------------
--  product_reviews
--  One review per customer per product, enforced by the unique key.
--  The stronger rule - the reviewer must have a DELIVERED order containing the
--  product - spans three tables and is enforced in ReviewService.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS product_reviews (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    product_id   BIGINT      NOT NULL,
    user_id      BIGINT      NOT NULL,
    rating       INT         NOT NULL COMMENT '1 to 5',
    review_text  TEXT            NULL,
    created_at   DATETIME(6) NOT NULL,
    updated_at   DATETIME(6)     NULL,

    PRIMARY KEY (id),
    CONSTRAINT uk_reviews_product_user UNIQUE (product_id, user_id),
    CONSTRAINT fk_reviews_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE,
    CONSTRAINT fk_reviews_user    FOREIGN KEY (user_id)    REFERENCES users (id)    ON DELETE CASCADE,
    CONSTRAINT chk_reviews_rating CHECK (rating BETWEEN 1 AND 5),

    INDEX idx_reviews_product (product_id)
) ENGINE=InnoDB;


-- -----------------------------------------------------------------------------
--  notifications - audit trail of every WhatsApp delivery attempt
--  A row is written for SENT, FAILED and SKIPPED alike, so the notification
--  history is complete even when Twilio is not configured.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS notifications (
    id                   BIGINT        NOT NULL AUTO_INCREMENT,
    user_id              BIGINT            NULL,
    order_id             BIGINT            NULL,
    channel              VARCHAR(30)   NOT NULL DEFAULT 'WHATSAPP',
    recipient            VARCHAR(30)   NOT NULL,
    message              TEXT          NOT NULL,
    trigger_status       VARCHAR(30)       NULL COMMENT 'Order status that triggered this message',
    status               VARCHAR(20)   NOT NULL COMMENT 'SENT | FAILED | SKIPPED',
    provider_message_id  VARCHAR(100)      NULL COMMENT 'Twilio message SID on success',
    error_message        VARCHAR(1000)     NULL,
    created_at           DATETIME(6)   NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT fk_notifications_user  FOREIGN KEY (user_id)  REFERENCES users (id)  ON DELETE SET NULL,
    CONSTRAINT fk_notifications_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,

    INDEX idx_notifications_user  (user_id),
    INDEX idx_notifications_order (order_id)
) ENGINE=InnoDB;


-- -----------------------------------------------------------------------------
--  Reference data
--  Roles must exist before any user can be created. Categories and the admin
--  account are seeded by the application (DataSeeder) so the admin password can
--  be BCrypt-hashed at runtime rather than stored here.
-- -----------------------------------------------------------------------------
INSERT IGNORE INTO roles (name) VALUES ('ROLE_CUSTOMER'), ('ROLE_ADMIN');
