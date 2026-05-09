package com.blogplatform.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(
        name = "share_stats",
        uniqueConstraints = @UniqueConstraint(name = "uk_article_platform", columnNames = {"article_id", "platform"})
)
public class ShareStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    @Column(nullable = false, length = 40)
    private String platform;

    @Column(nullable = false)
    private Integer shareCount = 0;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
