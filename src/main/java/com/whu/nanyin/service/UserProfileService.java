package com.whu.nanyin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.whu.nanyin.pojo.dto.UserProfileUpdateDTO;
import com.whu.nanyin.pojo.entity.UserProfile;
import com.whu.nanyin.pojo.vo.ProfitLossVO;

/**
 * 用户个人资料服务的接口，定义了与用户资料相关的核心业务方法。
 * 继承自MyBatis-Plus的IService接口，以获得基础的CRUD功能。
 */
public interface UserProfileService extends IService<UserProfile> {

    /**
     * 根据用户的唯一ID获取其个人详细资料。
     * @param userId 用户的ID。
     * @return 返回用户的个人资料实体对象。
     */
    UserProfile getUserProfileByUserId(Long userId);

    /**
     * 更新指定用户的个人资料。
     * @param userId 用户的ID。
     * @param dto 包含待更新字段的数据传输对象。
     * @return 返回更新后的个人资料实体对象。
     */
    UserProfile updateUserProfile(Long userId, UserProfileUpdateDTO dto);

    /**
     * 根据用户ID获取其投资的盈亏统计信息。
     * @param userId 用户的ID。
     * @return 返回一个包含盈亏统计数据的视图对象(VO)。
     */
    ProfitLossVO getProfitLossVOByUserId(Long userId);
}