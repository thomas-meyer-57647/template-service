package de.innologic.templateservice.domain.entity;

import de.innologic.templateservice.domain.enums.TemplateScope;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(
    name = "template_family",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_template_family_business_key",
            columnNames = {"scope", "owner_tenant_id", "template_key", "channel", "locale", "category"}
        )
    },
    indexes = {
        @Index(name = "idx_template_family_owner", columnList = "owner_tenant_id"),
        @Index(name = "idx_template_family_lookup", columnList = "scope,template_key,channel,locale"),
        @Index(name = "idx_template_family_category", columnList = "category")
    }
)
public class TemplateFamily extends AuditableEntity {

    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "template_id", nullable = false, updatable = false, length = 36)
    private UUID templateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 16)
    private TemplateScope scope;

    @Column(name = "owner_tenant_id", length = 64)
    private String ownerTenantId;

    @Column(name = "template_key", nullable = false, length = 120)
    private String templateKey;

    @Column(name = "channel", nullable = false, length = 40)
    private String channel;

    @Column(name = "locale", nullable = false, length = 16)
    private String locale;

    @Column(name = "category", nullable = false, length = 80)
    private String category;

    @Column(name = "active_approved_version")
    private Integer activeApprovedVersion;

    public UUID getTemplateId() {
        return templateId;
    }

    public void setTemplateId(UUID templateId) {
        this.templateId = templateId;
    }

    public TemplateScope getScope() {
        return scope;
    }

    public void setScope(TemplateScope scope) {
        this.scope = scope;
    }

    public String getOwnerTenantId() {
        return ownerTenantId;
    }

    public void setOwnerTenantId(String ownerTenantId) {
        this.ownerTenantId = ownerTenantId;
    }

    public String getTemplateKey() {
        return templateKey;
    }

    public void setTemplateKey(String templateKey) {
        this.templateKey = templateKey;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getActiveApprovedVersion() {
        return activeApprovedVersion;
    }

    public void setActiveApprovedVersion(Integer activeApprovedVersion) {
        this.activeApprovedVersion = activeApprovedVersion;
    }
}
