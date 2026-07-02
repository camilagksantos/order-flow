CREATE TABLE roles (
                       id         BIGINT          NOT NULL AUTO_INCREMENT,
                       name       VARCHAR(50)     NOT NULL UNIQUE,
                       PRIMARY KEY (id)
);

CREATE TABLE users (
                       id                  BIGINT          NOT NULL AUTO_INCREMENT,
                       email               VARCHAR(150)    NOT NULL UNIQUE,
                       password            VARCHAR(255)    NOT NULL,
                       active              BOOLEAN         NOT NULL DEFAULT TRUE,
                       created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                       PRIMARY KEY (id)
);

CREATE TABLE user_roles (
                            user_id     BIGINT  NOT NULL,
                            role_id     BIGINT  NOT NULL,
                            PRIMARY KEY (user_id, role_id),
                            CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id),
                            CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(id)
);

CREATE TABLE categories (
                            id                  BIGINT          NOT NULL AUTO_INCREMENT,
                            name                VARCHAR(80)     NOT NULL,
                            slug                VARCHAR(80)     NOT NULL UNIQUE,
                            description         VARCHAR(500),
                            parent_category_id  BIGINT,
                            active              BOOLEAN         NOT NULL DEFAULT TRUE,
                            PRIMARY KEY (id),
                            CONSTRAINT fk_categories_parent FOREIGN KEY (parent_category_id) REFERENCES categories(id)
);

CREATE TABLE products (
                          id                  BIGINT              NOT NULL AUTO_INCREMENT,
                          name                VARCHAR(150)        NOT NULL,
                          description         VARCHAR(2000),
                          sku                 VARCHAR(50)         NOT NULL UNIQUE,
                          price               DECIMAL(10,2)       NOT NULL,
                          cost_price          DECIMAL(10,2),
                          stock_quantity      INT                 NOT NULL DEFAULT 0,
                          reserved_quantity   INT                 NOT NULL DEFAULT 0,
                          category_id         BIGINT              NOT NULL,
                          status              VARCHAR(20)         NOT NULL DEFAULT 'ACTIVE',
                          weight_kg           DECIMAL(8,3),
                          width_cm            DECIMAL(8,2),
                          height_cm           DECIMAL(8,2),
                          depth_cm            DECIMAL(8,2),
                          created_at          DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          updated_at          DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                          PRIMARY KEY (id),
                          CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories(id)
);

