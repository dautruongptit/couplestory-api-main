package com.couplestory.repository;
import com.couplestory.entity.WebsiteEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WebsiteEventRepository extends JpaRepository<WebsiteEvent, String> {
    List<WebsiteEvent> findByWebsiteIdOrderBySortOrderAsc(String websiteId);
}
