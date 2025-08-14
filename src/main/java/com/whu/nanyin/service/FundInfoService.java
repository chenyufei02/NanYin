package com.whu.nanyin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.whu.nanyin.pojo.entity.FundBasicInfo;
import com.whu.nanyin.pojo.vo.FundDetailVO; // <-- 需要新建这个VO
import com.whu.nanyin.pojo.vo.FundNetValueTrendVO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
     * 为“基金详情页”提供所有需要的聚合数据。
     * @param fundCode 基金代码
     * @return 包含基金所有详细信息的VO对象
     */
    FundDetailVO getFundDetail(String fundCode);

    /**
     * 【新增】获取所有基金的基础信息列表
     * @return 所有基金基础信息的列表
     */
    List<FundBasicInfo> listAllBasicInfos();

    /**
     * 获取指定时间范围内的基金净值走势数据
     * @param fundCodes 基金代码列表
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 按基金代码分组的净值走势数据
     */
    Map<String, List<FundNetValueTrendVO>> getFundNetValueTrends(
        List<String> fundCodes,
        LocalDateTime startDate,
        LocalDateTime endDate
    );
}