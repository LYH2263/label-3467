package com.blogplatform.controller;

import com.blogplatform.dto.ApiResponse;
import com.blogplatform.dto.ArticleSummaryResponse;
import com.blogplatform.dto.UpdatePasswordRequest;
import com.blogplatform.dto.UpdateProfileRequest;
import com.blogplatform.dto.UserProfileResponse;
import com.blogplatform.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> me() {
        return ResponseEntity.ok(ApiResponse.ok(userService.getCurrentUserProfile()));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("资料更新成功", userService.updateProfile(request)));
    }

    @PutMapping("/me/password")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updatePassword(@Valid @RequestBody UpdatePasswordRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("密码更新成功", userService.updatePassword(request)));
    }

    @PostMapping("/me/avatar")
    public ResponseEntity<ApiResponse<UserProfileResponse>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.ok("头像上传成功", userService.uploadAvatar(file)));
    }

    @GetMapping("/me/favorites")
    public ResponseEntity<ApiResponse<List<ArticleSummaryResponse>>> favorites() {
        return ResponseEntity.ok(ApiResponse.ok(userService.getFavoriteArticles()));
    }
}
