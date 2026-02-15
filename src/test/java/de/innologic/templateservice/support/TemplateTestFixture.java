package de.innologic.templateservice.support;

import de.innologic.templateservice.domain.entity.TemplateFamily;
import de.innologic.templateservice.domain.entity.TemplateVersion;
import de.innologic.templateservice.domain.enums.RenderTarget;
import de.innologic.templateservice.domain.enums.TemplateScope;
import de.innologic.templateservice.domain.enums.TemplateStatus;
import de.innologic.templateservice.domain.repository.TemplateFamilyRepository;
import de.innologic.templateservice.domain.repository.TemplateVersionRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TemplateTestFixture {

    private final TemplateFamilyRepository familyRepository;
    private final TemplateVersionRepository versionRepository;

    public TemplateTestFixture(TemplateFamilyRepository familyRepository, TemplateVersionRepository versionRepository) {
        this.familyRepository = familyRepository;
        this.versionRepository = versionRepository;
    }

    public TemplateFamily createTenantFamily(String tenantId, String templateKey) {
        TemplateFamily family = new TemplateFamily();
        family.setTemplateId(UUID.randomUUID());
        family.setScope(TemplateScope.TENANT);
        family.setOwnerTenantId(tenantId);
        family.setTemplateKey(templateKey);
        family.setChannel("EMAIL");
        family.setLocale("de-DE");
        family.setCategory("BILLING");
        family.setCreatedBy("test");
        family.setUpdatedBy("test");
        return familyRepository.save(family);
    }

    public TemplateVersion createVersion(
        UUID templateId,
        int versionNo,
        TemplateStatus status,
        RenderTarget renderTarget,
        String bodyTpl
    ) {
        TemplateVersion version = new TemplateVersion();
        version.setVersionId(UUID.randomUUID());
        version.setTemplateId(templateId);
        version.setVersionNo(versionNo);
        version.setStatus(status);
        version.setRenderTarget(renderTarget);
        version.setSubjectTpl("Subject {{name}}");
        version.setBodyTpl(bodyTpl);
        version.setPlaceholders("[\"name\"]");
        version.setCreatedBy("test");
        version.setUpdatedBy("test");
        return versionRepository.save(version);
    }
}
