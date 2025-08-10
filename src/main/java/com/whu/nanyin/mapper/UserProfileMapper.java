package com.whu.nanyin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.whu.nanyin.pojo.entity.UserProfile;
import com.whu.nanyin.pojo.vo.ProfitLossVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserProfileMapper extends BaseMapper<UserProfile> {

    /**
     * 根据用户ID，获取该用户的盈亏统计信息
     * @param userId 用户的唯一ID
     * @return 用户的盈亏视图对象
     */
    ProfitLossVO getProfitLossVOByUserId(@Param("userId") Long userId);

}