package com.blogplatform.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ArticleRevisionResponse {
    private Long id;
    private String title;
    private String summary;
    private String contentMarkdown;
    private String contentHtml;
    private String changeNote;
    private String createdBy;
    private LocalDateTime createdAt;
}
