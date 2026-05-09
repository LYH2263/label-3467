package com.blogplatform.controller;

import com.blogplatform.dto.*;
import com.blogplatform.entity.User;
import com.blogplatform.service.ArticleService;
import com.blogplatform.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ArticleSummaryResponse>>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(ApiResponse.ok(articleService.searchPublished(keyword, categoryId, startDate, endDate, page, size)));
    }

    @GetMapping("/mine")
    public ResponseEntity<ApiResponse<Page<ArticleSummaryResponse>>> mine(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(ApiResponse.ok(articleService.searchMine(userService.getCurrentUserEntity(), page, size)));
    }

    @GetMapping("/mine/analytics/export")
    public ResponseEntity<ByteArrayResource> exportMineAnalytics() {
        byte[] content = articleService.exportMineAnalytics(userService.getCurrentUserEntity());
        ByteArrayResource resource = new ByteArrayResource(content);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=psyche-game-analytics.xlsx")
                .contentLength(content.length)
                .body(resource);
    }

    @GetMapping("/{articleId}")
    public ResponseEntity<ApiResponse<ArticleDetailResponse>> detail(@PathVariable Long articleId) {
        User viewer = userService.getCurrentUserOptional().orElse(null);
        return ResponseEntity.ok(ApiResponse.ok(articleService.getArticleDetail(articleId, viewer)));
    }

    @GetMapping("/{articleId}/edit")
    public ResponseEntity<ApiResponse<ArticleDetailResponse>> editDetail(@PathVariable Long articleId) {
        return ResponseEntity.ok(ApiResponse.ok(articleService.getArticleForEdit(articleId, userService.getCurrentUserEntity())));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ArticleSummaryResponse>> create(@Valid @RequestBody ArticleRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("文章创建成功", articleService.createArticle(request, userService.getCurrentUserEntity())));
    }

    @PutMapping("/{articleId}")
    public ResponseEntity<ApiResponse<ArticleSummaryResponse>> update(@PathVariable Long articleId,
                                                                      @Valid @RequestBody ArticleRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("文章更新成功", articleService.updateArticle(articleId, request, userService.getCurrentUserEntity())));
    }

    @DeleteMapping("/{articleId}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long articleId) {
        articleService.deleteArticle(articleId, userService.getCurrentUserEntity());
        return ResponseEntity.ok(ApiResponse.ok("文章删除成功", null));
    }

    @GetMapping("/{articleId}/revisions")
    public ResponseEntity<ApiResponse<List<ArticleRevisionResponse>>> revisions(@PathVariable Long articleId) {
        return ResponseEntity.ok(ApiResponse.ok(articleService.listRevisions(articleId, userService.getCurrentUserEntity())));
    }

    @PostMapping("/{articleId}/favorite")
    public ResponseEntity<ApiResponse<Map<String, Object>>> favorite(@PathVariable Long articleId) {
        return ResponseEntity.ok(ApiResponse.ok(articleService.toggleFavorite(articleId, userService.getCurrentUserEntity())));
    }

    @PostMapping("/{articleId}/share")
    public ResponseEntity<ApiResponse<Map<String, Object>>> share(@PathVariable Long articleId,
                                                                  @RequestParam(defaultValue = "link") String platform) {
        return ResponseEntity.ok(ApiResponse.ok(articleService.shareArticle(articleId, platform)));
    }

    @GetMapping("/{articleId}/related")
    public ResponseEntity<ApiResponse<List<ArticleSummaryResponse>>> related(@PathVariable Long articleId) {
        return ResponseEntity.ok(ApiResponse.ok(articleService.getRelatedArticles(articleId)));
    }
}
