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

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}