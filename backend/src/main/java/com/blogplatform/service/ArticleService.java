package com.blogplatform.service;

import com.blogplatform.dto.*;
import com.blogplatform.entity.*;
import com.blogplatform.exception.BusinessException;
import com.blogplatform.repository.*;
import com.blogplatform.util.ArticleMetricsCalculator;
import com.blogplatform.util.HtmlSanitizer;
import com.blogplatform.util.SlugUtil;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ArticleService {

    private final ArticleRepository articleRepo;
    private final CategoryRepository categoryRepo;
    private final TagRepository tagRepo;
    private final ArticleRevisionRepository revisionRepo;
    private final FavoriteRepository favoriteRepo;
    private final ShareStatRepository shareStatRepo;
    private final CommentRepository commentRepo;
    private final HtmlSanitizer htmlSanitizer;
    private final TagService tagSvc;
    private final ArticleExcelExporter excelExporterSvc;
    private final ArticleMetricsCalculator metricsCalculator = ArticleMetricsCalculator.INSTANCE;

    @Transactional
    @CacheEvict(value = {"articleList", "articleDetail", "relatedArticles"}, allEntries = true)
    public ArticleSummaryResponse createArticle(ArticleRequest request, User currentUser) {
        Category category = categoryRepo.findById(request.getCategoryId())
                .orElseThrow(() -> new BusinessException("分类不存在"));

        Article article = new Article();
        doApplyArticleChanges(article, request, category);
        article.setAuthor(currentUser);

        String slug = doGenerateUniqueSlug(request.getTitle());
        article.setSlug(slug);

        Article saved = articleRepo.save(article);
        return toSummary(new ArticleWithMetrics(saved, 0, 0));
    }

    @Transactional
    @CacheEvict(value = {"articleList", "articleDetail", "relatedArticles"}, allEntries = true)
    public ArticleSummaryResponse updateArticle(Long articleId, ArticleRequest request, User currentUser) {
        Article article = articleRepo.findById(articleId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "文章不存在"));

        doValidateArticlePermission(article, currentUser);

        ArticleRevision revision = new ArticleRevision();
        revision.setArticle(article);
        revision.setTitle(article.getTitle());
        revision.setSummary(article.getSummary());
        revision.setContentMarkdown(article.getContentMarkdown());
        revision.setContentHtml(article.getContentHtml());
        revision.setChangeNote(request.getChangeNote() == null ? "内容更新" : request.getChangeNote());
        revision.setCreatedBy(currentUser.getUsername());
        revisionRepo.save(revision);

        Category category = categoryRepo.findById(request.getCategoryId())
                .orElseThrow(() -> new BusinessException("分类不存在"));

        boolean titleChanged = !article.getTitle().equals(request.getTitle());
        doApplyArticleChanges(article, request, category);

        if (titleChanged) {
            article.setSlug(doGenerateUniqueSlug(request.getTitle()));
        }

        Article saved = articleRepo.save(article);
        return toSummary(new ArticleWithMetrics(saved, 0, 0));
    }

    @Transactional
    @CacheEvict(value = {"articleList", "articleDetail", "relatedArticles"}, allEntries = true)
    public void deleteArticle(Long articleId, User currentUser) {
        Article article = articleRepo.findById(articleId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "文章不存在"));

        doValidateArticlePermission(article, currentUser);
        try {
            commentRepo.clearParentRelationByArticleId(articleId);
            commentRepo.deleteByArticleId(articleId);
            favoriteRepo.deleteByArticleId(articleId);
            shareStatRepo.deleteByArticleId(articleId);
            revisionRepo.deleteByArticleId(articleId);
            articleRepo.deleteTagRelationsByArticleId(articleId);
            articleRepo.delete(article);
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException(HttpStatus.CONFLICT, "文章存在关联数据，暂时无法删除");
        }
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "articleList", key = "#keyword + ':' + #categoryId + ':' + #startDate + ':' + #endDate + ':' + #page + ':' + #size")
    public Page<ArticleSummaryResponse> searchPublished(String keyword,
                                                        Long categoryId,
                                                        LocalDate startDate,
                                                        LocalDate endDate,
                                                        int page,
                                                        int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 30));
        Specification<Article> spec = doBuildSearchSpec(keyword, categoryId, startDate, endDate, true, null);
        Page<Article> articles = articleRepo.findAll(spec, pageable);

        List<ArticleWithMetrics> articlesWithMetrics = doFetchMetrics(articles.getContent());

        List<ArticleSummaryResponse> data = articlesWithMetrics.stream()
                .map(this::toSummary)
                .toList();

        return new PageImpl<>(data, pageable, articles.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Page<ArticleSummaryResponse> searchMine(User currentUser, int page, int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 20));
        Page<Article> articles = articleRepo.findByAuthorIdOrderByUpdatedAtDesc(currentUser.getId(), pageable);

        List<ArticleWithMetrics> articlesWithMetrics = doFetchMetrics(articles.getContent());

        List<ArticleSummaryResponse> data = articlesWithMetrics.stream()
                .map(this::toSummary)
                .toList();

        return new PageImpl<>(data, pageable, articles.getTotalElements());
    }

    @Transactional(readOnly = true)
    public byte[] exportMineAnalytics(User currentUser) {
        return excelExporterSvc.exportMineAnalytics(currentUser);
    }

    @Transactional(readOnly = true)
    public Page<ArticleSummaryResponse> searchAllForAdmin(int page, int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 30));
        Page<Article> articles = articleRepo.findAllByOrderByUpdatedAtDesc(pageable);

        List<ArticleWithMetrics> articlesWithMetrics = doFetchMetrics(articles.getContent());

        List<ArticleSummaryResponse> data = articlesWithMetrics.stream()
                .map(this::toSummary)
                .toList();

        return new PageImpl<>(data, pageable, articles.getTotalElements());
    }

    @Transactional
    public ArticleDetailResponse getArticleDetail(Long articleId, User viewer) {
        Article article = articleRepo.findById(articleId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "文章不存在"));

        if (article.getStatus() != ArticleStatus.PUBLISHED) {
            if (viewer == null) {
                throw new BusinessException(HttpStatus.NOT_FOUND, "文章不存在");
            }
            boolean owner = article.getAuthor().getId().equals(viewer.getId());
            boolean admin = viewer.getRole() == Role.ROLE_ADMIN;
            if (!owner && !admin) {
                throw new BusinessException(HttpStatus.NOT_FOUND, "文章不存在");
            }
        }

        article.setViewCount(article.getViewCount() + 1);

        List<Map<String, Object>> shareStats = shareStatRepo.findByArticleId(articleId).stream()
                .map(share -> Map.<String, Object>of(
                        "platform", share.getPlatform(),
                        "shareCount", share.getShareCount()
                ))
                .toList();

        boolean favorited = viewer != null && favoriteRepo.existsByUserIdAndArticleId(viewer.getId(), articleId);

        long favoriteCount = favoriteRepo.countByArticleId(articleId);
        long commentCount = commentRepo.countByArticleId(articleId);

        return ArticleDetailResponse.builder()
                .article(toSummary(new ArticleWithMetrics(article, favoriteCount, commentCount)))
                .contentMarkdown(article.getContentMarkdown())
                .contentHtml(article.getContentHtml())
                .shareStats(shareStats)
                .favorited(favorited)
                .build();
    }

    @Transactional(readOnly = true)
    public ArticleDetailResponse getArticleForEdit(Long articleId, User currentUser) {
        Article article = articleRepo.findById(articleId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "文章不存在"));

        doValidateArticlePermission(article, currentUser);

        List<Map<String, Object>> shareStats = shareStatRepo.findByArticleId(articleId).stream()
                .map(share -> Map.<String, Object>of(
                        "platform", share.getPlatform(),
                        "shareCount", share.getShareCount()
                ))
                .toList();

        boolean favorited = favoriteRepo.existsByUserIdAndArticleId(currentUser.getId(), articleId);

        long favoriteCount = favoriteRepo.countByArticleId(articleId);
        long commentCount = commentRepo.countByArticleId(articleId);

        return ArticleDetailResponse.builder()
                .article(toSummary(new ArticleWithMetrics(article, favoriteCount, commentCount)))
                .contentMarkdown(article.getContentMarkdown())
                .contentHtml(article.getContentHtml())
                .shareStats(shareStats)
                .favorited(favorited)
                .build();
    }

    @Transactional(readOnly = true)
    public List<ArticleRevisionResponse> listRevisions(Long articleId, User currentUser) {
        Article article = articleRepo.findById(articleId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "文章不存在"));

        doValidateArticlePermission(article, currentUser);

        return revisionRepo.findByArticleIdOrderByCreatedAtDesc(articleId).stream()
                .map(revision -> ArticleRevisionResponse.builder()
                        .id(revision.getId())
                        .title(revision.getTitle())
                        .summary(revision.getSummary())
                        .contentMarkdown(revision.getContentMarkdown())
                        .contentHtml(revision.getContentHtml())
                        .changeNote(revision.getChangeNote())
                        .createdBy(revision.getCreatedBy())
                        .createdAt(revision.getCreatedAt())
                        .build())
                .toList();
    }

    @Transactional
    @CacheEvict(value = {"articleDetail", "articleList", "relatedArticles"}, allEntries = true)
    public Map<String, Object> toggleFavorite(Long articleId, User currentUser) {
        Article article = articleRepo.findById(articleId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "文章不存在"));

        Optional<Favorite> existing = favoriteRepo.findByUserIdAndArticleId(currentUser.getId(), articleId);
        boolean favorited;
        if (existing.isPresent()) {
            favoriteRepo.delete(existing.get());
            favorited = false;
        } else {
            Favorite favorite = new Favorite();
            favorite.setUser(currentUser);
            favorite.setArticle(article);
            favoriteRepo.save(favorite);
            favorited = true;
        }

        return Map.of(
                "favorited", favorited,
                "favoriteCount", favoriteRepo.countByArticleId(articleId)
        );
    }

    @Transactional
    public Map<String, Object> shareArticle(Long articleId, String platform) {
        Article article = articleRepo.findById(articleId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "文章不存在"));

        String normalizedPlatform = (platform == null || platform.isBlank()) ? "link" : platform.toLowerCase();

        ShareStat shareStat = shareStatRepo.findByArticleIdAndPlatform(articleId, normalizedPlatform)
                .orElseGet(() -> {
                    ShareStat stat = new ShareStat();
                    stat.setArticle(article);
                    stat.setPlatform(normalizedPlatform);
                    stat.setShareCount(0);
                    return stat;
                });

        shareStat.setShareCount(shareStat.getShareCount() + 1);
        shareStatRepo.save(shareStat);

        return Map.of(
                "platform", normalizedPlatform,
                "shareCount", shareStat.getShareCount(),
                "shareUrl", "/article/" + article.getSlug()
        );
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "relatedArticles", key = "#articleId")
    public List<ArticleSummaryResponse> getRelatedArticles(Long articleId) {
        Article article = articleRepo.findById(articleId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "文章不存在"));

        LinkedHashMap<Long, Article> related = new LinkedHashMap<>();

        List<Article> sameCategory = articleRepo.findTop5ByCategoryIdAndIdNotAndStatusOrderByPublishedAtDesc(
                article.getCategory().getId(),
                article.getId(),
                ArticleStatus.PUBLISHED
        );
        for (Article item : sameCategory) {
            related.put(item.getId(), item);
        }

        if (related.size() < 5 && !article.getTags().isEmpty()) {
            List<Long> tagIds = article.getTags().stream().map(Tag::getId).toList();
            List<Article> tagRelated = articleRepo.findRelatedByTags(
                    tagIds,
                    article.getId(),
                    ArticleStatus.PUBLISHED,
                    PageRequest.of(0, 5)
            );

            for (Article item : tagRelated) {
                related.put(item.getId(), item);
                if (related.size() >= 5) {
                    break;
                }
            }
        }

        List<ArticleWithMetrics> articlesWithMetrics = doFetchMetrics(related.values().stream().toList());
        return articlesWithMetrics.stream().limit(5).map(this::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public Article getArticleEntity(Long articleId) {
        return articleRepo.findById(articleId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "文章不存在"));
    }

    @Transactional(readOnly = true)
    public ArticleSummaryResponse toSummary(Article article) {
        long favoriteCount = favoriteRepo.countByArticleId(article.getId());
        long commentCount = commentRepo.countByArticleId(article.getId());
        return toSummary(new ArticleWithMetrics(article, favoriteCount, commentCount));
    }

    @Transactional(readOnly = true)
    public ArticleSummaryResponse toSummary(ArticleWithMetrics articleWithMetrics) {
        Article article = articleWithMetrics.article();
        return ArticleSummaryResponse.builder()
                .id(article.getId())
                .title(article.getTitle())
                .slug(article.getSlug())
                .summary(article.getSummary())
                .coverImageUrl(article.getCoverImageUrl())
                .category(article.getCategory().getName())
                .tags(article.getTags().stream().map(Tag::getName).toList())
                .author(article.getAuthor().getUsername())
                .status(article.getStatus())
                .viewCount(article.getViewCount())
                .favoriteCount(articleWithMetrics.favoriteCount())
                .commentCount(articleWithMetrics.commentCount())
                .publishedAt(article.getPublishedAt())
                .updatedAt(article.getUpdatedAt())
                .build();
    }

    private List<ArticleWithMetrics> doFetchMetrics(List<Article> articles) {
        if (articles.isEmpty()) {
            return List.of();
        }

        List<Long> articleIds = articles.stream().map(Article::getId).toList();
        Map<Long, Long> favoriteCounts = articleRepo.getFavoriteCounts(articleIds);
        Map<Long, Long> commentCounts = articleRepo.getCommentCounts(articleIds);

        return articles.stream()
                .map(article -> new ArticleWithMetrics(
                        article,
                        favoriteCounts.getOrDefault(article.getId(), 0L),
                        commentCounts.getOrDefault(article.getId(), 0L)
                ))
                .toList();
    }

    private void doApplyArticleChanges(Article article, ArticleRequest request, Category category) {
        article.setTitle(request.getTitle().trim());
        article.setSummary(request.getSummary());
        String normalizedMarkdown = doNormalizeMarkdown(request.getContentMarkdown(), request.getContentHtml());
        String normalizedHtml = doNormalizeHtml(request.getContentHtml(), normalizedMarkdown);
        article.setContentMarkdown(normalizedMarkdown);
        article.setContentHtml(normalizedHtml);
        article.setCategory(category);
        article.setStatus(request.getStatus());
        article.setCoverImageUrl(request.getCoverImageUrl());

        if (request.getStatus() == ArticleStatus.PUBLISHED && article.getPublishedAt() == null) {
            article.setPublishedAt(LocalDateTime.now());
        }

        TagResolutionResult result = tagSvc.resolveTags(request.getTags());
        Set<Tag> tags;
        if (result instanceof TagResolutionSuccess success) {
            tags = success.tags();
        } else if (result instanceof TagResolutionFailure failure) {
            throw new BusinessException(failure.message());
        } else {
            throw new BusinessException("标签解析失败");
        }
        article.setTags(tags);
    }

    private String doNormalizeMarkdown(String contentMarkdown, String contentHtml) {
        if (contentMarkdown != null && !contentMarkdown.isBlank()) {
            return contentMarkdown.trim();
        }

        if (contentHtml != null && !contentHtml.isBlank()) {
            String text = Jsoup.parseBodyFragment(contentHtml).body().wholeText();
            if (!text.isBlank()) {
                return text.trim();
            }
        }

        throw new BusinessException("Markdown 与富文本内容不能同时为空");
    }

    private String doNormalizeHtml(String contentHtml, String normalizedMarkdown) {
        if (contentHtml != null && !contentHtml.isBlank()) {
            return htmlSanitizer.sanitize(contentHtml);
        }

        String markdownAsHtml = doMarkdownToSimpleHtml(normalizedMarkdown);
        return htmlSanitizer.sanitize(markdownAsHtml);
    }

    private String doMarkdownToSimpleHtml(String markdown) {
        StringBuilder html = new StringBuilder("<article>");
        String[] lines = markdown.split("\\R");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isBlank()) {
                continue;
            }

            String escaped = HtmlUtils.htmlEscape(trimmed);
            if (trimmed.startsWith("### ")) {
                html.append("<h3>").append(HtmlUtils.htmlEscape(trimmed.substring(4))).append("</h3>");
            } else if (trimmed.startsWith("## ")) {
                html.append("<h2>").append(HtmlUtils.htmlEscape(trimmed.substring(3))).append("</h2>");
            } else if (trimmed.startsWith("# ")) {
                html.append("<h1>").append(HtmlUtils.htmlEscape(trimmed.substring(2))).append("</h1>");
            } else {
                html.append("<p>").append(escaped).append("</p>");
            }
        }
        html.append("</article>");
        return html.toString();
    }

    private String doGenerateUniqueSlug(String title) {
        String baseSlug = SlugUtil.toSlug(title);
        String slug = baseSlug;
        int sequence = 1;
        while (articleRepo.existsBySlug(slug)) {
            slug = baseSlug + "-" + sequence;
            sequence++;
        }
        return slug;
    }

    private void doValidateArticlePermission(Article article, User currentUser) {
        if (currentUser.getRole() == Role.ROLE_ADMIN) {
            return;
        }

        if (!article.getAuthor().getId().equals(currentUser.getId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "无权限操作该文章");
        }
    }

    private Specification<Article> doBuildSearchSpec(String keyword,
                                                      Long categoryId,
                                                      LocalDate startDate,
                                                      LocalDate endDate,
                                                      boolean publishedOnly,
                                                      Long authorId) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (publishedOnly) {
                predicates.add(criteriaBuilder.equal(root.get("status"), ArticleStatus.PUBLISHED));
            }

            if (authorId != null) {
                predicates.add(criteriaBuilder.equal(root.get("author").get("id"), authorId));
            }

            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                Predicate titleLike = criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), pattern);
                Predicate summaryLike = criteriaBuilder.like(criteriaBuilder.lower(root.get("summary")), pattern);
                predicates.add(criteriaBuilder.or(titleLike, summaryLike));
            }

            if (categoryId != null) {
                predicates.add(criteriaBuilder.equal(root.get("category").get("id"), categoryId));
            }

            if (startDate != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("publishedAt"), startDate.atStartOfDay()));
            }

            if (endDate != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("publishedAt"), endDate.atTime(23, 59, 59)));
            }

            query.orderBy(criteriaBuilder.desc(root.get("publishedAt")), criteriaBuilder.desc(root.get("createdAt")));
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
