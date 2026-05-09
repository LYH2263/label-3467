package com.blogplatform.dto;

import com.blogplatform.entity.Role;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminUpdateUserRoleRequest {

    @NotNull(message = "角色不能为空")
    private Role role;
}
