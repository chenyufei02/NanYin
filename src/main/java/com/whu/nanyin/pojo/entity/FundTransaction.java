package com.whu.nanyin.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("user_transactions")
public class FundTransaction {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("fund_code")
    private String fundCode;
    @TableField("transaction_type")
    private String transactionType;
    @TableField("transaction_amount")
    private BigDecimal transactionAmount;
    @TableField("transaction_shares")
    private BigDecimal transactionShares;
    @TableField("share_price")
    private BigDecimal sharePrice;
    @TableField("transaction_time")
    private LocalDateTime transactionTime;
    @TableField("status")
    private String status;

    @TableField("bank_account_number")
    private String bankAccountNumber;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}