CREATE TABLE product_images (
                                id          BIGINT          NOT NULL AUTO_INCREMENT,
                                product_id  BIGINT          NOT NULL,
                                url         VARCHAR(500)    NOT NULL,
                                alt_text    VARCHAR(200),
                                is_primary  BOOLEAN         NOT NULL DEFAULT FALSE,
                                sort_order  INT             NOT NULL DEFAULT 0,
                                PRIMARY KEY (id),
                                CONSTRAINT fk_product_images_product FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE TABLE customers (
                           id              BIGINT          NOT NULL AUTO_INCREMENT,
                           user_id         BIGINT          NOT NULL UNIQUE,
                           name            VARCHAR(120)    NOT NULL,
                           email           VARCHAR(150)    NOT NULL UNIQUE,
                           cpf             VARCHAR(11)     NOT NULL UNIQUE,
                           phone           VARCHAR(20),
                           birth_date      DATE,
                           status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
                           created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                           PRIMARY KEY (id),
                           CONSTRAINT fk_customers_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE addresses (
                           id              BIGINT          NOT NULL AUTO_INCREMENT,
                           customer_id     BIGINT          NOT NULL,
                           label           VARCHAR(50),
                           street          VARCHAR(200)    NOT NULL,
                           number          VARCHAR(20)     NOT NULL,
                           complement      VARCHAR(100),
                           neighborhood    VARCHAR(100)    NOT NULL,
                           city            VARCHAR(100)    NOT NULL,
                           state           CHAR(2)         NOT NULL,
                           zip_code        CHAR(8)         NOT NULL,
                           country         CHAR(2)         NOT NULL DEFAULT 'BR',
                           is_default      BOOLEAN         NOT NULL DEFAULT FALSE,
                           PRIMARY KEY (id),
                           CONSTRAINT fk_addresses_customer FOREIGN KEY (customer_id) REFERENCES customers(id)
);

CREATE TABLE carts (
                       id          VARCHAR(36)     NOT NULL,
                       customer_id BIGINT,
                       session_id  VARCHAR(100),
                       status      VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
                       coupon_code VARCHAR(50),
                       created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                       expires_at  DATETIME        NOT NULL,
                       PRIMARY KEY (id),
                       CONSTRAINT fk_carts_customer FOREIGN KEY (customer_id) REFERENCES customers(id)
);

CREATE TABLE cart_items (
                            id              VARCHAR(36)     NOT NULL,
                            cart_id         VARCHAR(36)     NOT NULL,
                            product_id      BIGINT          NOT NULL,
                            product_name    VARCHAR(150)    NOT NULL,
                            product_sku     VARCHAR(50)     NOT NULL,
                            unit_price      DECIMAL(10,2)   NOT NULL,
                            quantity        INT             NOT NULL,
                            PRIMARY KEY (id),
                            CONSTRAINT fk_cart_items_cart FOREIGN KEY (cart_id) REFERENCES carts(id)
);

CREATE TABLE orders (
                        id                  VARCHAR(36)     NOT NULL,
                        order_number        VARCHAR(20)     NOT NULL UNIQUE,
                        customer_id         BIGINT          NOT NULL,
                        customer_name       VARCHAR(120)    NOT NULL,
                        customer_email      VARCHAR(150)    NOT NULL,
                        customer_cpf        VARCHAR(11)     NOT NULL,
                        customer_phone      VARCHAR(20),
                        status              VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
                        shipping_street     VARCHAR(200)    NOT NULL,
                        shipping_number     VARCHAR(20)     NOT NULL,
                        shipping_complement VARCHAR(100),
                        shipping_neighborhood VARCHAR(100)  NOT NULL,
                        shipping_city       VARCHAR(100)    NOT NULL,
                        shipping_state      CHAR(2)         NOT NULL,
                        shipping_zip_code   CHAR(8)         NOT NULL,
                        shipping_country    CHAR(2)         NOT NULL DEFAULT 'BR',
                        subtotal            DECIMAL(10,2)   NOT NULL,
                        shipping_cost       DECIMAL(10,2)   NOT NULL DEFAULT 0.00,
                        discount_amount     DECIMAL(10,2)   NOT NULL DEFAULT 0.00,
                        total_amount        DECIMAL(10,2)   NOT NULL,
                        coupon_code         VARCHAR(50),
                        payment_method      VARCHAR(20)     NOT NULL,
                        tracking_code       VARCHAR(100),
                        notes               VARCHAR(500),
                        cancel_reason       VARCHAR(500),
                        idempotency_key     VARCHAR(36)     NOT NULL UNIQUE,
                        created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        paid_at             DATETIME,
                        preparing_at        DATETIME,
                        shipped_at          DATETIME,
                        delivered_at        DATETIME,
                        cancelled_at        DATETIME,
                        PRIMARY KEY (id),
                        CONSTRAINT fk_orders_customer FOREIGN KEY (customer_id) REFERENCES customers(id)
);

CREATE TABLE order_items (
                             id              VARCHAR(36)     NOT NULL,
                             order_id        VARCHAR(36)     NOT NULL,
                             product_id      BIGINT          NOT NULL,
                             product_name    VARCHAR(150)    NOT NULL,
                             product_sku     VARCHAR(50)     NOT NULL,
                             unit_price      DECIMAL(10,2)   NOT NULL,
                             quantity        INT             NOT NULL,
                             subtotal        DECIMAL(10,2)   NOT NULL,
                             PRIMARY KEY (id),
                             CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id)
);

CREATE TABLE payments (
                          id                  VARCHAR(36)     NOT NULL,
                          order_id            VARCHAR(36)     NOT NULL UNIQUE,
                          amount              DECIMAL(10,2)   NOT NULL,
                          method              VARCHAR(20)     NOT NULL,
                          status              VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
                          transaction_id      VARCHAR(100),
                          gateway_response    TEXT,
                          card_last_four      CHAR(4),
                          card_brand          VARCHAR(20),
                          pix_key             VARCHAR(100),
                          pix_qr_code         TEXT,
                          boleto_bar_code     VARCHAR(100),
                          boleto_url          VARCHAR(500),
                          idempotency_key     VARCHAR(36)     NOT NULL UNIQUE,
                          attempt_count       INT             NOT NULL DEFAULT 0,
                          processed_at        DATETIME,
                          created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          PRIMARY KEY (id),
                          CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders(id)
);

CREATE TABLE stock_movements (
                                 id                  BIGINT          NOT NULL AUTO_INCREMENT,
                                 product_id          BIGINT          NOT NULL,
                                 order_id            VARCHAR(36),
                                 type                VARCHAR(20)     NOT NULL,
                                 quantity            INT             NOT NULL,
                                 previous_quantity   INT             NOT NULL,
                                 current_quantity    INT             NOT NULL,
                                 reason              VARCHAR(255),
                                 created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 PRIMARY KEY (id),
                                 CONSTRAINT fk_stock_movements_product FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE TABLE outbox_events (
                               id              VARCHAR(36)     NOT NULL,
                               aggregate_id    VARCHAR(36)     NOT NULL,
                               aggregate_type  VARCHAR(50)     NOT NULL,
                               event_type      VARCHAR(50)     NOT NULL,
                               payload         TEXT            NOT NULL,
                               status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
                               attempts        INT             NOT NULL DEFAULT 0,
                               processed_at    DATETIME,
                               created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               PRIMARY KEY (id)
);

CREATE TABLE processed_events (
                                  id          VARCHAR(36)     NOT NULL,
                                  created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  PRIMARY KEY (id)
);