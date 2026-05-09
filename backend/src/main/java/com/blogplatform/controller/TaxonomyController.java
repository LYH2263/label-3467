package com.blogplatform.controller;

import com.blogplatform.dto.ApiResponse;
import com.blogplatform.dto.CreateCategoryRequest;
import com.blogplatform.entity.Category;
import com.blogplatform.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class TaxonomyController {

    private final CategoryService categoryService;

    @GetMapping("/api/categories")
    public ResponseEntity<ApiResponse<List<Category>>> categories() {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.listCategories()));
    }

    @PostMapping("/api/categories")
    public ResponseEntity<ApiResponse<Category>> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("分类创建成功", categoryService.createCategory(request)));
    }

    @GetMapping("/api/tags")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> tags(@RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.listTags(keyword)));
    }
}
