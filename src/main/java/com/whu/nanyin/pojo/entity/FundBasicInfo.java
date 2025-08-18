package com.whu.nanyin.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 基金基本信息实体类
 * 
 * 该实体类映射数据库中的fund_basic_info表，存储基金的静态基本信息
 * 在基金超市页面中用于展示基金列表，包含基金代码、名称、类型等基本信息
 * 同时也是基金详情页的基础数据来源
 */
@Data
@TableName("fund_basic_info")
public class FundBasicInfo {
    /**
     * 数据库自增主键
     */
    @TableId
    private Long seq;
    
    /**
     * 基金代码，是基金的唯一标识
     * 在基金超市页面作为主要的搜索和筛选条件
     */
    private String fundCode;
    
    /**
     * 基金简称，通常用于列表显示
     */
    private String abbreviation;
    
    /**
     * 基金全称，包含完整的基金名称信息
     */
    private String fundName;
    
    /**
     * 基金成立日期
     */
    private LocalDateTime establishedDate;
    
    /**
     * 基金投资类型代码
     * 0:股票型, 1:债券型, 2:混合型, 3:货币型, 6:基金型, 7:保本型, 8:REITs
     */
    private String fundInvestType;
    
    /**
     * 投资风格，如价值型、成长型、平衡型等
     */
    private String investStyle;
    
    /**
     * 业绩比较基准，用于评估基金表现的参考标准
     */
    private String perfComparativeBenchmark;
    
    /**
     * 基金经理姓名
     */
    private String managerName;
    
    /**
     * 基金托管人名称，通常是银行
     */
    private String custodianName;
    
    /**
     * 管理费率，基金公司收取的管理费用比例
     */
    private BigDecimal managementFeeRate;
    
    /**
     * 托管费率，基金托管人收取的托管费用比例
     */
    private BigDecimal custodianFeeRate;
    
    /**
     * 销售服务费率，基金销售机构收取的服务费用比例
     */
    private BigDecimal salesServiceFeeRate;

    /**
     * 最新单位净值
     * 非数据库字段，在基金超市页面中显示基金的最新净值
     * 数据来源于fund_net_value_perf_rank表，在查询时由Java代码动态填充
     */
    @TableField(exist = false)
    private BigDecimal latestNetValue;

    /**
     * 最新日涨跌幅
     * 非数据库字段，在基金超市页面中显示基金的日涨跌幅
     * 数据来源于fund_net_value_perf_rank表，在查询时由Java代码动态填充
     */
    @TableField(exist = false)
    private BigDecimal dailyGrowthRate;



}