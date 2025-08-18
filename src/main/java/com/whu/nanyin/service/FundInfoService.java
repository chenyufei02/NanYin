package com.whu.nanyin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.whu.nanyin.pojo.entity.FundBasicInfo;
import com.whu.nanyin.pojo.vo.FundDetailVO;
import com.whu.nanyin.pojo.vo.FundNetValueTrendVO;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 基金信息服务接口
 * 
 * 提供基金相关的业务逻辑，包括：
 * 1. 基金超市页面的分页查询和条件筛选
 * 2. 基金详情页的数据聚合
 * 3. 基金净值走势数据查询
 * 
 * 该接口是基金模块的核心服务接口，为前端提供所需的各类基金数据
 */
public interface FundInfoService {

    /**
     * 为“基金超市”页面提供分页和条件搜索功能。
     * @param page 分页对象
     * @param fundCode 基金代码 (模糊查询)
     * @param fundName 基金名称 (模糊查询)
     * @param fundType 基金类型 (精确查询)
     * @return 包含查询结果的分页对象
     */
    Page<FundBasicInfo> getFundBasicInfoPage(Page<FundBasicInfo> page, String fundCode, String fundName, String fundType);

    /**
     * 为"基金详情页"提供所有需要的聚合数据
     * 
     * 该方法是基金详情页的核心方法，负责聚合以下数据：
     * 1. 基金基本信息（名称、代码、类型、成立日期等）
     * 2. 基金最新业绩表现与排名（最新净值、日增长率、各期收益率、同类排名等）
     * 3. 基金历史净值走势数据（过去一年的净值数据，用于绘制走势图）
     * 
     * 前端基金详情页通过调用此方法获取所有需要展示的数据
     * 
     * @param fundCode 基金代码，用于唯一标识要查询的基金
     * @return 包含基金所有详细信息的FundDetailVO对象，如果基金不存在则返回null
     */
    FundDetailVO getFundDetail(String fundCode);

    /**
     * 【新增】获取所有基金的基础信息列表
     * @return 所有基金基础信息的列表
     */
    List<FundBasicInfo> listAllBasicInfos();

    /**
     * 获取指定时间范围内的基金净值走势数据
     * 
     * 该方法用于基金详情页的净值走势图表，支持查询一个或多个基金在指定时间范围内的净值数据
     * 返回的数据按基金代码分组，每组包含该基金在时间范围内的所有净值记录
     * 
     * 前端可以使用此数据绘制净值走势图表，进行基金业绩对比分析等功能
     * 如果未指定开始日期，默认为一年前；如果未指定结束日期，默认为当前时间
     * 
     * @param fundCodes 基金代码列表，支持同时查询多个基金的净值数据
     * @param startDate 开始日期，可为null（默认为一年前）
     * @param endDate 结束日期，可为null（默认为当前时间）
     * @return 按基金代码分组的净值走势数据，Map的key为基金代码，value为该基金的净值走势数据列表
     */
    Map<String, List<FundNetValueTrendVO>> getFundNetValueTrends(
        List<String> fundCodes,
        LocalDateTime startDate,
        LocalDateTime endDate
    );

}