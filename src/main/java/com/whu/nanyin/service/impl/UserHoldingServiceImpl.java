// 文件路径: src/main/java/com/whu/nanyin/service/impl/UserHoldingServiceImpl.java
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

@Service
public class UserHoldingServiceImpl extends ServiceImpl<UserHoldingMapper, UserHolding> implements UserHoldingService {

    @Autowired
    private UserProfileService userProfileService;
    @Autowired
    private FundInfoService fundInfoService;

    @Override
    public List<UserHolding> listByuserId(Long userId) { return baseMapper.listByUserId(userId);
    }

    @Override
    public List<UserHolding> listByUserIdAndFundInfo(Long userId, String fundCode, String fundName) {
        return baseMapper.listByUserIdAndFundInfo(userId, fundCode, fundName);}
    /**
     * 【核心修正】处理新交易时，同步更新份额、成本、基金名称和【市值】
     */
    @Override
    @Transactional
    public void updateHoldingAfterNewTransaction(FundTransaction transaction) {
        // 1. 查找或创建持仓记录
        QueryWrapper<UserHolding> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", transaction.getUserId())
                .eq("fund_code", transaction.getFundCode());
        UserHolding holding = this.getOne(queryWrapper);

        // 获取基金的详细信息，为后续计算做准备
        FundDetailVO fundDetail = fundInfoService.getFundDetail(transaction.getFundCode());
        Assert.notNull(fundDetail, "交易失败：找不到基金 " + transaction.getFundCode() + " 的详细信息。");
        Assert.notNull(fundDetail.getPerformance(), "交易失败：基金 " + transaction.getFundCode() + " 暂无业绩信息。");

        BigDecimal latestNetValue = fundDetail.getPerformance().getUnitNetValue();
        Assert.notNull(latestNetValue, "交易失败：基金 " + transaction.getFundCode() + " 最新净值未知。");


        if (holding == null) {
            holding = new UserHolding();
            holding.setUserId(transaction.getUserId());
            holding.setFundCode(transaction.getFundCode());
            holding.setTotalShares(BigDecimal.ZERO);
            holding.setAverageCost(BigDecimal.ZERO);
            holding.setFundName(fundDetail.getBasicInfo().getFundName());
        }

        // 2. 根据交易类型，更新份额和成本
        BigDecimal newTotalShares;
        if ("申购".equals(transaction.getTransactionType())) {
            newTotalShares = holding.getTotalShares().add(transaction.getTransactionShares());
            if (newTotalShares.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal newTotalCost = (holding.getAverageCost().multiply(holding.getTotalShares()))
                        .add(transaction.getTransactionAmount());
                holding.setAverageCost(newTotalCost.divide(newTotalShares, 4, RoundingMode.HALF_UP));
            } else {
                 holding.setAverageCost(BigDecimal.ZERO);
            }
        } else { // 赎回
            newTotalShares = holding.getTotalShares().subtract(transaction.getTransactionShares());
        }
        holding.setTotalShares(newTotalShares);

        // --- 【【【 新增逻辑：计算并更新最新市值 】】】 ---
        // 最新市值 = 最新总份额 * 最新净值
        BigDecimal newMarketValue = newTotalShares.multiply(latestNetValue).setScale(2, RoundingMode.HALF_UP);
        holding.setMarketValue(newMarketValue);

        // 3. 更新时间戳并保存
        holding.setLastUpdateDate(LocalDateTime.now());
        this.saveOrUpdate(holding);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserHoldingVO> getTopNHoldings(Long userId, int limit) {
        QueryWrapper<UserHolding> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId)
                    .orderByDesc("market_value")
                    .last("LIMIT " + limit);
        List<UserHolding> topHoldings = this.list(queryWrapper);

        if (topHoldings.isEmpty()) {
            return Collections.emptyList();
        }

        UserProfile userProfile = userProfileService.getUserProfileByUserId(userId);
        String userName = (userProfile != null) ? userProfile.getName() : "未知用户";

        return topHoldings.stream().map(holding -> {
            UserHoldingVO vo = new UserHoldingVO();
            BeanUtils.copyProperties(holding, vo);
            vo.setUserName(userName);
            return vo;
        }).collect(Collectors.toList());
    }
}