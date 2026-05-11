package com.blogplatform.dto;

import com.blogplatform.entity.Article;

public record ArticleWithMetrics(
        Article article,
        long favoriteCount,
        long commentCount
) {
}
