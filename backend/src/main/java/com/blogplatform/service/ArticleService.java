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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArticleService {

    private final ArticleRepository articleRepo;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final ArticleRevisionRepository revisionRepository;
    private final FavoriteRepository favoriteRepo;
    private final ShareStatRepository shareStatRepository;
    private final CommentRepository commentRepo;
    private final HtmlSanitizer htmlSanitizer;
    private final ArticleExcelExporter excelExporter;
    private final TagService tagSvc;

    private final ArticleMetricsCalculator metricsCalculator = ArticleMetricsCalculator.INSTANCE;

    @Transactional
    @CacheEvict(value = {"articleList", "articleDetail", "relatedArticles"}, allEntries = true)
    public ArticleSummaryResponse createArticle(ArticleRequest request, User currentUser) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new BusinessException("分类不存在"));

        Article article = new Article();
        applyArticleChanges(article, request, category);
        article.setAuthor(currentUser);

        String slug = generateUniqueSlug(request.getTitle());
        article.setSlug(slug);

        Article saved = articleRepo.save(article);
        return toSummary(saved);
    }

    @Transactional
    @CacheEvict(value = {"articleList", "articleDetail", "relatedArticles"}, allEntries = true)
    public ArticleSummaryResponse updateArticle(Long articleId, ArticleRequest request, User currentUser) {
        Article article = articleRepo.findById(articleId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "文章不存在"));

        validateArticlePermission(article, currentUser);

        ArticleRevision revision = new ArticleRevision();
        revision.setArticle(article);
        revision.setTitle(article.getTitle());
        revision.setSummary(article.getSummary());
        revision.setContentMarkdown(article.getContentMarkdown());
        revision.setContentHtml(article.getContentHtml());
        revision.setChangeNote(request.getChangeNote() == null ? "内容更新" : request.getChangeNote());
        revision.setCreatedBy(currentUser.getUsername());
        revisionRepository.save(revision);

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new BusinessException("分类不存在"));

        boolean titleChanged = !article.getTitle().equals(request.getTitle());
        applyArticleChanges(article, request, category);

        if (titleChanged) {
            article.setSlug(generateUniqueSlug(request.getTitle()));
        }

        Article saved = articleRepo.save(article);
        return toSummary(saved);
    }

    @Transactional
    @CacheEvict(value = {"articleList", "articleDetail", "relatedArticles"}, allEntries = true)
    public void deleteArticle(Long articleId, User currentUser) {
        Article article = articleRepo.findById(articleId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "文章不存在"));

        validateArticlePermission(article, currentUser);
        try {
            commentRepo.clearParentRelationByArticleId(articleId);
            commentRepo.deleteByArticleId(articleId);
            favoriteRepo.deleteByArticleId(articleId);
            shareStatRepository.deleteByArticleId(articleId);
            revisionRepository.deleteByArticleId(articleId);
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
        Specification<Article> spec = buildSearchSpec(keyword, categoryId, startDate, endDate, true, null);
        Page<Article> articles = articleRepo.findAll(spec, pageable);

        List<ArticleWithMetrics> withMetrics = enrichWithMetrics(articles.getContent());

        List<ArticleSummaryResponse> data = withMetrics.stream()
                .map(this::toSummary)
                .toList();

        return new PageImpl<>(data, pageable, articles.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Page<ArticleSummaryResponse> searchMine(User currentUser, int page, int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 20));
        Page<Article> articles = articleRepo.findByAuthorIdOrderByUpdatedAtDesc(currentUser.getId(), pageable);

        List<ArticleWithMetrics> withMetrics = enrichWithMetrics(articles.getContent());

        List<ArticleSummaryResponse> data = withMetrics.stream()
                .map(this::toSummary)
                .toList();

        return new PageImpl<>(data, pageable, articles.getTotalElements());
    }

    @Transactional(readOnly = true)
    public byte[] exportMineAnalytics(User currentUser) {
        List<Article> articles = articleRepo.findByAuthorIdOrderByUpdatedAtDesc(currentUser.getId());
        return excelExporter.exportArticles(articles);
    }

    @Transactional(readOnly = true)
    public Page<ArticleSummaryResponse> searchAllForAdmin(int page, int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 30));
        Page<Article> articles = articleRepo.findAllByOrderByUpdatedAtDesc(pageable);

        List<ArticleWithMetrics> withMetrics = enrichWithMetrics(articles.getContent());

        List<ArticleSummaryResponse> data = withMetrics.stream()
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

        List<Map<String, Object>> shareStats = shareStatRepository.findByArticleId(articleId).stream()
                .map(share -> Map.<String, Object>of(
                        "platform", share.getPlatform(),
                        "shareCount", share.getShareCount()
                ))
                .toList();

        boolean favorited = viewer != null && favoriteRepo.existsByUserIdAndArticleId(viewer.getId(), articleId);

        return ArticleDetailResponse.builder()
                .article(toSummary(article))
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

        validateArticlePermission(article, currentUser);

        List<Map<String, Object>> shareStats = shareStatRepository.findByArticleId(articleId).stream()
                .map(share -> Map.<String, Object>of(
                        "platform", share.getPlatform(),
                        "shareCount", share.getShareCount()
                ))
                .toList();

        boolean favorited = favoriteRepo.existsByUserIdAndArticleId(currentUser.getId(), articleId);

        return ArticleDetailResponse.builder()
                .article(toSummary(article))
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

        validateArticlePermission(article, currentUser);

        return revisionRepository.findByArticleIdOrderByCreatedAtDesc(articleId).stream()
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

        ShareStat shareStat = shareStatRepository.findByArticleIdAndPlatform(articleId, normalizedPlatform)
                .orElseGet(() -> {
                    ShareStat stat = new ShareStat();
                    stat.setArticle(article);
                    stat.setPlatform(normalizedPlatform);
                    stat.setShareCount(0);
                    return stat;
                });

        shareStat.setShareCount(shareStat.getShareCount() + 1);
        shareStatRepository.save(shareStat);

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

        List<ArticleWithMetrics> withMetrics = enrichWithMetrics(related.values().stream().limit(5).toList());
        return withMetrics.stream().map(this::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public Article getArticleEntity(Long articleId) {
        return articleRepo.findById(articleId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "文章不存在"));
    }

    private List<ArticleWithMetrics> enrichWithMetrics(List<Article> articles) {
        if (articles.isEmpty()) {
            return List.of();
        }

        List<Long> articleIds = articles.stream().map(Article::getId).toList();

        Map<Long, Long> favoriteCounts = favoriteRepo.countByArticleIds(articleIds).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row.get("articleId"),
                        row -> (Long) row.get("favoriteCount")
                ));

        Map<Long, Long> commentCounts = commentRepo.countByArticleIds(articleIds).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row.get("articleId"),
                        row -> (Long) row.get("commentCount")
                ));

        return articles.stream()
                .map(article -> new ArticleWithMetrics(
                        article,
                        favoriteCounts.getOrDefault(article.getId(), 0L),
                        commentCounts.getOrDefault(article.getId(), 0L)
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public ArticleSummaryResponse toSummary(Article article) {
        long favoriteCount = favoriteRepo.countByArticleId(article.getId());
        long commentCount = commentRepo.countByArticleId(article.getId());
        return toSummary(new ArticleWithMetrics(article, favoriteCount, commentCount));
    }

    private ArticleSummaryResponse toSummary(ArticleWithMetrics withMetrics) {
        Article article = withMetrics.article();
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
                .favoriteCount(withMetrics.favoriteCount())
                .commentCount(withMetrics.commentCount())
                .publishedAt(article.getPublishedAt())
                .updatedAt(article.getUpdatedAt())
                .build();
    }

    private void applyArticleChanges(Article article, ArticleRequest request, Category category) {
        article.setTitle(request.getTitle().trim());
        article.setSummary(request.getSummary());
        String normalizedMarkdown = normalizeMarkdown(request.getContentMarkdown(), request.getContentHtml());
        String normalizedHtml = normalizeHtml(request.getContentHtml(), normalizedMarkdown);
        article.setContentMarkdown(normalizedMarkdown);
        article.setContentHtml(normalizedHtml);
        article.setCategory(category);
        article.setStatus(request.getStatus());
        article.setCoverImageUrl(request.getCoverImageUrl());

        if (request.getStatus() == ArticleStatus.PUBLISHED && article.getPublishedAt() == null) {
            article.setPublishedAt(LocalDateTime.now());
        }

        TagResolutionResult result = tagSvc.resolveTags(request.getTags());
        Set<Tag> tags = doResolveTags(result);
        article.setTags(tags);
    }

    private Set<Tag> doResolveTags(TagResolutionResult result) {
        if (result instanceof TagResolutionResult.Success success) {
            return success.tags();
        }
        return new HashSet<>();
    }

    private String normalizeMarkdown(String contentMarkdown, String contentHtml) {
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

    private String normalizeHtml(String contentHtml, String normalizedMarkdown) {
        if (contentHtml != null && !contentHtml.isBlank()) {
            return htmlSanitizer.sanitize(contentHtml);
        }

        String markdownAsHtml = markdownToSimpleHtml(normalizedMarkdown);
        return htmlSanitizer.sanitize(markdownAsHtml);
    }

    private String markdownToSimpleHtml(String markdown) {
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

    private String generateUniqueSlug(String title) {
        String baseSlug = SlugUtil.toSlug(title);
        String slug = baseSlug;
        int sequence = 1;
        while (articleRepo.existsBySlug(slug)) {
            slug = baseSlug + "-" + sequence;
            sequence++;
        }
        return slug;
    }

    private void validateArticlePermission(Article article, User currentUser) {
        if (currentUser.getRole() == Role.ROLE_ADMIN) {
            return;
        }

        if (!article.getAuthor().getId().equals(currentUser.getId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "无权限操作该文章");
        }
    }

    private Specification<Article> buildSearchSpec(String keyword,
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
