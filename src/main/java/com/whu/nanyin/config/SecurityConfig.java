package com.whu.nanyin.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.whu.nanyin.pojo.vo.ApiResponseVO;
import com.whu.nanyin.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * Spring Security 安全配置类
 * 
 * 主要功能：
 * 1. 配置JWT认证过滤器
 * 2. 设置CORS跨域配置
 * 3. 配置密码加密器
 * 4. 定义安全过滤链和访问权限
 * 5. 处理认证和授权异常
 * 
 * 
 */
@Configuration  // 标识这是一个配置类
@EnableWebSecurity  // 启用Spring Security的Web安全功能
public class SecurityConfig {

    /**
     * JWT认证过滤器，用于处理JWT令牌的验证
     */
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    
    /**
     * JSON对象映射器，用于将Java对象转换为JSON字符串
     */
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 密码编码器Bean
     * 
     * 使用BCrypt算法对密码进行加密，BCrypt是一种安全的哈希算法，
     * 具有自适应性，可以通过增加工作因子来抵御暴力破解攻击。
     * 
     * @return BCryptPasswordEncoder 密码编码器实例
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 认证管理器Bean
     * 
     * AuthenticationManager是Spring Security的核心接口，负责处理认证请求。
     * 它会调用相应的AuthenticationProvider来验证用户凭据。
     * 
     * @param authenticationConfiguration 认证配置对象
     * @return AuthenticationManager 认证管理器实例
     * @throws Exception 配置异常
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * CORS（跨域资源共享）配置源Bean
     * 
     * CORS是一种机制，允许Web应用程序从不同的域访问资源。
     * 这对于前后端分离的应用程序特别重要，因为前端和后端通常运行在不同的端口上。
     * 
     * @return CorsConfigurationSource CORS配置源
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // 允许来自前端开发服务器的所有请求（Vue.js开发服务器通常运行在8081端口）
        configuration.setAllowedOrigins(List.of("http://localhost:8081"));
        
        // 允许所有HTTP请求方法 (GET, POST, PUT, DELETE, OPTIONS等)
        configuration.setAllowedMethods(List.of("*"));
        
        // 允许所有请求头（包括自定义头如Authorization等）
        configuration.setAllowedHeaders(List.of("*"));
        
        // 允许浏览器发送Cookie等凭证信息
        // 这对于需要携带认证信息的请求很重要
        configuration.setAllowCredentials(true);

        // 创建基于URL的CORS配置源
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        
        // 对所有以/api/开头的路径应用CORS配置
        source.registerCorsConfiguration("/api/**", configuration);
        
        return source;
    }

    /**
     * 安全过滤链配置Bean
     * 
     * 这是Spring Security的核心配置方法，定义了整个应用的安全策略，
     * 包括CORS配置、CSRF保护、会话管理、异常处理、访问权限控制等。
     * 
     * @param http HttpSecurity配置对象
     * @return SecurityFilterChain 安全过滤链
     * @throws Exception 配置异常
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // 启用CORS跨域配置，使用我们之前定义的corsConfigurationSource
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()));

        http
            // 禁用CSRF保护
            // 因为我们使用JWT令牌进行认证，不依赖于Cookie，所以可以安全地禁用CSRF
            .csrf(csrf -> csrf.disable())
            
            // 配置会话管理策略为无状态
            // JWT是无状态的，服务器不需要保存会话信息
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // 配置异常处理
            .exceptionHandling(exceptions -> exceptions
                // 认证入口点：当用户未认证时的处理逻辑
                .authenticationEntryPoint((request, response, authException) -> {
                    // 设置响应内容类型为JSON，编码为UTF-8
                    response.setContentType("application/json;charset=UTF-8");
                    // 设置HTTP状态码为401（未授权）
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    // 返回统一格式的错误响应
                    response.getWriter().write(objectMapper.writeValueAsString(ApiResponseVO.error("用户未登录或Token已过期")));
                })
                // 访问拒绝处理器：当用户已认证但权限不足时的处理逻辑
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    // 设置响应内容类型为JSON，编码为UTF-8
                    response.setContentType("application/json;charset=UTF-8");
                    // 设置HTTP状态码为403（禁止访问）
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    // 返回统一格式的错误响应
                    response.getWriter().write(objectMapper.writeValueAsString(ApiResponseVO.error("无权访问此资源")));
                })
            )
            // 配置HTTP请求的授权规则
            .authorizeHttpRequests(authz -> authz
                // 定义不需要认证的请求路径（白名单）
                .requestMatchers(
                    new AntPathRequestMatcher("/api/auth/**"),      // 认证相关接口（登录、注册）
                    new AntPathRequestMatcher("/swagger-ui/**"),   // Swagger UI文档界面
                    new AntPathRequestMatcher("/v3/api-docs/**")   // OpenAPI 3.0文档接口
                ).permitAll()  // 允许所有人访问
                // 其他所有请求都需要认证
                .anyRequest().authenticated()
            );

        // 在UsernamePasswordAuthenticationFilter之前添加我们的JWT认证过滤器
        // 这样JWT过滤器会先执行，验证JWT令牌并设置认证信息
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // 构建并返回安全过滤链
        return http.build();
    }
}