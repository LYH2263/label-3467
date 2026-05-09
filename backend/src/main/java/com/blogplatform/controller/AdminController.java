package com.blogplatform.controller;

import com.blogplatform.dto.AdminUpdateUserRoleRequest;
import com.blogplatform.dto.ApiResponse;
import com.blogplatform.dto.ArticleSummaryResponse;
import com.blogplatform.service.ArticleService;
import com.blogplatform.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final ArticleService articleService;
    private final UserService userService;

    @GetMapping("/articles")
    public ResponseEntity<ApiResponse<Page<ArticleSummaryResponse>>> articles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(ApiResponse.ok(articleService.searchAllForAdmin(page, size)));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> users() {
        return ResponseEntity.ok(ApiResponse.ok(userService.listUsers()));
    }

    @PutMapping("/users/{userId}/role")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateRole(@PathVariable Long userId,
                                                                       @Valid @RequestBody AdminUpdateUserRoleRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("用户角色更新成功", userService.updateUserRoleByAdmin(userId, request.getRole())));
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long userId) {
        userService.deleteUserByAdmin(userId);
        return ResponseEntity.ok(ApiResponse.ok("用户删除成功", null));
    }
}
