package com.blogplatform.service;

import com.blogplatform.dto.CommentRequest;
import com.blogplatform.dto.CommentResponse;
import com.blogplatform.entity.Article;
import com.blogplatform.entity.Comment;
import com.blogplatform.entity.User;
import com.blogplatform.exception.BusinessException;
import com.blogplatform.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final ArticleService articleService;

    @Transactional
    public CommentResponse addComment(Long articleId, CommentRequest request, User currentUser) {
        Article article = articleService.getArticleEntity(articleId);

        Comment comment = new Comment();
        comment.setArticle(article);
        comment.setUser(currentUser);
        comment.setContent(Jsoup.clean(request.getContent(), Safelist.none()));

        if (request.getParentId() != null) {
            Comment parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new BusinessException("父评论不存在"));
            if (!parent.getArticle().getId().equals(articleId)) {
                throw new BusinessException("父评论不属于该文章");
            }
            comment.setParent(parent);
        }

        Comment saved = commentRepository.save(comment);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getArticleComments(Long articleId) {
        articleService.getArticleEntity(articleId);

        List<Comment> all = commentRepository.findByArticleIdOrderByCreatedAtAsc(articleId);
        Map<Long, CommentResponse> indexed = new LinkedHashMap<>();
        List<CommentResponse> roots = new ArrayList<>();

        for (Comment comment : all) {
            indexed.put(comment.getId(), toResponse(comment));
        }

        for (Comment comment : all) {
            CommentResponse current = indexed.get(comment.getId());
            if (comment.getParent() == null) {
                roots.add(current);
            } else {
                CommentResponse parent = indexed.get(comment.getParent().getId());
                if (parent != null) {
                    parent.getReplies().add(current);
                }
            }
        }

        return roots;
    }

    @Transactional
    public CommentResponse likeComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "评论不存在"));

        comment.setLikeCount(comment.getLikeCount() + 1);
        return toResponse(commentRepository.save(comment));
    }

    private CommentResponse toResponse(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .parentId(comment.getParent() == null ? null : comment.getParent().getId())
                .content(comment.getContent())
                .likeCount(comment.getLikeCount())
                .username(comment.getUser().getUsername())
                .avatarUrl(comment.getUser().getAvatarUrl())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}
