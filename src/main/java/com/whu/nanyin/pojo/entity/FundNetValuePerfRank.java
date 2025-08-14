package com.whu.nanyin.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("fund_net_value_perf_rank")
public class FundNetValuePerfRank {

    @TableId
    private Long seq;

    private String fundCode;
    private LocalDateTime endDate;
    private BigDecimal unitNetValue;
    private BigDecimal accumNetValue;

    // --- 业绩增长率字段 ---
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

    // --- 【【【 核心修正：补全所有排名相关字段 】】】 ---
    @TableField("rank_1y")
    private Integer rank1y;
    @TableField("rank_base_1y")
    private Integer rankBase1y;

    @TableField("rank_2y")
    private Integer rank2y;
    @TableField("rank_base_2y")
    private Integer rankBase2y;

    @TableField("rank_3y")
    private Integer rank3y;
    @TableField("rank_base_3y")
    private Integer rankBase3y;

    @TableField("rank_5y")
    private Integer rank5y;
    @TableField("rank_base_5y")
    private Integer rankBase5y;

    // --- 根据数据库截图补全周、月、半年排名 ---
    @TableField("rank_1w")
    private Integer rank1w;
    @TableField("rank_base_1w")
    private Integer rankBase1w;

    @TableField("rank_1m")
    private Integer rank1m;
    @TableField("rank_base_1m")
    private Integer rankBase1m;

    @TableField("rank_3m")
    private Integer rank3m;
    @TableField("rank_base_3m")
    private Integer rankBase3m;

    @TableField("rank_6m")
    private Integer rank6m;
    @TableField("rank_base_6m")
    private Integer rankBase6m;

    @TableField("rank_establishment")
    private Integer rankEstablishment;
    @TableField("rank_base_establishment")
    private Integer rankBaseEstablishment;
}