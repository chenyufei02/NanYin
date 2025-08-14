package com.whu.nanyin.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 对应数据库中的 fund_net_value_perf_rank 表
 * 存储基金的业绩表现与同类排名信息
 */
@Data
@TableName("fund_net_value_perf_rank")
public class FundNetValuePerfRank {

    @TableId
    private Long seq;

    private String fundCode;
    private LocalDateTime endDate;
    private BigDecimal unitNetValue;
    private BigDecimal accumNetValue;

    // --- 【核心修正】使用@TableField注解，精确映射所有字段 ---

    @TableField("daily_growth_rate")
    private BigDecimal dailyGrowthRate;

    @TableField("weekly_growth_rate")
    private BigDecimal weeklyGrowthRate;

    @TableField("monthly_1m_growth_rate")
    private BigDecimal monthly1mGrowthRate;

    @TableField("monthly_3m_growth_rate")
    private BigDecimal monthly3mGrowthRate;

    @TableField("monthly_6m_growth_rate")
    private BigDecimal monthly6mGrowthRate;

    @TableField("yearly_1y_growth_rate")
    private BigDecimal yearly1yGrowthRate;

    @TableField("yearly_2y_growth_rate")
    private BigDecimal yearly2yGrowthRate;

    @TableField("yearly_3y_growth_rate")
    private BigDecimal yearly3yGrowthRate;

    @TableField("yearly_5y_growth_rate")
    private BigDecimal yearly5yGrowthRate;

    @TableField("from_establishment_growth_rate")
    private BigDecimal fromEstablishmentGrowthRate;

    @TableField("rank_1y")
    private BigDecimal rank1y;

    @TableField("rank_base_1y")
    private BigDecimal rankBase1y;

}