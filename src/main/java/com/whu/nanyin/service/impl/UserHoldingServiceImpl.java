package com.whu.nanyin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.whu.nanyin.mapper.UserHoldingMapper;
import com.whu.nanyin.pojo.entity.FundBasicInfo;
import com.whu.nanyin.pojo.entity.FundTransaction;
import com.whu.nanyin.pojo.entity.UserHolding;
import com.whu.nanyin.pojo.entity.UserProfile;
import com.whu.nanyin.pojo.vo.UserHoldingVO;
import com.whu.nanyin.service.FundInfoService;
import com.whu.nanyin.service.UserHoldingService;
import com.whu.nanyin.service.UserProfileService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UserHoldingServiceImpl extends ServiceImpl<UserHoldingMapper, UserHolding> implements UserHoldingService {

    @Autowired
    private UserProfileService userProfileService;
    @Autowired
    private FundInfoService fundInfoService;

    // --- 【新增实现】补上接口中定义的 listByUserId 方法 ---
    @Override
    public List<UserHolding> listByuserId(Long userId) {
        QueryWrapper<UserHolding> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        return this.list(queryWrapper);
    }

    @Override
    @Transactional
    public void updateHoldingAfterNewTransaction(FundTransaction transaction) {
        QueryWrapper<UserHolding> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", transaction.getUserId())
                .eq("fund_code", transaction.getFundCode());
        UserHolding holding = this.getOne(queryWrapper);

        if (holding == null) {
            holding = new UserHolding();
            holding.setUserId(transaction.getUserId());
            holding.setFundCode(transaction.getFundCode());
            holding.setTotalShares(BigDecimal.ZERO);
            holding.setAverageCost(BigDecimal.ZERO);
        }

        if ("申购".equals(transaction.getTransactionType())) {
            BigDecimal newTotalShares = holding.getTotalShares().add(transaction.getTransactionShares());
            if (newTotalShares.compareTo(BigDecimal.ZERO) == 0) {
                 holding.setAverageCost(BigDecimal.ZERO);
            } else {
                BigDecimal newTotalCost = (holding.getAverageCost().multiply(holding.getTotalShares()))
                        .add(transaction.getTransactionAmount());
                holding.setAverageCost(newTotalCost.divide(newTotalShares, 4, RoundingMode.HALF_UP));
            }
            holding.setTotalShares(newTotalShares);
        } else if ("赎回".equals(transaction.getTransactionType())) {
            BigDecimal newTotalShares = holding.getTotalShares().subtract(transaction.getTransactionShares());
            holding.setTotalShares(newTotalShares);
        }

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

        List<String> fundCodes = topHoldings.stream().map(UserHolding::getFundCode).distinct().toList();
        Map<String, String> fundCodeToNameMap = fundInfoService.listAllBasicInfos().stream()
                .filter(info -> fundCodes.contains(info.getFundCode()))
                .collect(Collectors.toMap(FundBasicInfo::getFundCode, FundBasicInfo::getFundName));

        UserProfile userProfile = userProfileService.getUserProfileByUserId(userId);
        String userName = (userProfile != null) ? userProfile.getName() : "未知用户";

        return topHoldings.stream().map(holding -> {
            UserHoldingVO vo = new UserHoldingVO();
            BeanUtils.copyProperties(holding, vo);
            vo.setFundName(fundCodeToNameMap.get(holding.getFundCode()));
            vo.setUserName(userName); // <-- 现在可以正常调用了
            return vo;
        }).collect(Collectors.toList());
    }
}