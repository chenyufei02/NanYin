package com.whu.nanyin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.whu.nanyin.pojo.entity.UserHolding;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserHoldingMapper extends BaseMapper<UserHolding> {
    
    /**
     * 根据用户ID查询持仓列表
     * @param userId 用户ID
     * @return 用户持仓列表
     */
    List<UserHolding> listByUserId(@Param("userId") Long userId);
    
    /**
     * 根据用户ID和基金代码或名称查询持仓
     * @param userId 用户ID
     * @param fundCode 基金代码（可选）
     * @param fundName 基金名称（可选）
     * @return 符合条件的用户持仓列表
     */
    List<UserHolding> listByUserIdAndFundInfo(
        @Param("userId") Long userId, 
        @Param("fundCode") String fundCode, 
        @Param("fundName") String fundName);
    
    /**
     * 获取用户市值排名前N的持仓
     * @param userId 用户ID
     * @param limit 限制数量
     * @return 市值排名前N的持仓列表
     */
    List<UserHolding> getTopNHoldings(@Param("userId") Long userId, @Param("limit") int limit);
}