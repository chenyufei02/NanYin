package com.whu.nanyin.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("fund_basic_info")
public class FundBasicInfo {
    @TableId
    private Long seq;
    private String fundCode;
    private String abbreviation;
    private String fundName;
    private LocalDateTime establishedDate;
    private String fundInvestType;
    private String investStyle;
    private String perfComparativeBenchmark;
    private String managerName;
    private String custodianName;
    private BigDecimal managementFeeRate;
    private BigDecimal custodianFeeRate;
    private BigDecimal salesServiceFeeRate;

    @TableField(exist = false)
    private BigDecimal latestNetValue; // 用于临时存放最新的单位净值

    @TableField(exist = false)
    private BigDecimal dailyGrowthRate; // 用于临时存放最新的日涨跌幅



}