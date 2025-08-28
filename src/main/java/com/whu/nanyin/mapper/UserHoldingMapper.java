package com.whu.nanyin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.whu.nanyin.pojo.entity.UserHolding;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @description 用户持仓表的数据库操作接口 (Mapper)。
 * 继承自MyBatis-Plus的BaseMapper，提供了基础的CRUD功能。
 * 自定义的方法通过XML文件实现复杂的SQL查询。
 */
@Mapper
public interface UserHoldingMapper extends BaseMapper<UserHolding> {

    /**
     * @description 根据用户ID查询其【完整的】持仓列表。
     * @param userId 用户的唯一ID。
     * @return 返回该用户的持仓实体列表。
     */
    List<UserHolding> listByUserId(@Param("userId") Long userId);

    /**
     * @description 根据【用户ID和可选的基金代码或名称】，筛选查询持仓列表。
     * @param userId   用户的唯一ID。
     * @param fundCode 基金代码（可选，用于模糊匹配）。
     * @param fundName 基金名称（可选，用于模糊匹配）。
     * @return 返回符合筛选条件的持仓实体列表。
     */
    List<UserHolding> listByUserIdAndFundInfo(
        @Param("userId") Long userId,
        @Param("fundCode") String fundCode,
        @Param("fundName") String fundName);
        
    /**
     * @description 分页查询用户持仓列表，支持按基金代码或名称筛选。
     * @param page      分页参数对象。
     * @param userId    用户的唯一ID。
     * @param fundCode  基金代码（可选，用于模糊匹配）。
     * @param fundName  基金名称（可选，用于模糊匹配）。
     * @return 返回分页后的持仓实体列表。
     */
    Page<UserHolding> getHoldingsByPage(
        @Param("page") Page<UserHolding> page,
        @Param("userId") Long userId,
        @Param("fundCode") String fundCode,
        @Param("fundName") String fundName);

}