package com.blogplatform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCategoryRequest {

    @NotBlank(message = "分类名称不能为空")
    @Size(min = 2, max = 80, message = "分类名长度需在2-80之间")
    private String name;

    @Size(max = 400, message = "分类描述最多400字")
    private String description;
}
