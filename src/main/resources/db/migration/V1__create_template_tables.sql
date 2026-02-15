CREATE TABLE template_family (
    template_id VARCHAR(36) NOT NULL,
    scope VARCHAR(16) NOT NULL,
    owner_tenant_id VARCHAR(64) NULL,
    template_key VARCHAR(120) NOT NULL,
    channel VARCHAR(40) NOT NULL,
    locale VARCHAR(16) NOT NULL,
    category VARCHAR(80) NOT NULL,
    active_approved_version INT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by VARCHAR(100) NULL,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    updated_by VARCHAR(100) NULL,
    CONSTRAINT pk_template_family PRIMARY KEY (template_id),
    CONSTRAINT chk_template_family_scope CHECK (scope IN ('GLOBAL', 'TENANT')),
    CONSTRAINT uk_template_family_business_key UNIQUE (scope, owner_tenant_id, template_key, channel, locale, category)
);

CREATE INDEX idx_template_family_owner ON template_family (owner_tenant_id);
CREATE INDEX idx_template_family_lookup ON template_family (scope, template_key, channel, locale);
CREATE INDEX idx_template_family_category ON template_family (category);

CREATE TABLE template_version (
    version_id VARCHAR(36) NOT NULL,
    template_id VARCHAR(36) NOT NULL,
    version_no INT NOT NULL,
    status VARCHAR(16) NOT NULL,
    render_target VARCHAR(16) NOT NULL,
    subject_tpl TEXT NULL,
    body_tpl LONGTEXT NOT NULL,
    placeholders JSON NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by VARCHAR(100) NULL,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    updated_by VARCHAR(100) NULL,
    CONSTRAINT pk_template_version PRIMARY KEY (version_id),
    CONSTRAINT fk_template_version_template_family
        FOREIGN KEY (template_id) REFERENCES template_family (template_id)
        ON DELETE CASCADE,
    CONSTRAINT uk_template_version_per_family UNIQUE (template_id, version_no),
    CONSTRAINT chk_template_version_status CHECK (status IN ('DRAFT', 'APPROVED', 'DEPRECATED', 'ARCHIVED')),
    CONSTRAINT chk_template_version_render_target CHECK (render_target IN ('TEXT', 'HTML'))
);

CREATE INDEX idx_template_version_template_status ON template_version (template_id, status);
CREATE INDEX idx_template_version_render_target ON template_version (render_target);
