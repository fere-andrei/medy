CREATE TABLE organizations (
    id         UUID PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    slug       VARCHAR(255) NOT NULL,
    created_at TIMESTAMP    NOT NULL,
    CONSTRAINT uq_organizations_name UNIQUE (name),
    CONSTRAINT uq_organizations_slug UNIQUE (slug)
);
