package com.whu.nanyin.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableField;

@Data
@NoArgsConstructor
@TableName("user_financial_profiles") // <-- 1. 修改表名
public class UserFinancialProfile {

    @TableId
    private Long userId; // <-- 2. 修改字段名，并设为主键

    // 总资产规模（总市值）
    private BigDecimal totalMarketValue;

    // 平均持仓周期
    private Integer avgHoldingDays;

    // 最近交易日期距今的天数（R）
    private Integer recencyDays;

    // 90天内交易频率（F）
    @TableField("frequency_90d")
    private Integer frequency90d;

    // 定投行为（F）
    private Boolean hasRegularInvestment;

    private LocalDateTime updateTime;

    @Version
    private Integer version;


}