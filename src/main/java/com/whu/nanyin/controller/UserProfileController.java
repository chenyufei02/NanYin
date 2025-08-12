package com.whu.nanyin.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whu.nanyin.pojo.dto.UserProfileUpdateDTO;
import com.whu.nanyin.pojo.entity.FundInfo;
import com.whu.nanyin.pojo.entity.FundTransaction;
import com.whu.nanyin.pojo.entity.UserHolding;
import com.whu.nanyin.pojo.entity.UserProfile;
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
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 个人中心控制器
 * 负责处理与当前登录用户个人信息相关的所有API请求，
 * 包括个人基本资料的查询与更新，以及个人主页（仪表盘）所需聚合数据的提供。
 */
@RestController
@RequestMapping("/api/user")
@Tag(name = "个人中心", description = "提供查询和更新个人资料的接口")
public class UserProfileController {


    @Autowired
    private UserProfileService userProfileService; // 注入用户个人资料服务
    @Autowired
    private UserHoldingService userHoldingService; // 注入用户持仓服务
    @Autowired
    private FundInfoService fundInfoService;       // 注入基金信息服务
    @Autowired
    private FundTransactionService fundTransactionService; // 注入用户交易服务
    @Autowired
    private ObjectMapper objectMapper; // 注入Jackson的核心组件，用于将Java对象序列化为JSON字符串

