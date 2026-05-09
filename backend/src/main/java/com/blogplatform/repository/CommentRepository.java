package com.blogplatform.repository;

import com.blogplatform.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByArticleIdOrderByCreatedAtAsc(Long articleId);

    long countByArticleId(Long articleId);

    long countByUserId(Long userId);

    @Modifying
    @Query("update Comment c set c.parent = null where c.article.id = :articleId and c.parent is not null")
    int clearParentRelationByArticleId(@Param("articleId") Long articleId);

    @Modifying
    @Query("delete from Comment c where c.article.id = :articleId")
    int deleteByArticleId(@Param("articleId") Long articleId);

    @Query("""
            select c.article.id as articleId, count(c) as commentCount
            from Comment c
            where c.article.id in :articleIds
            group by c.article.id
            """)
    List<Map<String, Object>> countByArticleIds(@Param("articleIds") List<Long> articleIds);
}
