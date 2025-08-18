package com.whu.nanyin.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.whu.nanyin.mapper.UserMapper;
import com.whu.nanyin.pojo.entity.User;
import com.whu.nanyin.security.CustomUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserAuthService implements UserDetailsService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. 根据用户名，从数据库的 "users" 表中查询用户信息
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        User user = userMapper.selectOne(queryWrapper);

        // 2. 如果查询不到用户，必须抛出此异常，Spring Security会捕获并处理
        if (user == null) {
            throw new UsernameNotFoundException("用户 '" + username + "' 不存在");
        }

        // 3. 如果查询到了用户，将其封装成 Spring Security 可识别的 UserDetails 对象返回
        return new CustomUserDetails(user);
    }
}