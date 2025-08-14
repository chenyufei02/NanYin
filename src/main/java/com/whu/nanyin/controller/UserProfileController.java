// 文件路径: src/main/java/com/whu/nanyin/controller/UserProfileController.java
package com.whu.nanyin.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whu.nanyin.mapper.UserMapper;
import com.whu.nanyin.pojo.dto.UserProfileUpdateDTO;
import com.whu.nanyin.pojo.entity.*;
import com.whu.nanyin.pojo.vo.ApiResponseVO;
import com.whu.nanyin.pojo.vo.UserDashboardVO;
import com.whu.nanyin.pojo.vo.UserProfileVO;
import com.whu.nanyin.security.CustomUserDetails;
import com.whu.nanyin.service.FundInfoService;
import com.whu.nanyin.service.FundTransactionService;
import com.whu.nanyin.service.UserHoldingService;
import com.whu.nanyin.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user")
@Tag(name = "个人中心", description = "提供个人资料查询、更新及主页数据聚合的接口")
public class UserProfileController {

    @Autowired private UserProfileService userProfileService;
    @Autowired private UserHoldingService userHoldingService;
    @Autowired private FundInfoService fundInfoService;
    @Autowired private FundTransactionService fundTransactionService;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserMapper userMapper;

    // ... (getMyProfile, updateUserProfile, getMyDashboard 方法保持不变) ...
    @GetMapping("/profile")
    @Operation(summary = "获取当前登录用户的个人资料")
    public ResponseEntity<ApiResponseVO<UserProfileVO>> getMyProfile(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long currentUserId = userDetails.getId();
        UserProfile userProfileEntity = userProfileService.getUserProfileByUserId(currentUserId);
        User currentUser = userMapper.selectById(currentUserId);

        if (userProfileEntity == null) {
            return ResponseEntity.ok(ApiResponseVO.success("新用户，暂无个人资料", null));
        }

        UserProfileVO userProfileVO = new UserProfileVO();
        BeanUtils.copyProperties(userProfileEntity, userProfileVO);

        if (currentUser != null) {
            userProfileVO.setBalance(currentUser.getBalance());
        }

        return ResponseEntity.ok(ApiResponseVO.success("个人资料获取成功", userProfileVO));
    }

    @PutMapping("/profile")
    @Operation(summary = "更新当前登录用户的个人资料")
    public ResponseEntity<ApiResponseVO<UserProfileVO>> updateUserProfile(@RequestBody @Validated UserProfileUpdateDTO dto, Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long currentUserId = userDetails.getId();
        try {
            UserProfile updatedProfileEntity = userProfileService.updateUserProfile(currentUserId, dto);
            UserProfileVO updatedProfileVO = new UserProfileVO();
            BeanUtils.copyProperties(updatedProfileEntity, updatedProfileVO);
            return ResponseEntity.ok(ApiResponseVO.success("个人资料更新成功", updatedProfileVO));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(ApiResponseVO.error(e.getMessage()));
        }
    }

