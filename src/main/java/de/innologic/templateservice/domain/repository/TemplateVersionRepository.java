package de.innologic.templateservice.domain.repository;

import de.innologic.templateservice.domain.entity.TemplateVersion;
import de.innologic.templateservice.domain.enums.TemplateStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TemplateVersionRepository extends JpaRepository<TemplateVersion, UUID> {

    List<TemplateVersion> findByTemplateIdOrderByVersionNoDesc(UUID templateId);

    Optional<TemplateVersion> findByTemplateIdAndVersionNo(UUID templateId, Integer versionNo);

    Optional<TemplateVersion> findFirstByTemplateIdAndStatusOrderByVersionNoDesc(UUID templateId, TemplateStatus status);

    Optional<TemplateVersion> findFirstByTemplateIdOrderByVersionNoDesc(UUID templateId);

    void deleteByTemplateId(UUID templateId);
}
