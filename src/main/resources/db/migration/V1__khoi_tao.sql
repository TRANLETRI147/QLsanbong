-- =====================================================================
-- V1: Tao toan bo bang cho he thong quan ly san bong noi bo
-- (Chu san = OWNER, Nhan vien = STAFF)
-- Khong sua file nay sau khi da chay tren Render. Muon doi cau truc thi
-- tao file moi: V2__ten_thay_doi.sql
-- =====================================================================

CREATE TABLE users (
    id          BIGSERIAL    PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL,
    password    VARCHAR(100) NOT NULL,
    full_name   VARCHAR(100) NOT NULL,
    role        VARCHAR(20)  NOT NULL,
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL,
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT ck_users_role CHECK (role IN ('OWNER', 'STAFF'))
);

-- Moi dong la 1 san co the dat. Mot "san 7" gom 3 "san 5" (san_lon_id tro toi san 7).
CREATE TABLE san_bong (
    id           BIGSERIAL     PRIMARY KEY,
    ten          VARCHAR(100)  NOT NULL,
    loai         VARCHAR(10)   NOT NULL,
    gia_moi_gio  NUMERIC(12,0) NOT NULL,
    san_lon_id   BIGINT        REFERENCES san_bong (id),
    active       BOOLEAN       NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_san_bong_ten UNIQUE (ten),
    CONSTRAINT ck_san_bong_loai CHECK (loai IN ('SAN_5', 'SAN_7')),
    CONSTRAINT ck_san_bong_gia CHECK (gia_moi_gio >= 0),
    -- chi san 5 moi thuoc ve mot san 7
    CONSTRAINT ck_san_bong_nhom CHECK (san_lon_id IS NULL OR loai = 'SAN_5')
);
CREATE INDEX idx_san_bong_san_lon ON san_bong (san_lon_id);

CREATE TABLE booking (
    id                BIGSERIAL     PRIMARY KEY,
    san_bong_id       BIGINT        NOT NULL REFERENCES san_bong (id),
    customer_name     VARCHAR(100)  NOT NULL,
    customer_phone    VARCHAR(20),
    start_at          TIMESTAMP     NOT NULL,
    end_at            TIMESTAMP     NOT NULL,
    status            VARCHAR(20)   NOT NULL,
    court_fee         NUMERIC(12,0) NOT NULL,
    discount          NUMERIC(12,0) NOT NULL DEFAULT 0,
    reason            VARCHAR(255),
    created_by_id     BIGINT        NOT NULL REFERENCES users (id),
    created_at        TIMESTAMP     NOT NULL,
    approved_by_id    BIGINT        REFERENCES users (id),
    approved_at       TIMESTAMP,
    checked_in_by_id  BIGINT        REFERENCES users (id),
    checked_in_at     TIMESTAMP,
    CONSTRAINT ck_booking_time CHECK (end_at > start_at),
    CONSTRAINT ck_booking_money CHECK (court_fee >= 0 AND discount >= 0),
    CONSTRAINT ck_booking_status CHECK (status IN
        ('PENDING_APPROVAL', 'CONFIRMED', 'CHECKED_IN', 'COMPLETED', 'REJECTED', 'CANCELLED', 'NO_SHOW'))
);
CREATE INDEX idx_booking_san_time ON booking (san_bong_id, start_at, end_at);
CREATE INDEX idx_booking_start ON booking (start_at);
CREATE INDEX idx_booking_status ON booking (status);

-- Dich vu / do dung nhan vien them luc tinh tien (nuoc, thue ao bib...)
CREATE TABLE booking_item (
    id            BIGSERIAL     PRIMARY KEY,
    booking_id    BIGINT        NOT NULL REFERENCES booking (id) ON DELETE CASCADE,
    name          VARCHAR(100)  NOT NULL,
    quantity      INTEGER       NOT NULL,
    unit_price    NUMERIC(12,0) NOT NULL,
    added_by_id   BIGINT        NOT NULL REFERENCES users (id),
    created_at    TIMESTAMP     NOT NULL,
    CONSTRAINT ck_booking_item_qty CHECK (quantity > 0),
    CONSTRAINT ck_booking_item_price CHECK (unit_price >= 0)
);
CREATE INDEX idx_booking_item_booking ON booking_item (booking_id);

-- Ghi chu cua nhan vien/chu san. he_thong = true la dong nhat ky tu dong.
CREATE TABLE booking_note (
    id          BIGSERIAL     PRIMARY KEY,
    booking_id  BIGINT        NOT NULL REFERENCES booking (id) ON DELETE CASCADE,
    author_id   BIGINT        NOT NULL REFERENCES users (id),
    content     VARCHAR(1000) NOT NULL,
    he_thong    BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP     NOT NULL
);
CREATE INDEX idx_booking_note_booking ON booking_note (booking_id);

-- Moi lan thu tien la 1 dong. Doanh thu tinh tu bang nay (tien thuc thu).
CREATE TABLE payment (
    id             BIGSERIAL     PRIMARY KEY,
    booking_id     BIGINT        NOT NULL REFERENCES booking (id) ON DELETE CASCADE,
    amount         NUMERIC(12,0) NOT NULL,
    method         VARCHAR(20)   NOT NULL,
    paid_at        TIMESTAMP     NOT NULL,
    received_by_id BIGINT        NOT NULL REFERENCES users (id),
    voided         BOOLEAN       NOT NULL DEFAULT FALSE,
    voided_by_id   BIGINT        REFERENCES users (id),
    voided_at      TIMESTAMP,
    CONSTRAINT ck_payment_amount CHECK (amount > 0),
    CONSTRAINT ck_payment_method CHECK (method IN ('CASH', 'BANK_TRANSFER'))
);
CREATE INDEX idx_payment_booking ON payment (booking_id);
CREATE INDEX idx_payment_paid_at ON payment (paid_at);

-- Thong bao gui len chu san
CREATE TABLE notification (
    id             BIGSERIAL     PRIMARY KEY,
    type           VARCHAR(30)   NOT NULL,
    booking_id     BIGINT        REFERENCES booking (id) ON DELETE CASCADE,
    message        VARCHAR(500)  NOT NULL,
    created_by_id  BIGINT        NOT NULL REFERENCES users (id),
    created_at     TIMESTAMP     NOT NULL,
    read_at        TIMESTAMP
);
CREATE INDEX idx_notification_unread ON notification (read_at);
