package com.blogplatform.controller;

import com.blogplatform.dto.ApiResponse;
import com.blogplatform.dto.CommentRequest;
import com.blogplatform.dto.CommentResponse;
import com.blogplatform.service.CommentService;
import com.blogplatform.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final UserService userService;

    @GetMapping("/api/articles/{articleId}/comments")
    public ResponseEntity<ApiResponse<List<CommentResponse>>> comments(@PathVariable Long articleId) {
        return ResponseEntity.ok(ApiResponse.ok(commentService.getArticleComments(articleId)));
    }

    @PostMapping("/api/articles/{articleId}/comments")
    public ResponseEntity<ApiResponse<CommentResponse>> addComment(@PathVariable Long articleId,
                                                                   @Valid @RequestBody CommentRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("评论成功", commentService.addComment(articleId, request, userService.getCurrentUserEntity())));
    }

    @PostMapping("/api/comments/{commentId}/like")
    public ResponseEntity<ApiResponse<CommentResponse>> like(@PathVariable Long commentId) {
        return ResponseEntity.ok(ApiResponse.ok(commentService.likeComment(commentId)));
    }
}
