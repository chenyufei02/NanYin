package com.whu.nanyin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.whu.nanyin.exception.InsufficientFundsException;
import com.whu.nanyin.mapper.FundTransactionMapper;
import com.whu.nanyin.mapper.UserMapper;
import com.whu.nanyin.pojo.dto.FundPurchaseDTO;
import com.whu.nanyin.pojo.dto.FundRedeemDTO;
import com.whu.nanyin.pojo.entity.User;
import com.whu.nanyin.pojo.entity.UserHolding;
import com.whu.nanyin.pojo.entity.FundTransaction;
import com.whu.nanyin.pojo.vo.FundDetailVO;
import com.whu.nanyin.pojo.vo.FundTransactionVO;
import com.whu.nanyin.service.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * 基金交易服务实现类
 * 负责处理申购和赎回的核心业务逻辑
 */
@Service
@Slf4j
public class FundTransactionServiceImpl extends ServiceImpl<FundTransactionMapper, FundTransaction> implements FundTransactionService {


    @Autowired
    private FundInfoService fundInfoService;

    /**
     * 使用@Lazy注解懒加载客户持仓服务，以解决循环依赖问题
     */
    @Autowired
    @Lazy
    private UserHoldingService userHoldingService;

    @Autowired
    private UserMapper userMapper;

    /**
     * 在一个事务内处理基金申购业务 和 更新客户持仓数据
     * @param dto 包含申购信息的DTO对象
     * @return 创建并保存好的交易记录实体
     */
    @Override
    @Transactional
    public FundTransaction createPurchaseTransaction(FundPurchaseDTO dto) {
        // 1. 校验并扣除用户余额...
        User user = userMapper.selectById(dto.getUserId());
        if (user == null || user.getBalance().compareTo(dto.getTransactionAmount()) < 0) {
            throw new InsufficientFundsException("购买失败：账户余额不足。");
        }
        user.setBalance(user.getBalance().subtract(dto.getTransactionAmount()));
        userMapper.updateById(user);

        // 2. 获取基金信息...
        FundDetailVO fundDetail = fundInfoService.getFundDetail(dto.getFundCode());
        Assert.notNull(fundDetail, "找不到对应的基金信息：" + dto.getFundCode());
        Assert.notNull(fundDetail.getPerformance(), "该基金暂无有效的业绩信息，无法交易。");
        BigDecimal sharePrice = fundDetail.getPerformance().getUnitNetValue();
        Assert.notNull(sharePrice, "该基金暂无有效的净值信息，无法交易。");

        // 3. 创建交易实体并手动赋值
        FundTransaction transaction = new FundTransaction();
        transaction.setUserId(dto.getUserId());
        transaction.setFundCode(dto.getFundCode());
        transaction.setTransactionAmount(dto.getTransactionAmount());
        transaction.setTransactionTime(dto.getTransactionTime());

        // --- 【【【 终极修正：手动、强制地设置银行卡号 】】】 ---
        transaction.setBankAccountNumber(dto.getBankAccountNumber());

        transaction.setTransactionType("申购");
        transaction.setSharePrice(sharePrice);
        transaction.setStatus("成功");

        BigDecimal shares = dto.getTransactionAmount().divide(sharePrice, 4, RoundingMode.HALF_UP);
        transaction.setTransactionShares(shares);

        // 4. 保存交易并更新持仓
        return saveTransactionAndUpdateHolding(transaction);
    }

