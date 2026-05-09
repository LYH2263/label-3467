package com.blogplatform.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProfileRequest {

    @Size(min = 3, max = 30, message = "用户名长度需在3-30之间")
    private String username;

    @Size(max = 1000, message = "简介最多1000字")
    private String bio;
}
