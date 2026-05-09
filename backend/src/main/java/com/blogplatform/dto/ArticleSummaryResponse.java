package com.blogplatform.dto;

import com.blogplatform.entity.ArticleStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ArticleSummaryResponse {
    private Long id;
    private String title;
    private String slug;
    private String summary;
    private String coverImageUrl;
    private String category;
    private List<String> tags;
    private String author;
    private ArticleStatus status;
    private Integer viewCount;
    private Long favoriteCount;
    private Long commentCount;
    private LocalDateTime publishedAt;
    private LocalDateTime updatedAt;
}
