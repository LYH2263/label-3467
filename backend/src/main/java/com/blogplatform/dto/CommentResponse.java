package com.blogplatform.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
public class CommentResponse {
    private Long id;
    private Long parentId;
    private String content;
    private Integer likeCount;
    private String username;
    private String avatarUrl;
    private LocalDateTime createdAt;

    @Builder.Default
    private List<CommentResponse> replies = new ArrayList<>();
}
