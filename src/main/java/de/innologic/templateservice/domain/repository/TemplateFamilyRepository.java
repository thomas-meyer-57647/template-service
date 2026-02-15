package de.innologic.templateservice.domain.repository;

import de.innologic.templateservice.domain.entity.TemplateFamily;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TemplateFamilyRepository extends JpaRepository<TemplateFamily, UUID> {
}
