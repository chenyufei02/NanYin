package com.whu.nanyin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.whu.nanyin.pojo.entity.FundBasicInfo;
import com.whu.nanyin.pojo.vo.ApiResponseVO;
import com.whu.nanyin.pojo.vo.FundDetailVO;
import com.whu.nanyin.pojo.vo.FundNetValueTrendVO;
import com.whu.nanyin.service.FundInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 基金信息控制器
 * 提供基金信息的查询与搜索接口，包括基金列表、基金详情和净值走势数据
 */

@RestController
@RequestMapping("/api/fund-info")
@Tag(name = "基金信息管理", description = "提供基金信息的查询与搜索接口，包括基金列表、基金详情和净值走势数据")
public class FundInfoController {

    /**
     * 基金信息服务
     * 负责处理基金相关的业务逻辑，包括基金列表查询、基金详情获取和净值走势数据查询
     */
    @Autowired
    private FundInfoService fundInfoService;


    /**
     * 分页并按条件搜索基金信息列表
     * 
     * 该接口是基金超市页面的核心API，提供以下功能：
     * 1. 基金列表的分页展示，默认每页10条数据
     * 2. 基金搜索功能，支持按基金代码、名称进行模糊查询
     * 3. 基金分类筛选，支持按基金类型（股票型、债券型、混合型等）进行筛选
     * 
     * 返回的数据包括：
     * 1. 基金的静态基本信息（代码、名称、类型、成立日期等）
     * 2. 基金的动态业绩数据（最新净值、日涨跌幅）
     * 3. 分页信息（总记录数、总页数、当前页码等）
     * 
     * 前端基金超市页面通过此接口实现基金列表展示、搜索框功能和分类筛选功能
     * 
     * @param pageNum 当前页码，默认为1，由前端分页控件传入
     * @param pageSize 每页显示条数，默认为10，可由前端调整
     * @param fundCode 基金代码（可选，支持模糊查询）
     * @param fundName 基金名称或搜索关键词（可选，支持模糊查询）
     * @param fundType 基金类型（可选，精确匹配，如"股票型"、"债券型"等）
     * @return 包含分页基金信息的响应对象，使用统一的ApiResponseVO格式
     */
    @Operation(summary = "分页并按条件搜索基金信息列表")
    @GetMapping("/search")
    public ResponseEntity<ApiResponseVO<Page<FundBasicInfo>>> searchFunds(
            @RequestParam(value = "page", defaultValue = "1") int pageNum,
            @RequestParam(value = "size", defaultValue = "10") int pageSize,
            @RequestParam(required = false) String fundCode,
            @RequestParam(required = false) String fundName,
            @RequestParam(required = false) String fundType
    ) {
        // 创建分页对象，设置当前页码和每页显示条数
        Page<FundBasicInfo> page = new Page<>(pageNum, pageSize);
        
        // 调用服务层方法获取分页数据
        // 该方法会根据条件筛选基金，并自动填充page对象的records、total等属性
        // 同时会为每只基金补充最新净值和日涨跌幅数据
        fundInfoService.getFundBasicInfoPage(page, fundCode, fundName, fundType);
        
        // 返回成功响应，包含完整的分页数据
        // 前端可以通过page.records获取基金列表，通过page.total获取总记录数等
        return ResponseEntity.ok(ApiResponseVO.success("基金搜索成功", page));
    }

    /**
 * 获取基金详情
 * 
 * 该接口是基金详情页的核心接口，提供基金的完整详细信息，包括：
 * 1. 基金基本信息（代码、名称、类型等）
 * 2. 基金最新业绩表现与排名数据（最新净值、日增长率、各期收益率、同类排名等）
 * 3. 基金历史净值走势数据（过去一年的净值数据）
 * 
 * 前端基金详情页通过调用此接口获取所有需要展示的数据，包括基金概览、业绩走势图表等
 * 
 * @param fundCode 基金代码，路径变量，用于唯一标识要查询的基金
 * @return 包含完整基金详情数据的响应对象
 */
@Operation(summary = "根据基金代码查询基金详细信息")
@GetMapping("/detail/{fundCode}")
// 明确指定泛型为 FundDetailVO
public ResponseEntity<ApiResponseVO<FundDetailVO>> getFundDetail(@PathVariable String fundCode) {
    FundDetailVO fundDetail = fundInfoService.getFundDetail(fundCode);
    if (fundDetail != null) {
        return ResponseEntity.ok(ApiResponseVO.success("基金详情获取成功", fundDetail));
    } else {
        return ResponseEntity.status(404).body(ApiResponseVO.error("找不到对应的基金信息"));
    }
}

    /**
     * 获取指定时间范围内的基金净值走势数据
     * 
     * 该接口用于基金详情页的净值走势图表，支持查询一个或多个基金在指定时间范围内的净值数据
     * 返回的数据按基金代码分组，每组包含该基金在时间范围内的所有净值记录
     * 
     * 前端可以使用此数据绘制净值走势图表，进行基金业绩对比分析等功能
     * 
     * @param fundCodes 基金代码列表，支持同时查询多个基金的净值数据
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 包含基金净值走势数据的响应对象，数据按基金代码分组
     */
    @Operation(summary = "获取指定时间范围内的基金净值走势数据")
    @GetMapping("/net-value-trends")
    public ResponseEntity<ApiResponseVO<Map<String, List<FundNetValueTrendVO>>>> getFundNetValueTrends(
            @RequestParam String fundCodes,
            @RequestParam String startDate,
            @RequestParam String endDate
    ) {
        List<String> fundCodeList = List.of(fundCodes);
        LocalDateTime startDateTime = LocalDateTime.parse(startDate + "T00:00:00");
        LocalDateTime endDateTime = LocalDateTime.parse(endDate + "T23:59:59");
        Map<String, List<FundNetValueTrendVO>> trends = fundInfoService.getFundNetValueTrends(fundCodeList, startDateTime, endDateTime);
        return ResponseEntity.ok(ApiResponseVO.success("基金净值走势数据获取成功", trends));
    }


}