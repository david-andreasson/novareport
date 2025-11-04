package com.novareport.reporter_service.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "news_items", indexes = {
    @Index(name = "idx_news_items_published_at", columnList = "published_at"),
    @Index(name = "idx_news_items_hash", columnList = "hash", unique = true),
    @Index(name = "idx_news_items_ingested_at", columnList = "ingested_at")
})
public class NewsItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String source;

    @Column(nullable = false, length = 512)
    private String url;

    @Column(nullable = false, length = 512)
    private String title;

    @Column(name = "published_at", nullable = false)
    private Instant publishedAt;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(nullable = false, length = 128, unique = true)
    private String hash;

    @Column(name = "ingested_at", nullable = false)
    private Instant ingestedAt;

    @PrePersist
    void onPersist() {
        if (ingestedAt == null) {
            ingestedAt = Instant.now();
        }
    }
}
