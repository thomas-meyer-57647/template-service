package de.innologic.templateservice.domain.repository;

import de.innologic.templateservice.domain.entity.TemplateFamily;
import de.innologic.templateservice.domain.entity.TemplateVersion;
import de.innologic.templateservice.domain.enums.TemplateScope;
import de.innologic.templateservice.domain.enums.TemplateStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TemplateVersionRepository extends JpaRepository<TemplateVersion, UUID> {

    List<TemplateVersion> findByTemplateIdOrderByVersionNoDesc(UUID templateId);

    Page<TemplateVersion> findByTemplateId(UUID templateId, Pageable pageable);

    Optional<TemplateVersion> findByTemplateIdAndVersionNo(UUID templateId, Integer versionNo);

    Optional<TemplateVersion> findFirstByTemplateIdAndStatusOrderByVersionNoDesc(UUID templateId, TemplateStatus status);
    Page<TemplateVersion> findByTemplateIdAndStatus(UUID templateId, TemplateStatus status, Pageable pageable);

    Optional<TemplateVersion> findFirstByTemplateIdOrderByVersionNoDesc(UUID templateId);

    void deleteByTemplateId(UUID templateId);

    @Query("""
        select tv from TemplateVersion tv
        join TemplateFamily tf on tf.templateId = tv.templateId
        where tv.templateId = :templateId
          and tv.versionNo = :versionNo
          and (
              (tf.scope = :globalScope and tf.ownerTenantId = :globalOwner)
              or (tf.scope = :tenantScope and tf.ownerTenantId = :tenantId)
          )
        """)
    Optional<TemplateVersion> findVisibleVersion(
            @Param("templateId") UUID templateId,
            @Param("versionNo") Integer versionNo,
            @Param("tenantScope") TemplateScope tenantScope,
            @Param("tenantId") String tenantId,
            @Param("globalScope") TemplateScope globalScope,
            @Param("globalOwner") String globalOwner
    );
}