    /**
     * 获取当前登录用户的基本个人资料。
     * @param authentication Spring Security提供的对象，用于获取当前登录用户的信息。
     * @return 包含用户个人资料VO的ResponseEntity。
     */
    @GetMapping("/profile")
    @Operation(summary = "获取当前登录用户的个人资料")
    public ResponseEntity<UserProfileVO> getMyProfile(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).build(); // 用户未登录，返回401状态码
        }
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long currentUserId = userDetails.getId();

        UserProfile userProfileEntity = userProfileService.getUserProfileByUserId(currentUserId);

        if (userProfileEntity == null) {
            return ResponseEntity.notFound().build(); // 数据库中未找到该用户的资料，返回404
        }

        // 将数据库实体(Entity)转换为视图对象(VO)，避免直接暴露数据库结构
        UserProfileVO userProfileVO = new UserProfileVO();
        BeanUtils.copyProperties(userProfileEntity, userProfileVO);

        return ResponseEntity.ok(userProfileVO);
    }

    /**
     * 更新当前登录用户的个人资料。
     * @param dto 包含待更新字段的数据传输对象。
     * @param authentication Spring Security提供的对象，用于获取当前登录用户的信息。
     * @return 包含更新后用户个人资料VO的ResponseEntity。
     */
    @PutMapping("/profile")
    @Operation(summary = "更新当前登录用户的个人资料")
    public ResponseEntity<UserProfileVO> updateUserProfile(@RequestBody @Validated UserProfileUpdateDTO dto, Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).build(); // 用户未登录
        }
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long currentUserId = userDetails.getId();

        try {
            UserProfile updatedProfileEntity = userProfileService.updateUserProfile(currentUserId, dto);
            UserProfileVO updatedProfileVO = new UserProfileVO();
            BeanUtils.copyProperties(updatedProfileEntity, updatedProfileVO);
            return ResponseEntity.ok(updatedProfileVO);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build(); // 如果更新时找不到用户，返回404
        }
    }

    /**
     * 【聚合接口】获取构建“我的主页/仪表盘”所需的全部数据。
     * 这是一个高效的接口，前端只需调用一次即可获取渲染整个页面所需的所有信息。
     * @param authentication Spring Security提供的对象，用于获取当前登录用户的信息。
     * @return 包含所有主页数据的UserDashboardVO的ResponseEntity。
     */
    @GetMapping("/dashboard")
    @Operation(summary = "获取当前登录用户的主页仪表盘所有数据")
    public ResponseEntity<UserDashboardVO> getMyDashboard(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long currentUserId = userDetails.getId();

        try {
            UserDashboardVO dashboardVO = new UserDashboardVO();

            // 1. 聚合个人基本资料
            dashboardVO.setUserProfile(userProfileService.getUserProfileByUserId(currentUserId));
            // 2. 聚合个人盈亏统计
            dashboardVO.setProfitLossStats(userProfileService.getProfitLossVOByUserId(currentUserId));
            // 3. 聚合市值最高的10条持仓记录
            dashboardVO.setTopHoldings(userHoldingService.getTopNHoldings(currentUserId, 10));
            // 4. 聚合所有图表所需的数据
            prepareChartData(currentUserId, dashboardVO);
            prepareHistoricalData(currentUserId, dashboardVO);

            return ResponseEntity.ok(dashboardVO);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build(); // 服务器内部处理出错，返回500
        }
    }

    // --- 以下为私有辅助方法，负责复杂的数据聚合与计算 ---

    /**
     * 准备资产分布、风险洞察等图表所需的数据。
     * @param userId 当前登录用户的ID。
     * @param vo 用于填充数据的视图对象。
     * @throws JsonProcessingException 如果对象序列化为JSON失败。
     */
    private void prepareChartData(Long userId, UserDashboardVO vo) throws JsonProcessingException {
        List<UserHolding> holdings = userHoldingService.listByuserId(userId);
        if (holdings == null || holdings.isEmpty()) {
            setEmptyChartData(vo);
            return;
        }
        BigDecimal totalMarketValue = holdings.stream().map(UserHolding::getMarketValue).filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalMarketValue.compareTo(BigDecimal.ZERO) <= 0) {
            setEmptyChartData(vo);
            return;
        }
        List<String> fundCodes = holdings.stream().map(UserHolding::getFundCode).distinct().collect(Collectors.toList());
        Map<String, FundInfo> fundInfoMap = fundInfoService.listByIds(fundCodes).stream().collect(Collectors.toMap(FundInfo::getFundCode, Function.identity()));
        Map<String, BigDecimal> assetAllocationData = holdings.stream().filter(h -> fundInfoMap.get(h.getFundCode()) != null && h.getMarketValue() != null && fundInfoMap.get(h.getFundCode()).getFundType() != null).collect(Collectors.groupingBy(h -> fundInfoMap.get(h.getFundCode()).getFundType(), Collectors.reducing(BigDecimal.ZERO, UserHolding::getMarketValue, BigDecimal::add)));
        Map<String, BigDecimal> riskInsightData = calculateRiskInsightData(holdings, fundInfoMap, totalMarketValue);
        Map<String, String> colorMap = new java.util.LinkedHashMap<>();
        colorMap.put("股票型", "#FF6384");
        colorMap.put("指数型", "#FF9F40");
        colorMap.put("混合型", "#FFCE56");
        colorMap.put("债券型", "#4BC0C0");
        colorMap.put("货币型", "#9966FF");
        vo.setAssetAllocationJson(objectMapper.writeValueAsString(assetAllocationData));
        vo.setRiskInsightJson(objectMapper.writeValueAsString(riskInsightData));
        vo.setColorMapJson(objectMapper.writeValueAsString(colorMap));
    }

    /**
     * 准备历史资产走势与月度资金流图表所需的数据。
     * @param userId 当前登录用户的ID。
     * @param vo 用于填充数据的视图对象。
     * @throws JsonProcessingException 如果对象序列化为JSON失败。
     */
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

    /**
     * 计算历史资产走势图的核心数据。
     * @param transactions 用户的交易列表。
     * @return 按日期组织的资产与投入数据。
     */
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
            dailyData.put("assets", totalMarketValue.setScale(2, java.math.RoundingMode.HALF_UP));
            dailyData.put("investment", cumulativeInvestment.setScale(2, java.math.RoundingMode.HALF_UP));
            historicalData.put(date, dailyData);
        }
        return historicalData;
    }

    /**
     * 计算风险洞察雷达图的核心数据。
     * @param holdings 用户的持仓列表。
     * @param fundInfoMap 相关的基金信息。
     * @param totalMarketValue 用户的总市值。
     * @return 包含各项风险指标得分的Map。
     */
    private Map<String, BigDecimal> calculateRiskInsightData(List<UserHolding> holdings, Map<String, FundInfo> fundInfoMap, BigDecimal totalMarketValue) {
        BigDecimal highRiskValue = filterAndSum(holdings, fundInfoMap, List.of("股票型", "指数型"));
        BigDecimal midHighRiskValue = filterAndSum(holdings, fundInfoMap, List.of("混合型"));
        BigDecimal lowRiskValue = filterAndSum(holdings, fundInfoMap, List.of("货币型"));
        BigDecimal topHoldingValue = holdings.stream().map(UserHolding::getMarketValue).filter(java.util.Objects::nonNull).max(java.util.Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
        BigDecimal allInvestableValue = highRiskValue.add(midHighRiskValue).add(filterAndSum(holdings, fundInfoMap, List.of("债券型")));
        Map<String, BigDecimal> riskInsightData = new java.util.LinkedHashMap<>();
        BigDecimal hundred = new BigDecimal(100);
        riskInsightData.put("风险暴露度", highRiskValue.add(midHighRiskValue).divide(totalMarketValue, 4, java.math.RoundingMode.HALF_UP).multiply(hundred));
        riskInsightData.put("投资进攻性", allInvestableValue.compareTo(BigDecimal.ZERO) > 0 ? highRiskValue.divide(allInvestableValue, 4, java.math.RoundingMode.HALF_UP).multiply(hundred) : BigDecimal.ZERO);
        riskInsightData.put("持仓集中度", topHoldingValue.divide(totalMarketValue, 4, java.math.RoundingMode.HALF_UP).multiply(hundred));
        riskInsightData.put("行为激进程度", new BigDecimal(50));
        riskInsightData.put("流动性风险", hundred.subtract(lowRiskValue.divide(totalMarketValue, 4, java.math.RoundingMode.HALF_UP).multiply(hundred)));
        return riskInsightData;
    }

    /**
     * 根据指定的基金类型，筛选持仓并计算市值总和。
     * @param holdings 用户的持仓列表。
     * @param fundInfoMap 相关的基金信息。
     * @param types 需要筛选的基金类型关键字列表。
     * @return 符合条件的持仓市值总和。
     */
    private BigDecimal filterAndSum(List<UserHolding> holdings, Map<String, FundInfo> fundInfoMap, List<String> types) {
        return holdings.stream().filter(h -> {
            FundInfo info = fundInfoMap.get(h.getFundCode());
            return info != null && info.getFundType() != null && h.getMarketValue() != null && types.stream().anyMatch(typeKeyword -> info.getFundType().contains(typeKeyword));
        }).map(UserHolding::getMarketValue).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 当用户没有持仓时，为VO设置空的图表JSON数据。
     * @param vo 用于填充数据的视图对象。
     */
    private void setEmptyChartData(UserDashboardVO vo) {
        vo.setAssetAllocationJson("{}");
        vo.setRiskInsightJson("{}");
        vo.setColorMapJson("{}");
    }
}