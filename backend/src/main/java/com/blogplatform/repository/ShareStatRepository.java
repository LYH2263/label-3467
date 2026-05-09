package com.blogplatform.repository;

import com.blogplatform.entity.ShareStat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ShareStatRepository extends JpaRepository<ShareStat, Long> {
    Optional<ShareStat> findByArticleIdAndPlatform(Long articleId, String platform);

    List<ShareStat> findByArticleId(Long articleId);

    @Modifying
    @Query("delete from ShareStat s where s.article.id = :articleId")
    int deleteByArticleId(@Param("articleId") Long articleId);
}
