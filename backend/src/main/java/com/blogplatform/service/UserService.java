package com.blogplatform.service;

import com.blogplatform.dto.ArticleSummaryResponse;
import com.blogplatform.dto.UpdatePasswordRequest;
import com.blogplatform.dto.UpdateProfileRequest;
import com.blogplatform.dto.UserProfileResponse;
import com.blogplatform.entity.Favorite;
import com.blogplatform.entity.Role;
import com.blogplatform.entity.User;
import com.blogplatform.exception.BusinessException;
import com.blogplatform.repository.ArticleRepository;
import com.blogplatform.repository.CommentRepository;
import com.blogplatform.repository.FavoriteRepository;
import com.blogplatform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;
    private final FavoriteRepository favoriteRepository;
    private final ArticleService articleService;
    private final ArticleRepository articleRepository;
    private final CommentRepository commentRepository;

    public User getCurrentUserEntity() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || "anonymousUser".equals(authentication.getName())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "未登录");
        }

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "用户不存在"));
    }

    public Optional<User> getCurrentUserOptional() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || "anonymousUser".equals(authentication.getName())) {
            return Optional.empty();
        }
        return userRepository.findByEmail(authentication.getName());
    }

    public UserProfileResponse getCurrentUserProfile() {
        return toProfile(getCurrentUserEntity());
    }

    @Transactional
    public UserProfileResponse updateProfile(UpdateProfileRequest request) {
        User user = getCurrentUserEntity();

        if (request.getUsername() != null && !request.getUsername().isBlank() && !request.getUsername().equals(user.getUsername())) {
            userRepository.findByUsername(request.getUsername()).ifPresent(existing -> {
                if (!existing.getId().equals(user.getId())) {
                    throw new BusinessException("用户名已被占用");
                }
            });
            user.setUsername(request.getUsername().trim());
        }

        if (request.getBio() != null) {
            user.setBio(request.getBio().trim());
        }

        userRepository.save(user);
        return toProfile(user);
    }

    @Transactional
    public UserProfileResponse updatePassword(UpdatePasswordRequest request) {
        User user = getCurrentUserEntity();

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException("旧密码错误");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BusinessException("新密码不能与旧密码一致");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        return toProfile(user);
    }

    @Transactional
    public UserProfileResponse uploadAvatar(MultipartFile file) {
        User user = getCurrentUserEntity();
        String avatarUrl = fileStorageService.store(file);
        user.setAvatarUrl(avatarUrl);
        userRepository.save(user);
        return toProfile(user);
    }

    public List<ArticleSummaryResponse> getFavoriteArticles() {
        User user = getCurrentUserEntity();
        List<Favorite> favorites = favoriteRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        return favorites.stream()
                .map(Favorite::getArticle)
                .map(articleService::toSummary)
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> listUsers() {
        return userRepository.findAll().stream()
                .map(this::toAdminUserMap)
                .toList();
    }

    @Transactional
    public Map<String, Object> updateUserRoleByAdmin(Long userId, Role role) {
        User operator = getCurrentUserEntity();
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在"));

        if (operator.getId().equals(target.getId()) && role != Role.ROLE_ADMIN) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "不能将当前登录管理员降级为普通用户");
        }

        target.setRole(role);
        userRepository.save(target);
        return toAdminUserMap(target);
    }

    @Transactional
    public void deleteUserByAdmin(Long userId) {
        User operator = getCurrentUserEntity();
        if (operator.getId().equals(userId)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "不能删除当前登录管理员账号");
        }

        User target = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在"));

        long articleCount = articleRepository.countByAuthorId(userId);
        long commentCount = commentRepository.countByUserId(userId);
        long favoriteCount = favoriteRepository.countByUserId(userId);
        if (articleCount > 0 || commentCount > 0 || favoriteCount > 0) {
            throw new BusinessException(HttpStatus.CONFLICT, "用户存在关联文章/评论/收藏，无法直接删除");
        }

        userRepository.delete(target);
    }

    private Map<String, Object> toAdminUserMap(User user) {
        return Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "email", user.getEmail(),
                "role", user.getRole(),
                "emailVerified", user.isEmailVerified(),
                "createdAt", user.getCreatedAt()
        );
    }

    private UserProfileResponse toProfile(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .emailVerified(user.isEmailVerified())
                .avatarUrl(user.getAvatarUrl())
                .bio(user.getBio())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
