package com.whu.nanyin.controller;

import com.whu.nanyin.pojo.dto.LoginDTO; // 注意：这个LoginDTO我们下一步创建
import com.whu.nanyin.pojo.dto.RegisterDTO; // 注意：这个RegisterDTO我们下一步创建
import com.whu.nanyin.service.AuthService; // 注意：这个AuthService我们下一步创建
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Validated @RequestBody RegisterDTO registerDTO) {
        try {
            authService.register(registerDTO);
            // 【修改】将返回的字符串包装在一个Map中
            return ResponseEntity.ok(Map.of("message", "用户注册成功！"));
        } catch (Exception e) {
            // 【修改】同样，也将错误信息包装在Map中，保持格式统一
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@Validated @RequestBody LoginDTO loginDTO) {
        try {
            String token = authService.login(loginDTO);
            // 登录成功，返回包含纯净JWT的响应对象
            return ResponseEntity.ok(new JwtResponse(token));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("登录失败: " + e.getMessage());
        }
    }


    @Getter
    @Setter
    public static class JwtResponse {
        private String accessToken;
        private String tokenType = "Bearer";

        public JwtResponse(String accessToken) {
            this.accessToken = accessToken;
        }
    }
}