    /**
     * 【核心修正】处理基金赎回业务
     */
    @Override
    @Transactional
    public FundTransaction createRedeemTransaction(FundRedeemDTO dto) {
        // 1. 校验用户持仓份额是否足够
        QueryWrapper<UserHolding> holdingQuery = new QueryWrapper<>();
        holdingQuery.eq("user_id", dto.getUserId()).eq("fund_code", dto.getFundCode());
        UserHolding currentHolding = userHoldingService.getOne(holdingQuery);
        if (currentHolding == null || dto.getTransactionShares().compareTo(currentHolding.getTotalShares()) > 0) {
            String availableShares = (currentHolding != null) ? currentHolding.getTotalShares().toPlainString() : "0";
            throw new InsufficientFundsException("赎回失败：份额不足。当前持有 " + availableShares + " 份，尝试赎回 " + dto.getTransactionShares().toPlainString() + " 份。");
        }

        // 2. 获取基金最新净值
        FundDetailVO fundDetail = fundInfoService.getFundDetail(dto.getFundCode());
        Assert.notNull(fundDetail, "找不到对应的基金信息：" + dto.getFundCode());
        Assert.notNull(fundDetail.getPerformance(), "该基金暂无有效的业绩信息，无法交易。");
        BigDecimal sharePrice = fundDetail.getPerformance().getUnitNetValue();
        Assert.notNull(sharePrice, "该基金暂无有效的净值信息，无法交易。");

        // 3. 计算赎回可获得的金额 (份额 * 净值)，保留2位小数
        BigDecimal redeemAmount = dto.getTransactionShares().multiply(sharePrice).setScale(2, RoundingMode.HALF_UP);

        // 4. 将赎回金额增加到用户的可用余额中
        User user = userMapper.selectById(dto.getUserId());
        user.setBalance(user.getBalance().add(redeemAmount));
        userMapper.updateById(user);

        // 5. 创建并保存交易记录实体
        FundTransaction transaction = new FundTransaction();
        transaction.setUserId(dto.getUserId());
        transaction.setFundCode(dto.getFundCode());
        transaction.setTransactionShares(dto.getTransactionShares());
        transaction.setTransactionTime(dto.getTransactionTime());
        transaction.setBankAccountNumber(dto.getBankAccountNumber());
        transaction.setTransactionType("赎回");
        transaction.setSharePrice(sharePrice);
        transaction.setStatus("成功");

        // --- 【【【 Bug修复：手动设置计算出的赎回金额 】】】 ---
        transaction.setTransactionAmount(redeemAmount);

        // 6. 保存交易记录并触发持仓更新
        return saveTransactionAndUpdateHolding(transaction);
    }

    public List<FundTransaction> listByUserId(Long userId) {
        QueryWrapper<FundTransaction> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        queryWrapper.orderByAsc("id"); // 按ID升序排序，确保从1开始
        return this.list(queryWrapper);
    }

    @Override
    public List<FundTransactionVO> listByUserIdWithFundName(Long userId) {
        List<FundTransaction> transactions = listByUserId(userId);
        return transactions.stream().map(transaction -> {
            FundTransactionVO vo = new FundTransactionVO();
            BeanUtils.copyProperties(transaction, vo);
            
            // 根据fund_code查询基金名称
            FundDetailVO fundDetail = fundInfoService.getFundDetail(transaction.getFundCode());
            if (fundDetail != null && fundDetail.getBasicInfo() != null) {
                vo.setFundName(fundDetail.getBasicInfo().getFundName());
            }
            
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 私有辅助方法，用于统一处理保存交易和更新持仓的逻辑
     * @param transaction 已经构建好的交易实体
     * @return 保存后的交易实体（包含数据库生成的ID）
     */
    private FundTransaction saveTransactionAndUpdateHolding(FundTransaction transaction) {
        // 步骤1：将交易记录保存到数据库
        boolean ok = this.save(transaction);
        if (!ok || transaction.getId() == null) {
            log.error("[Purchase] Save transaction failed, tx={}", transaction);
            throw new RuntimeException("保存交易失败");
        }
        // 步骤2：调用客户持仓服务，根据这笔新交易实时更新持仓信息
        userHoldingService.updateHoldingAfterNewTransaction(transaction);
        // 步骤3：返回包含ID的完整交易实体
        return transaction;
    }


    // 【新增】实现安全获取方法
    @Override
    public FundTransaction getTransactionByIdAndUserId(Long transactionId, Long userId) {
        FundTransaction transaction = this.getById(transactionId);

        // 安全校验：如果交易不存在，或者交易的userId与当前登录的userId不匹配
        if (transaction == null || !transaction.getUserId().equals(userId)) {
            // 抛出异常，Spring Security会捕获并返回403 Forbidden错误
            throw new AccessDeniedException("无权访问此交易记录");
        }

        return transaction;
    }

    @Override
    public FundTransactionVO getTransactionByIdAndUserIdWithFundName(Long transactionId, Long userId) {
        FundTransaction transaction = getTransactionByIdAndUserId(transactionId, userId);
        
        FundTransactionVO vo = new FundTransactionVO();
        BeanUtils.copyProperties(transaction, vo);
        
        // 根据fund_code查询基金名称
        FundDetailVO fundDetail = fundInfoService.getFundDetail(transaction.getFundCode());
        if (fundDetail != null && fundDetail.getBasicInfo() != null) {
            vo.setFundName(fundDetail.getBasicInfo().getFundName());
        }
        
        return vo;
    }



}