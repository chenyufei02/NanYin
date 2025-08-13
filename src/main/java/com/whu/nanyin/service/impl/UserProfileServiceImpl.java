package com.whu.nanyin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.whu.nanyin.mapper.UserProfileMapper;
import com.whu.nanyin.pojo.dto.UserProfileUpdateDTO;
import com.whu.nanyin.pojo.entity.UserProfile;
import com.whu.nanyin.pojo.vo.ProfitLossVO;
import com.whu.nanyin.service.UserProfileService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Service
public class UserProfileServiceImpl extends ServiceImpl<UserProfileMapper, UserProfile> implements UserProfileService {

    @Override
    public UserProfile getUserProfileByUserId(Long userId) {
        return this.getOne(new QueryWrapper<UserProfile>().eq("user_id", userId));
    }

    /**
     * 【核心修正】此方法现在接收一个从安全上下文传来的、绝对可靠的userId
     */
    @Override
    public UserProfile updateUserProfile(Long userId, UserProfileUpdateDTO dto) {
        UserProfile userProfile = this.getUserProfileByUserId(userId);
        if (userProfile == null) {
            throw new RuntimeException("找不到该用户的个人资料");
        }
        // 将不包含userId的DTO中的更新信息，复制到从数据库查出的实体对象上
        BeanUtils.copyProperties(dto, userProfile);

        // 执行更新
        this.updateById(userProfile);
        return userProfile;
    }

    @Override
    public ProfitLossVO getProfitLossVOByUserId(Long userId) {
        return getBaseMapper().getProfitLossVOByUserId(userId);
    }
}