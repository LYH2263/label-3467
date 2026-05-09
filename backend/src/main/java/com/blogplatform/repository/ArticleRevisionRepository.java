package com.blogplatform.repository;

import com.blogplatform.entity.ArticleRevision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ArticleRevisionRepository extends JpaRepository<ArticleRevision, Long> {
    List<ArticleRevision> findByArticleIdOrderByCreatedAtDesc(Long articleId);

    @Modifying
    @Query("delete from ArticleRevision r where r.article.id = :articleId")
    int deleteByArticleId(@Param("articleId") Long articleId);
}
