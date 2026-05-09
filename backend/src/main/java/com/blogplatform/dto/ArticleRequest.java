package com.blogplatform.dto;

import com.blogplatform.entity.ArticleStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ArticleRequest {

    @NotBlank(message = "标题不能为空")
    @Size(min = 3, max = 200, message = "标题长度需在3-200之间")
    private String title;

    @Size(max = 1200, message = "摘要最多1200字")
    private String summary;

    private String contentMarkdown;

    private String contentHtml;

    @NotNull(message = "分类不能为空")
    private Long categoryId;

    private List<String> tags = new ArrayList<>();

    private String coverImageUrl;

    private String changeNote;

    @NotNull(message = "文章状态不能为空")
    private ArticleStatus status;
}
