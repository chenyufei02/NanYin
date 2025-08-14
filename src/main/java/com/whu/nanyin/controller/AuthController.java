package com.whu.nanyin.controller;

import com.whu.nanyin.pojo.dto.LoginDTO;
import com.whu.nanyin.pojo.dto.RegisterDTO;
import com.whu.nanyin.pojo.vo.ApiResponseVO;
import com.whu.nanyin.service.AuthService;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponseVO<String>> registerUser(@Validated @RequestBody RegisterDTO registerDTO) {
        try {
            authService.register(registerDTO);
            // 【修改】返回统一的ApiResponseVO格式
            return ResponseEntity.ok(ApiResponseVO.success("用户注册成功！", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponseVO.error(e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponseVO<JwtResponse>> loginUser(@Validated @RequestBody LoginDTO loginDTO) {
        try {
            String token = authService.login(loginDTO);
            // 【核心修改】将JwtResponse对象作为data，包装在ApiResponseVO中
            return ResponseEntity.ok(ApiResponseVO.success("登录成功", new JwtResponse(token)));
        } catch (Exception e) {
            // 对于登录失败，Spring Security的异常处理器会处理，但这里也加上以防万一
            return ResponseEntity.status(401).body(ApiResponseVO.error("登录失败: " + e.getMessage()));
        }
    }

    // JwtResponse内部类保持不变
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