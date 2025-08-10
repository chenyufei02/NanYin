package com.whu.nanyin.controller;

import com.whu.nanyin.pojo.entity.FundInfo;
import com.whu.nanyin.pojo.vo.ApiResponseVO;
import com.whu.nanyin.service.FundDataImportService;
import com.whu.nanyin.service.FundInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity; // 导入ResponseEntity
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/fund-info")
@Tag(name = "基金信息管理", description = "提供基金信息的增删改查接口")
public class FundInfoController {

    @Autowired
    private FundInfoService fundInfoService;

    @Autowired
    private FundDataImportService fundDataImportService;

    @Operation(summary = "根据基金代码查询基金信息")
    @GetMapping("/{fundCode}")
    public ResponseEntity<FundInfo> getFundInfo(@PathVariable String fundCode) {
        FundInfo fundInfo = fundInfoService.getById(fundCode);
        if (fundInfo != null) {
            return ResponseEntity.ok(fundInfo);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "查询所有基金信息列表")
    @GetMapping("/list")
    public List<FundInfo> listAllFundInfo() {
        return fundInfoService.list();
    }

    @PostMapping("/import-all")
    @Operation(summary = "【手动触发】从外部数据源导入所有公募基金数据")
    public ResponseEntity<ApiResponseVO<String>> importAllFunds() { // <-- 指定泛型
        try {
            int count = fundDataImportService.importFundsFromDataSource();
            return ResponseEntity.ok(new ApiResponseVO<>(true, "数据导入任务完成！共处理了 " + count + " 只基金。"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(new ApiResponseVO<>(false, "数据导入失败: " + e.getMessage()));
        }
    }
}