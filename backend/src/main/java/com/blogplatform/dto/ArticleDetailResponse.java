package com.blogplatform.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
public class ArticleDetailResponse {
    private ArticleSummaryResponse article;
    private String contentMarkdown;
    private String contentHtml;
    private List<Map<String, Object>> shareStats;
    private boolean favorited;
}
