package com.whu.nanyin.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.whu.nanyin.mapper.UserFinancialProfileMapper;
import com.whu.nanyin.pojo.entity.UserFinancialProfile;
import com.whu.nanyin.service.UserFinancialProfileService;
import org.springframework.stereotype.Service;

@Service
public class UserFinancialProfileServiceImpl extends ServiceImpl<UserFinancialProfileMapper, UserFinancialProfile> implements UserFinancialProfileService {
}