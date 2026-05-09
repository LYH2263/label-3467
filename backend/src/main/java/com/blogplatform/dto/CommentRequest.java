package com.blogplatform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommentRequest {

    private Long parentId;

    @NotBlank(message = "评论内容不能为空")
    @Size(max = 1500, message = "评论内容最多1500字")
    private String content;
}
