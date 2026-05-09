package com.blogplatform.service;

import com.blogplatform.dto.CreateCategoryRequest;
import com.blogplatform.entity.Category;
import com.blogplatform.entity.Tag;
import com.blogplatform.exception.BusinessException;
import com.blogplatform.repository.CategoryRepository;
import com.blogplatform.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;

    public List<Category> listCategories() {
        return categoryRepository.findAll();
    }

    @Transactional
    public Category createCategory(CreateCategoryRequest request) {
        categoryRepository.findByName(request.getName().trim()).ifPresent(c -> {
            throw new BusinessException("分类已存在");
        });

        Category category = new Category();
        category.setName(request.getName().trim());
        category.setDescription(request.getDescription());
        return categoryRepository.save(category);
    }

    public List<Map<String, Object>> listTags(String keyword) {
        List<Tag> tags;
        if (keyword == null || keyword.isBlank()) {
            tags = tagRepository.findAll();
        } else {
            tags = tagRepository.findTop20ByNameContainingIgnoreCaseOrderByNameAsc(keyword.trim());
        }

        return tags.stream()
                .map(tag -> Map.<String, Object>of(
                        "id", tag.getId(),
                        "name", tag.getName()
                ))
                .toList();
    }
}
