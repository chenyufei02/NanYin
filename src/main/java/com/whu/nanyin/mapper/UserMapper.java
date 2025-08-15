package com.whu.nanyin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.whu.nanyin.pojo.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    /**
     * 原子扣减可用余额（余额充足才扣减）
     * @param userId 用户ID
     * @param amount 扣减金额（正数）
     * @return 受影响行数（1 表示成功扣减，0 表示余额不足）
     */
    @Update("UPDATE users SET balance = balance - #{amount} WHERE id = #{userId} AND balance >= #{amount}")
    int deductBalanceIfEnough(@Param("userId") Long userId, @Param("amount") java.math.BigDecimal amount);
}