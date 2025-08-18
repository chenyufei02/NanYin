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
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户个人资料服务的实现类，负责具体的业务逻辑执行。
 * 继承自MyBatis-Plus的ServiceImpl，内置了对Mapper的基础操作。
 */
@Service
public class UserProfileServiceImpl extends ServiceImpl<UserProfileMapper, UserProfile> implements UserProfileService {

    /**
     * 根据用户ID获取其个人资料实体。
     * @param userId 用户的唯一ID。
     * @return 如果找到，返回UserProfile实体；否则返回null。
     */
    @Override
    public UserProfile getUserProfileByUserId(Long userId) {
        // 使用MyBatis-Plus的QueryWrapper构建查询条件：WHERE user_id = #{userId}
        return this.getOne(new QueryWrapper<UserProfile>().eq("user_id", userId));
    }

    /**
     * 更新用户的个人资料。此方法在一个事务中执行。
     * @param userId 用户的唯一ID，从安全上下文中获取，确保操作的安全性。
     * @param dto 包含待更新字段的数据传输对象(DTO)。
     * @return 更新后的UserProfile实体。
     * @throws RuntimeException 如果找不到该用户的个人资料。
     */
    @Override
    @Transactional // 声明为事务方法，确保数据一致性
    public UserProfile updateUserProfile(Long userId, UserProfileUpdateDTO dto) {
        // 首先根据userId从数据库中查出对应的实体
        UserProfile userProfile = this.getUserProfileByUserId(userId);
        if (userProfile == null) {
            // 如果找不到，抛出运行时异常，事务将回滚
            throw new RuntimeException("找不到该用户的个人资料");
        }
        // 使用Spring的BeanUtils.copyProperties方法，将DTO中的属性值复制到从数据库查出的实体对象上。
        // 这个方法会自动匹配同名同类型的属性进行复制。
        BeanUtils.copyProperties(dto, userProfile);

        // 调用MyBatis-Plus提供的updateById方法，将更新后的实体持久化到数据库
        this.updateById(userProfile);
        return userProfile;
    }

    /**
     * 根据用户ID获取其投资的盈亏统计信息。
     * @param userId 用户的唯一ID。
     * @return 包含盈亏数据的视图对象(VO)。
     */
    @Override
    public ProfitLossVO getProfitLossVOByUserId(Long userId) {
        // 调用Mapper层中自定义的SQL查询方法来获取盈亏数据
        return getBaseMapper().getProfitLossVOByUserId(userId);
    }
}