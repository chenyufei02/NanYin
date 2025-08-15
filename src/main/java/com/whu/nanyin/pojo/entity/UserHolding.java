package com.whu.nanyin.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("user_holdings")
public class UserHolding {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String fundCode;
    private BigDecimal totalShares;
    private BigDecimal marketValue;
    private BigDecimal averageCost;
    private LocalDateTime lastUpdateDate;
    private String fundName;
    
    // 最新净值字段，从fund_net_value表中获取
    @TableField(exist = false)
    private BigDecimal latestNetValue;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}