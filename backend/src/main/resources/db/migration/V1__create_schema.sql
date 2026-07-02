CREATE TABLE role (
                      id      BIGINT      NOT NULL AUTO_INCREMENT,
                      name    VARCHAR(50) NOT NULL UNIQUE,
                      PRIMARY KEY (id)
);

CREATE TABLE user (
                      id          BIGINT          NOT NULL AUTO_INCREMENT,
                      email       VARCHAR(150)    NOT NULL UNIQUE,
                      password    VARCHAR(255)    NOT NULL,
                      active      BOOLEAN         NOT NULL DEFAULT TRUE,
                      created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
                      updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                      PRIMARY KEY (id)
);

CREATE TABLE user_role (
                           user_id BIGINT NOT NULL,
                           role_id BIGINT NOT NULL,
                           PRIMARY KEY (user_id, role_id),
                           CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES user(id),
                           CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES role(id)
);

CREATE TABLE category (
                          id      BIGINT      NOT NULL AUTO_INCREMENT,
                          name    VARCHAR(80) NOT NULL,
                          PRIMARY KEY (id)
);

CREATE TABLE product (
                         id                  BIGINT          NOT NULL AUTO_INCREMENT,
                         name                VARCHAR(150)    NOT NULL,
                         description         VARCHAR(2000),
                         sku                 VARCHAR(50)     NOT NULL UNIQUE,
                         price               DECIMAL(10,2)   NOT NULL,
                         stock_quantity      INT             NOT NULL DEFAULT 0,
                         reserved_quantity   INT             NOT NULL DEFAULT 0,
                         category_id         BIGINT          NOT NULL,
                         image_url           VARCHAR(500),
                         status              VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
                         created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                         PRIMARY KEY (id),
                         CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES category(id)
);

CREATE TABLE customer (
                          id          BIGINT          NOT NULL AUTO_INCREMENT,
                          user_id     BIGINT          NOT NULL UNIQUE,
                          name        VARCHAR(120)    NOT NULL,
                          email       VARCHAR(150)    NOT NULL UNIQUE,
                          nif         CHAR(9)         NOT NULL UNIQUE,
                          phone       VARCHAR(20),
                          status      VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
                          created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                          PRIMARY KEY (id),
                          CONSTRAINT fk_customer_user FOREIGN KEY (user_id) REFERENCES user(id)
);

CREATE TABLE address (
                         id              BIGINT          NOT NULL AUTO_INCREMENT,
                         customer_id     BIGINT          NOT NULL,
                         street          VARCHAR(200)    NOT NULL,
                         number          VARCHAR(20)     NOT NULL,
                         complement      VARCHAR(100),
                         neighborhood    VARCHAR(100)    NOT NULL,
                         city            VARCHAR(100)    NOT NULL,
                         district        VARCHAR(50)     NOT NULL,
                         postal_code     CHAR(8)         NOT NULL,
                         country         CHAR(2)         NOT NULL DEFAULT 'PT',
                         is_default      BOOLEAN         NOT NULL DEFAULT FALSE,
                         PRIMARY KEY (id),
                         CONSTRAINT fk_address_customer FOREIGN KEY (customer_id) REFERENCES customer(id)
);

CREATE TABLE cart (
                      id          VARCHAR(36)     NOT NULL,
                      customer_id BIGINT          NOT NULL,
                      status      VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
                      created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
                      updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                      PRIMARY KEY (id),
                      CONSTRAINT fk_cart_customer FOREIGN KEY (customer_id) REFERENCES customer(id)
);

CREATE TABLE cart_item (
                           id              VARCHAR(36)     NOT NULL,
                           cart_id         VARCHAR(36)     NOT NULL,
                           product_id      BIGINT          NOT NULL,
                           product_name    VARCHAR(150)    NOT NULL,
                           product_sku     VARCHAR(50)     NOT NULL,
                           unit_price      DECIMAL(10,2)   NOT NULL,
                           quantity        INT             NOT NULL,
                           PRIMARY KEY (id),
                           CONSTRAINT fk_cart_item_cart FOREIGN KEY (cart_id) REFERENCES cart(id)
);

CREATE TABLE shop_order (
                            id                  VARCHAR(36)     NOT NULL,
                            order_number        VARCHAR(20)     NOT NULL UNIQUE,
                            customer_id         BIGINT          NOT NULL,
                            status              VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
                            subtotal            DECIMAL(10,2)   NOT NULL,
                            shipping_cost       DECIMAL(10,2)   NOT NULL DEFAULT 0.00,
                            discount_amount     DECIMAL(10,2)   NOT NULL DEFAULT 0.00,
                            total_amount        DECIMAL(10,2)   NOT NULL,
                            payment_method      VARCHAR(20)     NOT NULL,
                            tracking_code       VARCHAR(100),
                            cancel_reason       VARCHAR(500),
                            idempotency_key     VARCHAR(36)     NOT NULL UNIQUE,
                            created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                            paid_at             DATETIME,
                            shipped_at          DATETIME,
                            delivered_at        DATETIME,
                            cancelled_at        DATETIME,
                            PRIMARY KEY (id),
                            CONSTRAINT fk_shop_order_customer FOREIGN KEY (customer_id) REFERENCES customer(id)
);

CREATE TABLE order_item (
                            id              VARCHAR(36)     NOT NULL,
                            order_id        VARCHAR(36)     NOT NULL,
                            product_id      BIGINT          NOT NULL,
                            product_name    VARCHAR(150)    NOT NULL,
                            product_sku     VARCHAR(50)     NOT NULL,
                            unit_price      DECIMAL(10,2)   NOT NULL,
                            quantity        INT             NOT NULL,
                            subtotal        DECIMAL(10,2)   NOT NULL,
                            PRIMARY KEY (id),
                            CONSTRAINT fk_order_item_order FOREIGN KEY (order_id) REFERENCES shop_order(id)
);

CREATE TABLE payment (
                         id              VARCHAR(36)     NOT NULL,
                         order_id        VARCHAR(36)     NOT NULL UNIQUE,
                         amount          DECIMAL(10,2)   NOT NULL,
                         method          VARCHAR(20)     NOT NULL,
                         status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
                         transaction_id  VARCHAR(100),
                         card_last_four  CHAR(4),
                         card_brand      VARCHAR(20),
                         mbway_phone     VARCHAR(20),
                         mb_entity       CHAR(5),
                         mb_reference    CHAR(9),
                         attempt_count   INT             NOT NULL DEFAULT 0,
                         processed_at    DATETIME,
                         created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         PRIMARY KEY (id),
                         CONSTRAINT fk_payment_order FOREIGN KEY (order_id) REFERENCES shop_order(id)
);

CREATE TABLE outbox_event (
                              id          VARCHAR(36)     NOT NULL,
                              event_type  VARCHAR(50)     NOT NULL,
                              payload     TEXT            NOT NULL,
                              status      VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
                              created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              PRIMARY KEY (id)
);

CREATE TABLE processed_event (
                                 id          VARCHAR(36)     NOT NULL,
                                 created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 PRIMARY KEY (id)
);