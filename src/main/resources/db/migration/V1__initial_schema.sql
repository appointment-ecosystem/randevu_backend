-- Faz 0: Initial schema — Genel Yerel Hizmet ve Randevu Ekosistemi

CREATE TABLE users (
    id                  UUID PRIMARY KEY,
    full_name           VARCHAR(100) NOT NULL,
    email               VARCHAR(150) UNIQUE,
    phone               VARCHAR(20) NOT NULL UNIQUE,
    phone_verified      BOOLEAN NOT NULL DEFAULT FALSE,
    password_hash       VARCHAR(255) NOT NULL,
    role                VARCHAR(30) NOT NULL,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    profile_photo_url   VARCHAR(500),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_phone ON users (phone);

CREATE TABLE refresh_tokens (
    id            UUID PRIMARY KEY,
    user_id       UUID NOT NULL REFERENCES users (id),
    token_hash    VARCHAR(255) NOT NULL UNIQUE,
    expires_at    TIMESTAMPTZ NOT NULL,
    revoked       BOOLEAN NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_token_hash ON refresh_tokens (token_hash);

CREATE TABLE cities (
    id            UUID PRIMARY KEY,
    name          VARCHAR(100) NOT NULL UNIQUE,
    code          VARCHAR(10),
    is_active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE districts (
    id            UUID PRIMARY KEY,
    city_id       UUID NOT NULL REFERENCES cities (id),
    name          VARCHAR(100) NOT NULL,
    is_active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (city_id, name)
);

CREATE INDEX idx_districts_city_id ON districts (city_id);

CREATE TABLE neighborhoods (
    id             UUID PRIMARY KEY,
    district_id    UUID NOT NULL REFERENCES districts (id),
    name           VARCHAR(150) NOT NULL,
    is_active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (district_id, name)
);

CREATE INDEX idx_neighborhoods_district_id ON neighborhoods (district_id);

CREATE TABLE business_categories (
    id            UUID PRIMARY KEY,
    name          VARCHAR(100) NOT NULL UNIQUE,
    slug          VARCHAR(100) NOT NULL UNIQUE,
    description   TEXT,
    icon_url      VARCHAR(500),
    is_active     BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order    INT NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE businesses (
    id                  UUID PRIMARY KEY,
    owner_id            UUID NOT NULL REFERENCES users (id),
    name                VARCHAR(150) NOT NULL,
    slug                VARCHAR(150) NOT NULL UNIQUE,
    description         TEXT,
    phone               VARCHAR(20),
    email               VARCHAR(150),
    website             VARCHAR(300),
    city_id             UUID REFERENCES cities (id),
    district_id         UUID REFERENCES districts (id),
    neighborhood_id     UUID REFERENCES neighborhoods (id),
    address_line        VARCHAR(300),
    latitude            DOUBLE PRECISION,
    longitude           DOUBLE PRECISION,
    status              VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    rejection_reason    TEXT,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_businesses_owner_id ON businesses (owner_id);
CREATE INDEX idx_businesses_status ON businesses (status);
CREATE INDEX idx_businesses_city_district ON businesses (city_id, district_id);
CREATE INDEX idx_businesses_lat_lng ON businesses (latitude, longitude);
CREATE INDEX idx_businesses_slug ON businesses (slug);

CREATE TABLE business_category_map (
    business_id   UUID NOT NULL REFERENCES businesses (id),
    category_id   UUID NOT NULL REFERENCES business_categories (id),
    PRIMARY KEY (business_id, category_id)
);

CREATE TABLE business_photos (
    id              UUID PRIMARY KEY,
    business_id     UUID NOT NULL REFERENCES businesses (id),
    url             VARCHAR(500) NOT NULL,
    file_name       VARCHAR(255) NOT NULL,
    file_size       INT,
    mime_type       VARCHAR(100),
    is_cover        BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order      INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_business_photos_business_id ON business_photos (business_id);

CREATE TABLE services (
    id              UUID PRIMARY KEY,
    business_id     UUID NOT NULL REFERENCES businesses (id),
    name            VARCHAR(150) NOT NULL,
    description     TEXT,
    duration_min    INT NOT NULL,
    price           NUMERIC(10, 2) NOT NULL,
    currency        VARCHAR(10) NOT NULL DEFAULT 'TRY',
    image_url       VARCHAR(500),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order      INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_services_business_id ON services (business_id);
CREATE INDEX idx_services_business_active ON services (business_id, is_active);

CREATE TABLE staff (
    id                  UUID PRIMARY KEY,
    business_id         UUID NOT NULL REFERENCES businesses (id),
    user_id             UUID REFERENCES users (id),
    full_name           VARCHAR(100) NOT NULL,
    title               VARCHAR(100),
    bio                 TEXT,
    profile_photo_url   VARCHAR(500),
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order          INT NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_staff_business_id ON staff (business_id);

CREATE TABLE staff_services (
    staff_id    UUID NOT NULL REFERENCES staff (id),
    service_id  UUID NOT NULL REFERENCES services (id),
    PRIMARY KEY (staff_id, service_id)
);

CREATE TABLE working_hours (
    id              UUID PRIMARY KEY,
    business_id     UUID NOT NULL REFERENCES businesses (id),
    staff_id        UUID REFERENCES staff (id),
    day_of_week     SMALLINT NOT NULL,
    open_time       TIME NOT NULL,
    close_time      TIME NOT NULL,
    is_closed       BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (business_id, staff_id, day_of_week)
);

CREATE INDEX idx_working_hours_business_id ON working_hours (business_id);
CREATE INDEX idx_working_hours_staff_id ON working_hours (staff_id);

CREATE TABLE holidays (
    id              UUID PRIMARY KEY,
    business_id     UUID NOT NULL REFERENCES businesses (id),
    staff_id        UUID REFERENCES staff (id),
    date            DATE NOT NULL,
    reason          VARCHAR(200),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_holidays_business_date ON holidays (business_id, date);
CREATE INDEX idx_holidays_staff_date ON holidays (staff_id, date);

CREATE TABLE appointments (
    id                  UUID PRIMARY KEY,
    user_id             UUID NOT NULL REFERENCES users (id),
    business_id         UUID NOT NULL REFERENCES businesses (id),
    service_id          UUID NOT NULL REFERENCES services (id),
    staff_id            UUID REFERENCES staff (id),
    start_time          TIMESTAMPTZ NOT NULL,
    end_time            TIMESTAMPTZ NOT NULL,
    status              VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    price_snapshot      NUMERIC(10, 2) NOT NULL,
    currency            VARCHAR(10) NOT NULL DEFAULT 'TRY',
    notes               TEXT,
    cancellation_reason TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_appointments_user_id ON appointments (user_id);
CREATE INDEX idx_appointments_business_start ON appointments (business_id, start_time);
CREATE INDEX idx_appointments_staff_start ON appointments (staff_id, start_time);
CREATE INDEX idx_appointments_status ON appointments (status);
CREATE INDEX idx_appointments_business_status ON appointments (business_id, status);

CREATE UNIQUE INDEX idx_appointments_staff_start_unique
    ON appointments (staff_id, start_time)
    WHERE staff_id IS NOT NULL;

CREATE TABLE reviews (
    id                  UUID PRIMARY KEY,
    appointment_id      UUID NOT NULL UNIQUE REFERENCES appointments (id),
    user_id             UUID NOT NULL REFERENCES users (id),
    business_id         UUID NOT NULL REFERENCES businesses (id),
    rating              SMALLINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment             TEXT,
    is_visible          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_reviews_business_id ON reviews (business_id);
CREATE INDEX idx_reviews_business_visible ON reviews (business_id, is_visible);
CREATE INDEX idx_reviews_user_id ON reviews (user_id);

CREATE TABLE device_tokens (
    id              UUID PRIMARY KEY,
    user_id         UUID NOT NULL REFERENCES users (id),
    token           VARCHAR(500) NOT NULL UNIQUE,
    platform        VARCHAR(20) NOT NULL,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_device_tokens_user_id ON device_tokens (user_id);

CREATE TABLE payments (
    id                  UUID PRIMARY KEY,
    appointment_id      UUID NOT NULL REFERENCES appointments (id),
    user_id             UUID NOT NULL REFERENCES users (id),
    amount              NUMERIC(10, 2) NOT NULL,
    currency            VARCHAR(10) NOT NULL DEFAULT 'TRY',
    payment_type        VARCHAR(30) NOT NULL,
    status              VARCHAR(30) NOT NULL DEFAULT 'INITIATED',
    provider            VARCHAR(30),
    provider_reference  VARCHAR(255),
    provider_response   JSONB,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payments_appointment_id ON payments (appointment_id);
CREATE INDEX idx_payments_user_id ON payments (user_id);
CREATE INDEX idx_payments_status ON payments (status);
