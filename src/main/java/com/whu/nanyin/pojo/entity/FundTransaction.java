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

    private Long userId;

    private String fundCode;
    private String transactionType;
    private BigDecimal transactionAmount;
    private BigDecimal transactionShares;
    private BigDecimal sharePrice;
    private LocalDateTime transactionTime;
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}