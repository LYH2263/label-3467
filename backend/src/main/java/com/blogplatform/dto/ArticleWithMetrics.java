package com.blogplatform.dto;

public record ArticleWithMetrics(Long articleId, Long favoriteCount, Long commentCount) {
}
