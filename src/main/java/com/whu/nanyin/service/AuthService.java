package com.whu.nanyin.service;

import com.whu.nanyin.pojo.dto.LoginDTO;
import com.whu.nanyin.pojo.dto.RegisterDTO;

public interface AuthService {
    void register(RegisterDTO registerDTO);
    String login(LoginDTO loginDTO);
}