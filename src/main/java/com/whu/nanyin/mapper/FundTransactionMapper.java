package com.whu.nanyin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.whu.nanyin.pojo.entity.FundTransaction;
import org.apache.ibatis.annotations.Mapper;

/**
 * 基金交易数据访问层接口
 * 
 * <p>该接口负责基金交易相关的数据库操作，包括基金申购、赎回等交易记录的增删改查。
 * 通过继承MyBatis-Plus的BaseMapper接口，自动获得常用的CRUD操作方法。
 */
@Mapper
public interface FundTransactionMapper extends BaseMapper<FundTransaction> {
    // 继承BaseMapper，自动获得以下方法：
    // - insert(T entity): 插入一条记录
    // - deleteById(Serializable id): 根据ID删除记录
    // - updateById(T entity): 根据ID更新记录
    // - selectById(Serializable id): 根据ID查询记录
    // - selectList(Wrapper<T> queryWrapper): 根据条件查询记录列表
    // - selectPage(IPage<T> page, Wrapper<T> queryWrapper): 分页查询

}