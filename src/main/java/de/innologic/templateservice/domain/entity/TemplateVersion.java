package de.innologic.templateservice.domain.entity;

import de.innologic.templateservice.domain.enums.RenderTarget;
import de.innologic.templateservice.domain.enums.TemplateStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(
        name = "template_version",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_template_version_per_family", columnNames = {"template_id", "version_no"})
        },
        indexes = {
                @Index(name = "idx_template_version_template_status", columnList = "template_id,status"),
                @Index(name = "idx_template_version_render_target", columnList = "render_target")
        }
)
public class TemplateVersion extends AuditableEntity {

    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "version_id", nullable = false, updatable = false, length = 36)
    private UUID versionId;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "template_id", nullable = false, length = 36)
    private UUID templateId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "template_id",
            nullable = false,
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_template_version_template_family")
    )
    private TemplateFamily templateFamily;

    @Column(name = "version_no", nullable = false)
    private Integer versionNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private TemplateStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "render_target", nullable = false, length = 16)
    private RenderTarget renderTarget;

    // Flyway: subject_tpl TEXT
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "subject_tpl", columnDefinition = "text")
    private String subjectTpl;

    // Flyway: body_tpl LONGTEXT NOT NULL
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "body_tpl", nullable = false, columnDefinition = "longtext")
    private String bodyTpl;

    // Flyway: placeholders JSON (bei MariaDB 10.4 effektiv LONGTEXT-Alias)
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "placeholders", columnDefinition = "json")
    private String placeholders;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public UUID getVersionId() {
        return versionId;
    }

    public void setVersionId(UUID versionId) {
        this.versionId = versionId;
    }

    public UUID getTemplateId() {
        return templateId;
    }

    public void setTemplateId(UUID templateId) {
        this.templateId = templateId;
    }

    public TemplateFamily getTemplateFamily() {
        return templateFamily;
    }

    public void setTemplateFamily(TemplateFamily templateFamily) {
        this.templateFamily = templateFamily;
    }

    public Integer getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(Integer versionNo) {
        this.versionNo = versionNo;
    }

    public TemplateStatus getStatus() {
        return status;
    }

    public void setStatus(TemplateStatus status) {
        this.status = status;
    }

    public RenderTarget getRenderTarget() {
        return renderTarget;
    }

    public void setRenderTarget(RenderTarget renderTarget) {
        this.renderTarget = renderTarget;
    }

    public String getSubjectTpl() {
        return subjectTpl;
    }

    public void setSubjectTpl(String subjectTpl) {
        this.subjectTpl = subjectTpl;
    }

    public String getBodyTpl() {
        return bodyTpl;
    }

    public void setBodyTpl(String bodyTpl) {
        this.bodyTpl = bodyTpl;
    }

    public String getPlaceholders() {
        return placeholders;
    }

    public void setPlaceholders(String placeholders) {
        this.placeholders = placeholders;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
