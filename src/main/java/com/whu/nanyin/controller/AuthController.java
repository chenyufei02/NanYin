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

/**
 * 用户认证控制器
 * 
 * 负责处理用户认证相关的HTTP请求，包括用户注册和登录功能。
 * 这是整个JWT认证系统的入口点，为前端提供标准的RESTful API接口。
 * 
 * 主要功能：
 * 1. 用户注册 - 处理新用户的注册请求
 * 2. 用户登录 - 验证用户凭据并返回JWT令牌
 * 3. 统一响应格式 - 所有接口都返回ApiResponseVO格式的响应
 * 4. 异常处理 - 妥善处理认证过程中的各种异常情况
 * 
 * @author lly
 */
@RestController  // 标识这是一个REST控制器，返回JSON格式数据
@RequestMapping("/api/auth")  // 所有认证相关接口的基础路径
public class AuthController {

    /**
     * 认证服务，处理用户注册和登录的业务逻辑
     */
    @Autowired
    private AuthService authService;

    /**
     * 用户注册接口
     * 
     * 处理新用户的注册请求，验证用户输入数据的有效性，
     * 调用业务层进行用户创建，并返回统一格式的响应。
     * 
     * @param registerDTO 注册数据传输对象，包含用户名、密码等注册信息
     * @return ResponseEntity<ApiResponseVO<String>> 注册结果响应
     *         - 成功：HTTP 200 + 成功消息
     *         - 失败：HTTP 400 + 错误消息
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponseVO<String>> registerUser(@Validated @RequestBody RegisterDTO registerDTO) {
        try {
            // 调用业务层进行用户注册
            authService.register(registerDTO);
            
            // 返回统一的ApiResponseVO格式，注册成功时data为null
            return ResponseEntity.ok(ApiResponseVO.success("用户注册成功！", null));
        } catch (Exception e) {
            // 捕获注册过程中的异常（如用户名已存在等），返回400状态码和错误信息
            return ResponseEntity.badRequest().body(ApiResponseVO.error(e.getMessage()));
        }
    }

    /**
     * 用户登录接口
     * 
     * 验证用户登录凭据（用户名和密码），成功后生成JWT令牌并返回。
     * JWT令牌将用于后续请求的身份验证。
     * 
     * @param loginDTO 登录数据传输对象，包含用户名和密码
     * @return ResponseEntity<ApiResponseVO<JwtResponse>> 登录结果响应
     *         - 成功：HTTP 200 + JWT令牌信息
     *         - 失败：HTTP 401 + 错误消息
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponseVO<JwtResponse>> loginUser(@Validated @RequestBody LoginDTO loginDTO) {
        try {
            // 调用业务层进行用户身份验证，成功后返回JWT令牌
            String token = authService.login(loginDTO);
            
            // 将JWT令牌包装在JwtResponse对象中，再封装到ApiResponseVO中返回
            return ResponseEntity.ok(ApiResponseVO.success("登录成功", new JwtResponse(token)));
        } catch (Exception e) {
            // 捕获登录失败的异常（如用户名不存在、密码错误等）
            // 注意：Spring Security的异常处理器也会处理认证失败，这里是额外的保护
            return ResponseEntity.status(401).body(ApiResponseVO.error("登录失败: " + e.getMessage()));
        }
    }

    /**
     * JWT响应数据传输对象
     * 
     * 用于封装登录成功后返回给前端的JWT令牌信息。
     * 采用标准的OAuth 2.0 Bearer Token格式。
     * 
     * 响应示例：
     * {
     *   "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
     *   "tokenType": "Bearer"
     * }
     */
    @Getter  // Lombok注解，自动生成getter方法
    @Setter  // Lombok注解，自动生成setter方法
    public static class JwtResponse {
        
        /**
         * JWT访问令牌
         * 前端需要在后续请求的Authorization头中携带此令牌
         */
        private String accessToken;
        
        /**
         * 令牌类型，固定为"Bearer"
         * 符合OAuth 2.0标准，表示这是一个Bearer类型的令牌
         */
        private String tokenType = "Bearer";

        /**
         * 构造函数
         * 
         * @param accessToken JWT访问令牌字符串
         */
        public JwtResponse(String accessToken) {
            this.accessToken = accessToken;
        }
    }
}