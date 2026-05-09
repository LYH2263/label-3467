package com.blogplatform.repository;

import com.blogplatform.dto.ArticleWithMetrics;
import com.blogplatform.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    Optional<Favorite> findByUserIdAndArticleId(Long userId, Long articleId);

    boolean existsByUserIdAndArticleId(Long userId, Long articleId);

    long countByArticleId(Long articleId);

    long countByUserId(Long userId);

    List<Favorite> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("""
            select new com.blogplatform.dto.ArticleWithMetrics(f.article.id, count(f), 0L)
            from Favorite f where f.article.id in :articleIds
            group by f.article.id
            """)
    List<ArticleWithMetrics> countByArticleIds(@Param("articleIds") List<Long> articleIds);

    @Modifying
    @Query("delete from Favorite f where f.article.id = :articleId")
    int deleteByArticleId(@Param("articleId") Long articleId);
}
