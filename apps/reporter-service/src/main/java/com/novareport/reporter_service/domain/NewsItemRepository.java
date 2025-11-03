package com.novareport.reporter_service.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface NewsItemRepository extends JpaRepository<NewsItem, UUID> {

    boolean existsByHash(String hash);

    List<NewsItem> findTop10ByPublishedAtAfterOrderByPublishedAtDesc(Instant threshold);

    List<NewsItem> findAllByPublishedAtBetweenOrderByPublishedAtDesc(Instant from, Instant to);

    Optional<NewsItem> findTop1ByOrderByPublishedAtDesc();

    Optional<NewsItem> findTop1ByOrderByIngestedAtDesc();

    @Query("select n.hash from NewsItem n where n.hash in :hashes")
    Set<String> findExistingHashes(@Param("hashes") Collection<String> hashes);
}
