package de.innologic.templateservice.domain.repository;

import de.innologic.templateservice.domain.entity.TemplateFamily;
import de.innologic.templateservice.domain.enums.TemplateScope;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
