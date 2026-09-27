package com.couplestory.repository;
import com.couplestory.entity.WebsiteMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WebsiteMessageRepository extends JpaRepository<WebsiteMessage, String> {
    List<WebsiteMessage> findByWebsiteId(String websiteId);
    List<WebsiteMessage> findByWebsiteIdAndType(String websiteId, String type);
    Optional<WebsiteMessage> findByWebsiteIdAndTypeAndId(String websiteId, String type, String id);
}
