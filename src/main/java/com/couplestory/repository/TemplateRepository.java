package com.couplestory.repository;

import com.couplestory.entity.Template;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TemplateRepository extends JpaRepository<Template, UUID> {

    Optional<Template> findByCode(String code);

    List<Template> findByIsActiveTrueOrderBySortOrder();

    @Query(value = """
            select t, (select count(s) from Story s where s.templateCode = t.code and s.status <> 'DELETED') as usageCount
            from Template t
            where t.isActive = true and (:type is null or t.type = :type)
              and (lower(t.name) like :q escape '\\' or lower(coalesce(t.description, '')) like :q escape '\\')
            order by usageCount desc, t.createdAt desc nulls last, t.sortOrder asc
            """,
            countQuery = """
            select count(t) from Template t
            where t.isActive = true and (:type is null or t.type = :type)
              and (lower(t.name) like :q escape '\\' or lower(coalesce(t.description, '')) like :q escape '\\')
            """)
    Page<Object[]> explorePopular(@Param("type") String type, @Param("q") String q, Pageable pageable);

    @Query(value = """
            select t, (select count(s) from Story s where s.templateCode = t.code and s.status <> 'DELETED') as usageCount
            from Template t
            where t.isActive = true and (:type is null or t.type = :type)
              and (lower(t.name) like :q escape '\\' or lower(coalesce(t.description, '')) like :q escape '\\')
            order by t.createdAt desc nulls last, t.sortOrder asc
            """,
            countQuery = """
            select count(t) from Template t
            where t.isActive = true and (:type is null or t.type = :type)
              and (lower(t.name) like :q escape '\\' or lower(coalesce(t.description, '')) like :q escape '\\')
            """)
    Page<Object[]> exploreNewest(@Param("type") String type, @Param("q") String q, Pageable pageable);
}