    @GetMapping("/dashboard")
    @Operation(summary = "获取当前登录用户的主页仪表盘所有数据")
    public ResponseEntity<ApiResponseVO<UserDashboardVO>> getMyDashboard(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).body(ApiResponseVO.error("用户未登录"));
        }
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long currentUserId = userDetails.getId();

        try {
            UserDashboardVO dashboardVO = new UserDashboardVO();
            User currentUser = userMapper.selectById(currentUserId);
            if (currentUser != null) {
                dashboardVO.setBalance(currentUser.getBalance());
            }
            dashboardVO.setUserProfile(userProfileService.getUserProfileByUserId(currentUserId));
            dashboardVO.setProfitLossStats(userProfileService.getProfitLossVOByUserId(currentUserId));
            dashboardVO.setTopHoldings(userHoldingService.getTopNHoldings(currentUserId, 10));

            prepareChartData(currentUserId, dashboardVO);
            prepareHistoricalData(currentUserId, dashboardVO);

            return ResponseEntity.ok(ApiResponseVO.success("主页数据获取成功", dashboardVO));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(ApiResponseVO.error("获取主页数据时发生内部错误"));
        }
    }

    // =================================================================
    // ==                  私有辅助方法 (最终修正版)                    ==
    // =================================================================

    private void prepareChartData(Long userId, UserDashboardVO vo) throws JsonProcessingException {
        List<UserHolding> holdings = userHoldingService.listByuserId(userId);
        if (holdings == null || holdings.isEmpty()) {
            setEmptyChartData(vo);
            return;
        }

        // --- 【【【 核心修正第一处：对持仓记录中的fund_code进行trim 】】】 ---
        List<String> fundCodes = holdings.stream()
            .map(h -> h.getFundCode().trim()) // <--- 强制trim
            .distinct()
            .toList();

        // --- 【【【 核心修正第二处：构建Map时，对作为Key的fund_code进行trim 】】】 ---
        Map<String, FundBasicInfo> fundInfoMap = fundInfoService.listAllBasicInfos().stream()
                .filter(info -> fundCodes.contains(info.getFundCode().trim())) // <--- 匹配时也trim
                .collect(Collectors.toMap(
                    info -> info.getFundCode().trim(), // <--- 用trim后的结果作为Key
                    Function.identity()
                ));

        // --- 【【【 核心修正第三处：查找Map时，对用于查找的fund_code进行trim 】】】 ---
        Map<String, BigDecimal> assetAllocationData = holdings.stream()
                .filter(h -> {
                    FundBasicInfo info = fundInfoMap.get(h.getFundCode().trim()); // <--- 查找时也trim
                    return info != null && h.getMarketValue() != null && h.getMarketValue().compareTo(BigDecimal.ZERO) > 0;
                })
                .collect(Collectors.groupingBy(
                    h -> translateFundTypeCode(fundInfoMap.get(h.getFundCode().trim()).getFundInvestType()), // <--- 查找时也trim
                    Collectors.reducing(BigDecimal.ZERO, UserHolding::getMarketValue, BigDecimal::add)
                ));

        vo.setAssetAllocationJson(objectMapper.writeValueAsString(assetAllocationData));
        // 注意：因为雷达图也依赖同样的数据，所以我们暂时只生成assetAllocationJson
    }

    // ... (其他所有私有辅助方法保持不变) ...
    private void prepareHistoricalData(Long userId, UserDashboardVO vo) throws JsonProcessingException {
        List<FundTransaction> transactions = fundTransactionService.listByUserId(userId);
        if (transactions == null || transactions.isEmpty()) {
            vo.setHistoricalDataJson("{}");
            vo.setMonthlyFlowJson("{}");
            return;
        }
        Map<String, Map<String, BigDecimal>> historicalData = getStringMapMap(transactions);
        Map<String, BigDecimal> monthlyFlowData = transactions.stream().collect(Collectors.groupingBy(tx -> tx.getTransactionTime().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM")), Collectors.mapping(tx -> "申购".equals(tx.getTransactionType()) ? tx.getTransactionAmount() : tx.getTransactionAmount().negate(), Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));
        Map<String, BigDecimal> sortedMonthlyFlow = new java.util.LinkedHashMap<>();
        monthlyFlowData.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEachOrdered(e -> sortedMonthlyFlow.put(e.getKey(), e.getValue()));
        vo.setHistoricalDataJson(objectMapper.writeValueAsString(historicalData));
        vo.setMonthlyFlowJson(objectMapper.writeValueAsString(sortedMonthlyFlow));
    }
    private static Map<String, Map<String, BigDecimal>> getStringMapMap(List<FundTransaction> transactions) {
         Map<String, Map<String, BigDecimal>> historicalData = new java.util.LinkedHashMap<>();
         BigDecimal cumulativeInvestment = BigDecimal.ZERO;
         Map<String, BigDecimal> currentShares = new java.util.HashMap<>();
         for (FundTransaction tx : transactions) {
             String date = tx.getTransactionTime().toLocalDate().toString();
             String fundCode = tx.getFundCode();
             if ("申购".equals(tx.getTransactionType())) {
                 cumulativeInvestment = cumulativeInvestment.add(tx.getTransactionAmount());
                 currentShares.put(fundCode, currentShares.getOrDefault(fundCode, BigDecimal.ZERO).add(tx.getTransactionShares()));
             } else {
                 cumulativeInvestment = cumulativeInvestment.subtract(tx.getTransactionAmount());
                 currentShares.put(fundCode, currentShares.getOrDefault(fundCode, BigDecimal.ZERO).subtract(tx.getTransactionShares()));
             }
             BigDecimal totalMarketValue = BigDecimal.ZERO;
             for (Map.Entry<String, BigDecimal> entry : currentShares.entrySet()) {
                 totalMarketValue = totalMarketValue.add(entry.getValue().multiply(tx.getSharePrice() != null ? tx.getSharePrice() : BigDecimal.ONE));
             }
             Map<String, BigDecimal> dailyData = new java.util.HashMap<>();
             dailyData.put("assets", totalMarketValue.setScale(2, RoundingMode.HALF_UP));
             dailyData.put("investment", cumulativeInvestment.setScale(2, RoundingMode.HALF_UP));
             historicalData.put(date, dailyData);
         }
         return historicalData;
    }
    private void setEmptyChartData(UserDashboardVO vo) {
        vo.setAssetAllocationJson("{}");
        vo.setRiskInsightJson("{}"); // 保持这个，即使现在没用到
    }
    private String translateFundTypeCode(String typeCode) {
         if (typeCode == null) return "其他";
         return switch (typeCode) {
             case "0" -> "股票型";
             case "1" -> "债券型";
             case "2" -> "混合型";
             case "3" -> "货币型";
             case "6" -> "基金型";
             case "7" -> "保本型";
             case "8" -> "REITs";
             default -> "其他";
         };
    }
}