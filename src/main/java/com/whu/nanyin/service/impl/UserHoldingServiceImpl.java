package com.whu.nanyin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.whu.nanyin.mapper.UserHoldingMapper;
import com.whu.nanyin.pojo.entity.FundTransaction;
import com.whu.nanyin.pojo.entity.UserHolding;
import com.whu.nanyin.pojo.vo.FundDetailVO;
import com.whu.nanyin.service.FundInfoService;
import com.whu.nanyin.service.UserHoldingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

// 继承自MyBatis-Plus的ServiceImpl，简化了与Mapper层的交互。
@Service
public class UserHoldingServiceImpl extends ServiceImpl<UserHoldingMapper, UserHolding> implements UserHoldingService {


    @Autowired
    private FundInfoService fundInfoService;

    /**
     * @description 根据用户ID查询其所有持仓记录。
     * @param userId 用户的唯一ID。
     * @return 该用户的所有持仓记录列表。
     */
    @Override
    public List<UserHolding> listByuserId(Long userId) {
        // baseMapper是来自MP的ServiceImpl父类提供的mapper 在继承ServiceImpl父类的时候传入了参数
        // <UserHoldingMapper, UserHolding> 表明是对UserHolding对象进行处理 并已自动注入UserHoldingMapper实例继承baseMapper
        // 因此这里直接使用baseMapper进行调用即可，也因此后面的方法是自己注入的UserHoldingMapper里写好的方法
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
        // 同上
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
    
        // 5. 检查赎回后份额是否为0，如果为0则删除持仓记录
        if ("赎回".equals(transaction.getTransactionType()) && 
            newTotalShares.compareTo(BigDecimal.ZERO) <= 0) {
            // 份额为0或负数时，删除持仓记录
            if (holding.getId() != null) {
                this.removeById(holding.getId());
            }
            return; // 删除后直接返回，不需要更新记录
        }
    
        // 6.  最新市值 = 最新的总份额 * 最新的基金净值
        BigDecimal newMarketValue = newTotalShares.multiply(latestNetValue).setScale(2, RoundingMode.HALF_UP);
        holding.setMarketValue(newMarketValue);
        holding.setLatestNetValue(latestNetValue);
    
        // 7. 更新时间戳并保存或更新持仓记录到数据库
        holding.setLastUpdateDate(LocalDateTime.now());
        this.saveOrUpdate(holding);
    }

}