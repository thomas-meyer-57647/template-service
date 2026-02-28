package de.innologic.templateservice.domain.repository;

import de.innologic.templateservice.domain.entity.TemplateFamily;
import de.innologic.templateservice.domain.enums.TemplateScope;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface TemplateFamilyRepository extends JpaRepository<TemplateFamily, UUID> {

    Optional<TemplateFamily> findFirstByScopeAndOwnerTenantIdAndTemplateKeyAndChannelAndLocale(
            TemplateScope scope,
            String ownerTenantId,
            String templateKey,
            String channel,
            String locale
    );

    boolean existsByScopeAndOwnerTenantIdAndTemplateKeyAndChannelAndLocale(
            TemplateScope scope,
            String ownerTenantId,
            String templateKey,
            String channel,
            String locale
    );

    Page<TemplateFamily> findByScopeAndOwnerTenantIdAndChannelAndLocale(
            TemplateScope scope,
            String ownerTenantId,
            String channel,
            String locale,
            Pageable pageable
    );

    @Query("""
        select tf from TemplateFamily tf
        where (tf.scope = :globalScope and tf.ownerTenantId = :globalOwner)
           or (tf.scope = :tenantScope and tf.ownerTenantId = :tenantId)
        """)
    Page<TemplateFamily> findVisibleFamilies(
            @Param("globalScope") TemplateScope globalScope,
            @Param("globalOwner") String globalOwner,
            @Param("tenantScope") TemplateScope tenantScope,
            @Param("tenantId") String tenantId,
            Pageable pageable
    );

    @Query("""
        select tf from TemplateFamily tf
        where tf.templateId = :templateId
          and (
            (tf.scope = :tenantScope and tf.ownerTenantId = :tenantId)
            or (tf.scope = :globalScope and tf.ownerTenantId = :globalOwner)
          )
        """)
    Optional<TemplateFamily> findVisibleByTemplateId(
            @Param("templateId") UUID templateId,
            @Param("tenantScope") TemplateScope tenantScope,
            @Param("tenantId") String tenantId,
            @Param("globalScope") TemplateScope globalScope,
            @Param("globalOwner") String globalOwner
    );
}
