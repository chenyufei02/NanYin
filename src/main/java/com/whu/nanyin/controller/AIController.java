package com.whu.nanyin.controller;

import com.whu.nanyin.pojo.entity.UserHolding;
import com.whu.nanyin.pojo.entity.FundInfo;
import com.whu.nanyin.pojo.vo.ApiResponseVO;
import com.whu.nanyin.pojo.vo.ProfitLossVO;
import com.whu.nanyin.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ai")
@Tag(name = "AI助手", description = "提供基于大模型的智能分析与建议")
public class AIController {

    // 【修改】注入重命名后的 UserProfileService
    @Autowired private UserProfileService userProfileService;
    @Autowired private AISuggestionService aiSuggestionService;
    @Autowired private UserHoldingService userHoldingService;
    @Autowired private FundInfoService fundInfoService;

    // --- 【核心安全改造】 ---
    @PostMapping("/suggestion")
    @Operation(summary = "为【当前登录用户】生成投资建议")
    public ApiResponseVO<String> generateSuggestionForCurrentUser(Principal principal) {
        if (principal == null) {
            return ApiResponseVO.error("用户未登录，无法生成建议。");
        }

        Long currentUserId = Long.parseLong(principal.getName());

        try {
            // 1. 获取AI建议所需的核心数据
            // 【修改】调用正确命名的方法 getProfitLossVOByUserId
            ProfitLossVO profitLossVO = userProfileService.getProfitLossVOByUserId(currentUserId);
            if (profitLossVO == null) {
                return ApiResponseVO.error("无法获取用户的盈亏数据。");
            }

            List<UserHolding> holdings = userHoldingService.listByuserId(currentUserId);
            Map<String, FundInfo> fundInfoMap = fundInfoService.list().stream().collect(Collectors.toMap(FundInfo::getFundCode, Function.identity()));

            // 2. 计算图表数据
            Map<String, BigDecimal> assetAllocationData = calculateAssetAllocation(holdings, fundInfoMap);

            // 3. 调用AI服务
            String suggestion = aiSuggestionService.getMarketingSuggestion(profitLossVO, assetAllocationData);
            return ApiResponseVO.success("建议生成成功", suggestion);

        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponseVO.error("生成建议时发生后端错误：" + e.getMessage());
        }
    }

    // --- 辅助方法保持不变 ---
    private Map<String, BigDecimal> calculateAssetAllocation(List<UserHolding> holdings, Map<String, FundInfo> fundInfoMap) {
        if (holdings == null || holdings.isEmpty()) {
            return Map.of();
        }
        return holdings.stream()
                .filter(h -> fundInfoMap.get(h.getFundCode()) != null && h.getMarketValue() != null && fundInfoMap.get(h.getFundCode()).getFundType() != null)
                .collect(Collectors.groupingBy(h -> fundInfoMap.get(h.getFundCode()).getFundType(),
                        Collectors.reducing(BigDecimal.ZERO, UserHolding::getMarketValue, BigDecimal::add)));
    }
}