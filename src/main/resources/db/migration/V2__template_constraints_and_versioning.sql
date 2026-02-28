ALTER TABLE template_family
    DROP INDEX uk_template_family_business_key;

ALTER TABLE template_family
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE template_family
    ADD CONSTRAINT uk_template_family_business_key UNIQUE (scope, owner_tenant_id, template_key, channel, locale);

ALTER TABLE template_version
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
