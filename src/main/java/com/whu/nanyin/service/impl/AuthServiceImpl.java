package com.whu.nanyin.service.impl;

import com.whu.nanyin.security.JwtTokenProvider;
import com.whu.nanyin.mapper.UserMapper;
import com.whu.nanyin.pojo.dto.LoginDTO;
import com.whu.nanyin.pojo.dto.RegisterDTO;
import com.whu.nanyin.pojo.entity.User;
import com.whu.nanyin.pojo.entity.UserProfile; // 【新增】导入UserProfile实体
import com.whu.nanyin.service.AuthService;
import com.whu.nanyin.service.UserProfileService; // 【新增】注入UserProfileService
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // 【新增】导入Transactional

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private JwtTokenProvider tokenProvider;

    // 【新增】注入UserProfileService，用于操作user_profiles表
    @Autowired
    private UserProfileService userProfileService;

    /**
     * 【核心修改】为注册方法增加事务，并同步创建空的个人资料
     */
    @Override
    @Transactional // 确保创建用户和创建资料这两个操作，要么都成功，要么都失败
    public void register(RegisterDTO registerDTO) {
        if (userMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>().eq("username", registerDTO.getUsername())) != null) {
            throw new RuntimeException("错误: 用户名已存在!");
        }

        // 1. 创建并保存用户登录信息 (users表)
        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        userMapper.insert(user); // 注意：执行后，user对象会自动获得数据库生成的ID

        // 2. 【新增】为该用户创建一条空的个人资料记录 (user_profiles表)
        UserProfile userProfile = new UserProfile();
        userProfile.setUserId(user.getId()); // 使用刚刚创建的用户的ID
        // 可以为新用户设置一个默认名字
        userProfile.setName("新用户" + user.getId());
        userProfileService.save(userProfile);
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