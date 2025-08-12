package com.whu.nanyin.controller;

import com.whu.nanyin.pojo.entity.UserHolding;
import com.whu.nanyin.pojo.entity.FundBasicInfo;
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

    @Autowired private UserProfileService userProfileService;
    @Autowired private AISuggestionService aiSuggestionService;
    @Autowired private UserHoldingService userHoldingService;
    @Autowired private FundInfoService fundInfoService;

    @PostMapping("/suggestion")
    @Operation(summary = "为【当前登录用户】生成投资建议")
    public ApiResponseVO<String> generateSuggestionForCurrentUser(Principal principal) {
        if (principal == null) {
            return ApiResponseVO.error("用户未登录，无法生成建议。");
        }

        Long currentUserId = Long.parseLong(principal.getName());

        try {
            // 1. 获取AI建议所需的核心数据
            ProfitLossVO profitLossVO = userProfileService.getProfitLossVOByUserId(currentUserId);
            if (profitLossVO == null) {
                return ApiResponseVO.error("无法获取用户的盈亏数据。");
            }
            List<UserHolding> holdings = userHoldingService.listByuserId(currentUserId);

            // 从FundInfoService获取FundBasicInfo列表
            List<FundBasicInfo> allFundBasicInfos = fundInfoService.listAllBasicInfos();
            Map<String, FundBasicInfo> fundInfoMap = allFundBasicInfos.stream()
                    .collect(Collectors.toMap(FundBasicInfo::getFundCode, Function.identity()));

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

    // 辅助方法
    private Map<String, BigDecimal> calculateAssetAllocation(List<UserHolding> holdings, Map<String, FundBasicInfo> fundInfoMap) {
        if (holdings == null || holdings.isEmpty()) {
            return Map.of();
        }
        return holdings.stream()
                .filter(h -> fundInfoMap.get(h.getFundCode()) != null && h.getMarketValue() != null && fundInfoMap.get(h.getFundCode()).getFundInvestType() != null)
                .collect(Collectors.groupingBy(
                    h -> fundInfoMap.get(h.getFundCode()).getFundInvestType(), // 注意：字段名可能需要根据您的FundBasicInfo实体类调整
                    Collectors.reducing(BigDecimal.ZERO, UserHolding::getMarketValue, BigDecimal::add))
                );
    }
}