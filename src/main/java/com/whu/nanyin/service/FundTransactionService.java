package com.whu.nanyin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.whu.nanyin.pojo.dto.FundPurchaseDTO;
import com.whu.nanyin.pojo.dto.FundRedeemDTO;
import com.whu.nanyin.pojo.entity.FundTransaction;

import java.util.List;

public interface FundTransactionService extends IService<FundTransaction> {
    // 创建一个新方法，它不仅保存交易，还负责触发持仓更新
    FundTransaction createPurchaseTransaction(FundPurchaseDTO dto);

    FundTransaction createRedeemTransaction(FundRedeemDTO dto);

    List<FundTransaction> listByUserId(Long userId);


    // 根据交易ID和用户ID，安全地获取单条交易详情
    FundTransaction getTransactionByIdAndUserId(Long transactionId, Long userId);

}