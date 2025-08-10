package com.whu.nanyin.service.impl;

import com.whu.nanyin.security.JwtTokenProvider;
import com.whu.nanyin.mapper.UserMapper;
import com.whu.nanyin.pojo.dto.LoginDTO;
import com.whu.nanyin.pojo.dto.RegisterDTO;
import com.whu.nanyin.pojo.entity.User;
import com.whu.nanyin.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager; // 需要在SecurityConfig中暴露Bean
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private AuthenticationManager authenticationManager; // 下一步在SecurityConfig中配置
    @Autowired
    private JwtTokenProvider tokenProvider;

    @Override
    public void register(RegisterDTO registerDTO) {
        if (userMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>().eq("username", registerDTO.getUsername())) != null) {
            throw new RuntimeException("错误: 用户名已存在!");
        }
        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        userMapper.insert(user);
    }

    @Override
    public String login(LoginDTO loginDTO) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginDTO.getUsername(),
                        loginDTO.getPassword()
                )
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return tokenProvider.generateToken(authentication);
    }
}