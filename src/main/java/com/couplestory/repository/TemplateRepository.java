package com.couplestory.repository;

import com.couplestory.entity.Template;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TemplateRepository extends JpaRepository<Template, UUID> {

    Optional<Template> findByCode(String code);

    List<Template> findByIsActiveTrueOrderBySortOrder();
}
