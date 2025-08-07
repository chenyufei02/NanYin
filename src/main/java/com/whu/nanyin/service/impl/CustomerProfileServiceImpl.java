package com.whu.nanyin.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.whu.nanyin.mapper.CustomerProfileMapper;
import com.whu.nanyin.pojo.entity.CustomerProfile;
import com.whu.nanyin.service.CustomerProfileService;
import org.springframework.stereotype.Service;

@Service
public class CustomerProfileServiceImpl extends ServiceImpl<CustomerProfileMapper, CustomerProfile> implements CustomerProfileService {
}