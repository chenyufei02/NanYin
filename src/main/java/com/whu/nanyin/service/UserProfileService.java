package com.whu.nanyin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.whu.nanyin.pojo.dto.UserProfileUpdateDTO;
import com.whu.nanyin.pojo.entity.UserProfile;
import com.whu.nanyin.pojo.vo.ProfitLossVO;

public interface UserProfileService extends IService<UserProfile> {

    // 根据用户的ID获取个人资料
    UserProfile getUserProfileByUserId(Long userId);

    // 更新用户个人资料
    UserProfile updateUserProfile(Long userId, UserProfileUpdateDTO dto);

    ProfitLossVO getProfitLossVOByUserId(Long userId);
}