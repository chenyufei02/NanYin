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

@RestController
@RequestMapping("/api/fund-info")
@Tag(name = "基金信息管理", description = "提供基金信息的查询与搜索接口")
public class FundInfoController {

    @Autowired
    private FundInfoService fundInfoService;


    @Operation(summary = "分页并按条件搜索基金信息列表")
    @GetMapping("/search")
    // 明确指定泛型为 Page<FundBasicInfo>
    public ResponseEntity<ApiResponseVO<Page<FundBasicInfo>>> searchFunds(
            @RequestParam(value = "page", defaultValue = "1") int pageNum,
            @RequestParam(value = "size", defaultValue = "10") int pageSize,
            @RequestParam(required = false) String fundCode,
            @RequestParam(required = false) String fundName,
            @RequestParam(required = false) String fundType
    ) {
        Page<FundBasicInfo> page = new Page<>(pageNum, pageSize);
        fundInfoService.getFundBasicInfoPage(page, fundCode, fundName, fundType);
        return ResponseEntity.ok(ApiResponseVO.success("基金搜索成功", page));
    }

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