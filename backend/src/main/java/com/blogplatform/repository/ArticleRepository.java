package com.blogplatform.repository;

import com.blogplatform.entity.Article;
import com.blogplatform.entity.ArticleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public interface ArticleRepository extends JpaRepository<Article, Long>, JpaSpecificationExecutor<Article> {

    Optional<Article> findBySlug(String slug);

    List<Article> findByAuthorIdOrderByUpdatedAtDesc(Long authorId);

    Page<Article> findByAuthorIdOrderByUpdatedAtDesc(Long authorId, Pageable pageable);

    Page<Article> findAllByOrderByUpdatedAtDesc(Pageable pageable);

    List<Article> findTop5ByCategoryIdAndIdNotAndStatusOrderByPublishedAtDesc(Long categoryId, Long articleId, ArticleStatus status);

    @Query("""
            select distinct a from Article a join a.tags t
            where t.id in :tagIds and a.id <> :articleId and a.status = :status
            order by a.publishedAt desc
            """)
    List<Article> findRelatedByTags(@Param("tagIds") List<Long> tagIds,
                                    @Param("articleId") Long articleId,
                                    @Param("status") ArticleStatus status,
                                    Pageable pageable);

    boolean existsBySlug(String slug);

    long countByAuthorId(Long authorId);
    long countByStatus(ArticleStatus status);

    @Modifying
    @Query(value = "delete from article_tags where article_id = :articleId", nativeQuery = true)
    int deleteTagRelationsByArticleId(@Param("articleId") Long articleId);

    @Query("""
            select f.article.id, count(f)
            from Favorite f
            where f.article.id in :articleIds
            group by f.article.id
            """)
    List<Object[]> countFavoritesByArticleIds(@Param("articleIds") List<Long> articleIds);

    @Query("""
            select c.article.id, count(c)
            from Comment c
            where c.article.id in :articleIds
            group by c.article.id
            """)
    List<Object[]> countCommentsByArticleIds(@Param("articleIds") List<Long> articleIds);

    default Map<Long, Long> getFavoriteCounts(List<Long> articleIds) {
        return countFavoritesByArticleIds(articleIds).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));
    }

    default Map<Long, Long> getCommentCounts(List<Long> articleIds) {
        return countCommentsByArticleIds(articleIds).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));
    }
}
