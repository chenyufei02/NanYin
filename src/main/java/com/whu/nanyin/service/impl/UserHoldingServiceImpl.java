package com.whu.nanyin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.whu.nanyin.mapper.UserHoldingMapper;
import com.whu.nanyin.pojo.entity.UserHolding;
import com.whu.nanyin.pojo.entity.UserProfile;
import com.whu.nanyin.pojo.entity.FundInfo;
import com.whu.nanyin.pojo.entity.FundTransaction;
import com.whu.nanyin.pojo.vo.UserHoldingVO;
import com.whu.nanyin.service.UserHoldingService;
import com.whu.nanyin.service.UserProfileService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.whu.nanyin.service.FundInfoService;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserHoldingServiceImpl extends ServiceImpl<UserHoldingMapper, UserHolding> implements UserHoldingService {


    @Autowired
    private UserProfileService userProfileService;
    @Autowired
    private FundInfoService fundInfoService;

    /**
     * 根据ID查询持仓情况
     *
     * @param
     * @return java.util.List<com.whu.nanyin.pojo.entity.UserHolding>
     * @author yufei
     * @since 2025/7/4
     */
    @Override
    public List<UserHolding> listByuserId(Long userId) {
        QueryWrapper<UserHolding> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        return this.list(queryWrapper);
    }


    /**
     * 处理一笔新交易后自动更新客户持仓
     *
     * @author yufei
     * @since 2025/7/5
     */
    @Override
    @Transactional
    public void updateHoldingAfterNewTransaction(FundTransaction transaction) {
        // 1. 查找该客户对该基金是否已有持仓记录
        QueryWrapper<UserHolding> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("customer_id", transaction.getUserId())
                .eq("fund_code", transaction.getFundCode());
        UserHolding holding = this.getOne(queryWrapper);

        if (holding == null) { // 如果没有持仓记录，说明是首次购买
            holding = new UserHolding();
            holding.setUserId(transaction.getUserId());
            holding.setFundCode(transaction.getFundCode());
            holding.setTotalShares(BigDecimal.ZERO);
            holding.setAverageCost(BigDecimal.ZERO);
        }

        // 2. 根据交易类型，更新份额和成本
        if ("申购".equals(transaction.getTransactionType())) {
            // 新总份额 = 原份额 + 新交易份额
            BigDecimal newTotalShares = holding.getTotalShares().add(transaction.getTransactionShares());
            // 新总成本 = (原成本*原份额 + 新交易金额)
            BigDecimal newTotalCost = (holding.getAverageCost().multiply(holding.getTotalShares()))
                    .add(transaction.getTransactionAmount());

            holding.setTotalShares(newTotalShares);
            // 新平均成本 = 新总成本/新总份额
            holding.setAverageCost(newTotalCost.divide(newTotalShares, 4, RoundingMode.HALF_UP));

        } else if ("赎回".equals(transaction.getTransactionType())) {
            // 赎回不影响平均成本，所以不做处理。只对总份额做处理
            BigDecimal newTotalShares = holding.getTotalShares().subtract(transaction.getTransactionShares());
            holding.setTotalShares(newTotalShares);
        }

        holding.setLastUpdateDate(LocalDateTime.now());

        // 3. 保存或更新持仓记录到数据库
        this.saveOrUpdate(holding);
    }




    @Override
    @Transactional(readOnly = true)
    public List<UserHoldingVO> getTopNHoldings(Long userId, int limit) {
        // 1. 构建查询，按市值降序，并限制数量
        QueryWrapper<UserHolding> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("customer_id", userId)
                    .orderByDesc("market_value")
                    .last("LIMIT " + limit);

        List<UserHolding> topHoldings = this.list(queryWrapper);

        if (topHoldings.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 复用已有逻辑，批量获取关联的基金信息
        List<String> fundCodes = topHoldings.stream().map(UserHolding::getFundCode).distinct().collect(Collectors.toList());
        Map<String, String> fundCodeToNameMap = fundInfoService.listByIds(fundCodes).stream()
                .collect(Collectors.toMap(FundInfo::getFundCode, FundInfo::getFundName));

        // 我们只需要客户自己的名字，可以直接从第一个持仓记录中获取客户ID来查询
        UserProfile userProfile = userProfileService.getById(topHoldings.get(0).getUserId());
        String customerName = (userProfile != null) ? userProfile.getName() : "未知客户";

        // 3. 组装VO列表
        return topHoldings.stream().map(holding -> {
            UserHoldingVO vo = new UserHoldingVO();
            BeanUtils.copyProperties(holding, vo);
            vo.setFundName(fundCodeToNameMap.get(holding.getFundCode()));
            vo.setCustomerName(customerName); // 所有记录都用同一个客户名
            return vo;
        }).collect(Collectors.toList());
    }



}