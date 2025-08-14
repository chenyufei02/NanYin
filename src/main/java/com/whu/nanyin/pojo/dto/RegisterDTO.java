package com.whu.nanyin.pojo.dto;

import javax.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class RegisterDTO {
    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
}