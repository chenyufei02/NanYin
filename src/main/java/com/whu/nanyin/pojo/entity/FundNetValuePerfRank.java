package com.whu.nanyin.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 基金净值业绩与排名实体类
 * 
 * 该实体类映射数据库中的fund_net_value_perf_rank表
 * 存储基金的净值、收益率和同类排名等业绩数据
 * 在基金详情页中用于展示基金的业绩表现和同类排名情况
 */
import java.time.LocalDateTime;

@Data
@TableName("fund_net_value_perf_rank")
public class FundNetValuePerfRank {

    /**
     * 主键序列号
     */
    @TableId
    private Long seq;

    /**
     * 基金代码
     * 用于唯一标识基金
     */
    private String fundCode;
    
    /**
     * 净值日期
     */
    private LocalDateTime endDate;
    
    /**
     * 单位净值
     */
    private BigDecimal unitNetValue;
    
    /**
     * 累计净值，包含所有分红再投资的累计净值
     */
    private BigDecimal accumNetValue;

    // --- 业绩增长率字段 ---
    /**
     * 日增长率
     */
    @TableField("daily_growth_rate")
    private BigDecimal dailyGrowthRate;
    
    /**
     * 周增长率
     */
    @TableField("weekly_growth_rate")
    private BigDecimal weeklyGrowthRate;
    
    /**
     * 近1个月增长率
     */
    @TableField("monthly_1m_growth_rate")
    private BigDecimal monthly1mGrowthRate;
    
    /**
     * 近3个月增长率
     * 基金最近3个月的收益率
     */
    @TableField("monthly_3m_growth_rate")
    private BigDecimal monthly3mGrowthRate;
    
    /**
     * 近6个月增长率
     */
    @TableField("monthly_6m_growth_rate")
    private BigDecimal monthly6mGrowthRate;
    
    /**
     * 近1年增长率
     */
    @TableField("yearly_1y_growth_rate")
    private BigDecimal yearly1yGrowthRate;
    
    /**
     * 近2年增长率
     */
    @TableField("yearly_2y_growth_rate")
    private BigDecimal yearly2yGrowthRate;
    
    /**
     * 近3年增长率
     */
    @TableField("yearly_3y_growth_rate")
    private BigDecimal yearly3yGrowthRate;
    
    /**
     * 近5年增长率
     */
    @TableField("yearly_5y_growth_rate")
    private BigDecimal yearly5yGrowthRate;
    
    /**
     * 成立以来增长率
     */
    @TableField("from_establishment_growth_rate")
    private BigDecimal fromEstablishmentGrowthRate;

    // --- 排名相关字段 ---
    /**
     * 近1年排名
     * 在基金详情页中用于展示基金的相对表现
     */
    @TableField("rank_1y")
    private Integer rank1y;
    
    /**
     * 近1年排名基数
     */
    @TableField("rank_base_1y")
    private Integer rankBase1y;

    /**
     * 近2年排名
     */
    @TableField("rank_2y")
    private Integer rank2y;
    
    /**
     * 近2年排名基数
     */
    @TableField("rank_base_2y")
    private Integer rankBase2y;

    /**
     * 近3年排名
     */
    @TableField("rank_3y")
    private Integer rank3y;
    
    /**
     * 近3年排名基数
     */
    @TableField("rank_base_3y")
    private Integer rankBase3y;

    /**
     * 近5年排名
     */
    @TableField("rank_5y")
    private Integer rank5y;
    
    /**
     * 近5年排名基数
     */
    @TableField("rank_base_5y")
    private Integer rankBase5y;

    /**
     * 近1周排名
     */
    @TableField("rank_1w")
    private Integer rank1w;
    
    /**
     * 近1周排名基数
     */
    @TableField("rank_base_1w")
    private Integer rankBase1w;

    /**
     * 近1月排名
     */
    @TableField("rank_1m")
    private Integer rank1m;
    
    /**
     * 近1月排名基数
     */
    @TableField("rank_base_1m")
    private Integer rankBase1m;

    /**
     * 近3月排名
     */
    @TableField("rank_3m")
    private Integer rank3m;
    
    /**
     * 近3月排名基数
     */
    @TableField("rank_base_3m")
    private Integer rankBase3m;

    /**
     * 近6月排名
     */
    @TableField("rank_6m")
    private Integer rank6m;
    
    /**
     * 近6月排名基数
     */
    @TableField("rank_base_6m")
    private Integer rankBase6m;

    /**
     * 成立以来排名
     */
    @TableField("rank_establishment")
    private Integer rankEstablishment;
    
    /**
     * 成立以来排名基数
     */
    @TableField("rank_base_establishment")
    private Integer rankBaseEstablishment;
}