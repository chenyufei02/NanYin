package com.whu.nanyin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.whu.nanyin.pojo.entity.UserProfile;
import com.whu.nanyin.pojo.vo.ProfitLossVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户个人资料表的数据库操作接口 (Mapper)。
 * 继承自MyBatis-Plus的BaseMapper，提供了基础CRUD功能。
 */
@Mapper
public interface UserProfileMapper extends BaseMapper<UserProfile> {

    /**
     * 根据用户ID，获取该用户的盈亏统计信息。
     * 这是一个自定义的SQL查询，具体实现在对应的XML文件中。
     * @param userId 用户的唯一ID。
     * @return 返回一个包含用户盈亏统计数据的视图对象(VO)。
     */
    ProfitLossVO getProfitLossVOByUserId(@Param("userId") Long userId);

}