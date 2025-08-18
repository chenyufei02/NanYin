package com.whu.nanyin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.whu.nanyin.mapper.UserHoldingMapper;
import com.whu.nanyin.pojo.entity.FundTransaction;
import com.whu.nanyin.pojo.entity.UserHolding;
import com.whu.nanyin.pojo.entity.UserProfile;
import com.whu.nanyin.pojo.vo.FundDetailVO;
import com.whu.nanyin.pojo.vo.UserHoldingVO;
import com.whu.nanyin.service.FundInfoService;
import com.whu.nanyin.service.UserHoldingService;
import com.whu.nanyin.service.UserProfileService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @description 用户持仓服务的实现类，负责具体的业务逻辑处理。
 * 继承自MyBatis-Plus的ServiceImpl，简化了与Mapper层的交互。
 */
@Service
public class UserHoldingServiceImpl extends ServiceImpl<UserHoldingMapper, UserHolding> implements UserHoldingService {

    // 自动注入UserProfileService，用于获取用户信息
    @Autowired
    private UserProfileService userProfileService;
    // 自动注入FundInfoService，用于获取基金的详细信息
    @Autowired
    private FundInfoService fundInfoService;

    /**
     * @description 根据用户ID查询其所有持仓记录。
     * @param userId 用户的唯一ID。
     * @return 该用户的所有持仓记录列表。
     */
    @Override
    public List<UserHolding> listByuserId(Long userId) {
        // 调用Mapper层定义的自定义方法来执行查询
        return baseMapper.listByUserId(userId);
    }

    /**
     * @description 根据用户ID和可选的基金代码或名称进行筛选查询。
     * @param userId   用户的唯一ID。
     * @param fundCode 基金代码（可选）。
     * @param fundName 基金名称（可选）。
     * @return 符合条件的持仓记录列表。
     */
    @Override
    public List<UserHolding> listByUserIdAndFundInfo(Long userId, String fundCode, String fundName) {
        // 调用Mapper层定义的带条件查询的方法
        return baseMapper.listByUserIdAndFundInfo(userId, fundCode, fundName);
    }

    /**
     * @description 核心业务方法：在发生新的交易后，同步更新持仓的份额、成本、基金名称及市值。
     * 使用@Transactional注解，确保整个方法在一个数据库事务中执行。
     * @param transaction 新发生的交易记录实体。
     */
    @Override
    @Transactional
    public void updateHoldingAfterNewTransaction(FundTransaction transaction) {
        // 1. 根据用户ID和基金代码，查找已存在的持仓记录
        QueryWrapper<UserHolding> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", transaction.getUserId())
                .eq("fund_code", transaction.getFundCode());
        UserHolding holding = this.getOne(queryWrapper);

        // 2. 获取基金的详细信息，为后续计算做准备
        FundDetailVO fundDetail = fundInfoService.getFundDetail(transaction.getFundCode());
        // 使用Assert进行断言，确保基金信息和业绩信息存在，否则抛出异常中断事务
        Assert.notNull(fundDetail, "交易失败：找不到基金 " + transaction.getFundCode() + " 的详细信息。");
        Assert.notNull(fundDetail.getPerformance(), "交易失败：基金 " + transaction.getFundCode() + " 暂无业绩信息。");

        BigDecimal latestNetValue = fundDetail.getPerformance().getUnitNetValue();
        Assert.notNull(latestNetValue, "交易失败：基金 " + transaction.getFundCode() + " 最新净值未知。");


        // 3. 如果不存在持仓记录，则创建一个新的持仓对象
        if (holding == null) {
            holding = new UserHolding();
            holding.setUserId(transaction.getUserId());
            holding.setFundCode(transaction.getFundCode());
            holding.setTotalShares(BigDecimal.ZERO);
            holding.setAverageCost(BigDecimal.ZERO);
            // 从基金详情中获取并设置基金名称
            holding.setFundName(fundDetail.getBasicInfo().getFundName());
        }

        // 4. 根据交易类型，更新份额和成本价
        BigDecimal newTotalShares;
        if ("申购".equals(transaction.getTransactionType())) {
            // 申购：增加总份额
            newTotalShares = holding.getTotalShares().add(transaction.getTransactionShares());
            if (newTotalShares.compareTo(BigDecimal.ZERO) > 0) {
                // 使用加权平均法重新计算平均成本
                BigDecimal newTotalCost = (holding.getAverageCost().multiply(holding.getTotalShares()))
                        .add(transaction.getTransactionAmount());
                holding.setAverageCost(newTotalCost.divide(newTotalShares, 4, RoundingMode.HALF_UP));
            } else {
                 holding.setAverageCost(BigDecimal.ZERO);
            }
        } else { // 赎回
            // 赎回：减少总份额 (平均成本不变)
            newTotalShares = holding.getTotalShares().subtract(transaction.getTransactionShares());
        }
        holding.setTotalShares(newTotalShares);

        // 5. 新增逻辑：计算并更新最新的市值和净值
        // 最新市值 = 最新的总份额 * 最新的基金净值
        BigDecimal newMarketValue = newTotalShares.multiply(latestNetValue).setScale(2, RoundingMode.HALF_UP);
        holding.setMarketValue(newMarketValue);
        holding.setLatestNetValue(latestNetValue);

        // 6. 更新时间戳并保存或更新持仓记录到数据库
        holding.setLastUpdateDate(LocalDateTime.now());
        this.saveOrUpdate(holding);
    }

    /**
     * @description 获取指定用户市值排名前N的持仓记录。
     * @param userId 用户的唯一ID。
     * @param limit  要返回的记录数量。
     * @return 包含持仓视图对象(VO)的列表。
     */
    @Override
    @Transactional(readOnly = true) // 设置为只读事务，提高查询性能
    public List<UserHoldingVO> getTopNHoldings(Long userId, int limit) {
        List<UserHolding> topHoldings;
        QueryWrapper<UserHolding> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId)
                    .orderByDesc("market_value") // 按市值降序排序
                    .last("LIMIT " + limit); // 限制返回的记录数
        topHoldings = this.list(queryWrapper);

        // 如果没有持仓记录，返回一个空列表
        if (topHoldings.isEmpty()) {
            return Collections.emptyList();
        }

        // 获取用户信息以填充VO中的userName字段
        UserProfile userProfile = userProfileService.getUserProfileByUserId(userId);
        String userName = (userProfile != null) ? userProfile.getName() : "未知用户";

        // 将持仓实体(Entity)列表转换为视图对象(VO)列表
        return topHoldings.stream().map(holding -> {
            UserHoldingVO vo = new UserHoldingVO();
            BeanUtils.copyProperties(holding, vo); // 属性复制
            vo.setUserName(userName); // 设置用户姓名
            return vo;
        }).collect(Collectors.toList());
    }
}