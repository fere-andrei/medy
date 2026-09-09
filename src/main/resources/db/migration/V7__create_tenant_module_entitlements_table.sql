CREATE TABLE tenant_module_entitlements (
    id           UUID PRIMARY KEY,
    tenant_id    UUID         NOT NULL REFERENCES organizations (id),
    module_code  VARCHAR(50)  NOT NULL,
    enabled      BOOLEAN      NOT NULL DEFAULT true,
    valid_until  TIMESTAMP
);

CREATE UNIQUE INDEX uq_tenant_module_entitlements_tenant_module
    ON tenant_module_entitlements (tenant_id, module_code);
