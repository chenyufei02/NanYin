package com.whu.nanyin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page; // 导入Page类
import com.whu.nanyin.pojo.entity.FundInfo;
import com.whu.nanyin.pojo.vo.ApiResponseVO;
import com.whu.nanyin.service.FundDataImportService;
import com.whu.nanyin.service.FundInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fund-info")
@Tag(name = "基金信息管理", description = "提供基金信息的查询与搜索接口")
public class FundInfoController {

    @Autowired
    private FundInfoService fundInfoService;

    @Autowired
    private FundDataImportService fundDataImportService;

    @Operation(summary = "分页并按条件搜索基金信息列表")
    @GetMapping("/search") // URL改为/search，更符合语义
    public ResponseEntity<ApiResponseVO<Page<FundInfo>>> searchFunds(
            @RequestParam(value = "page", defaultValue = "1") int pageNum,
            @RequestParam(value = "size", defaultValue = "10") int pageSize,
            @RequestParam(required = false) String fundCode,
            @RequestParam(required = false) String fundName,
            @RequestParam(required = false) String fundType,
            @RequestParam(required = false) Integer riskScore
    ) {
        Page<FundInfo> page = new Page<>(pageNum, pageSize);
        // 调用搜索服务
        fundInfoService.getFundInfoPage(page, fundCode, fundName, fundType, riskScore);
        return ResponseEntity.ok(ApiResponseVO.success("基金搜索成功", page));
    }

    @Operation(summary = "根据基金代码查询基金详细信息")
    @GetMapping("/{fundCode}")
    public ResponseEntity<ApiResponseVO<FundInfo>> getFundInfo(@PathVariable String fundCode) {
        FundInfo fundInfo = fundInfoService.getById(fundCode);
        if (fundInfo != null) {
            return ResponseEntity.ok(ApiResponseVO.success("基金信息获取成功", fundInfo));
        } else {
            return ResponseEntity.status(404).body(ApiResponseVO.error("找不到对应的基金信息"));
        }
    }

    @PostMapping("/import-all")
    @Operation(summary = "【手动触发】从外部数据源导入所有公募基金数据")
    public ResponseEntity<ApiResponseVO<String>> importAllFunds() {
        try {
            int count = fundDataImportService.importFundsFromDataSource();
            String message = "数据导入任务完成！共处理了 " + count + " 只基金。";
            return ResponseEntity.ok(new ApiResponseVO<>(true, message, message));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(new ApiResponseVO<>(false, "数据导入失败: " + e.getMessage()));
        }
    }
}