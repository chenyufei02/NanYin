package com.whu.nanyin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.whu.nanyin.pojo.dto.UserProfileUpdateDTO;
import com.whu.nanyin.pojo.entity.FundInfo;
import com.whu.nanyin.pojo.entity.FundTransaction;
import com.whu.nanyin.pojo.entity.UserHolding;
import com.whu.nanyin.pojo.entity.UserProfile;
import com.whu.nanyin.pojo.vo.UserDashboardVO;
import com.whu.nanyin.pojo.vo.UserProfileVO; // <-- 【新增】导入UserProfileVO
import com.whu.nanyin.service.FundInfoService;
import com.whu.nanyin.service.FundTransactionService;
import com.whu.nanyin.service.UserHoldingService;
import com.whu.nanyin.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.BeanUtils; // <-- 【新增】导入BeanUtils
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user")
@Tag(name = "个人中心", description = "提供查询和更新个人资料的接口")
public class UserProfileController {

    @Autowired
    private UserProfileService userProfileService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserHoldingService userHoldingService;
    @Autowired
    private FundInfoService fundInfoService;
    @Autowired
    private FundTransactionService fundTransactionService;



    @GetMapping("/profile")
    @Operation(summary = "获取当前登录用户的个人资料")
    // 返回类型从 UserProfile转换为 UserProfileVO 防止直接将数据库实体类返回
    public ResponseEntity<UserProfileVO> getMyProfile(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        Long currentUserId = Long.parseLong(principal.getName());
        UserProfile userProfileEntity = userProfileService.getUserProfileByUserId(currentUserId);

        if (userProfileEntity == null) {
            return ResponseEntity.notFound().build();
        }

        // 将实体类(Entity)转换为视图对象(VO)
        UserProfileVO userProfileVO = new UserProfileVO();
        BeanUtils.copyProperties(userProfileEntity, userProfileVO);

        return ResponseEntity.ok(userProfileVO);
    }

    @PutMapping("/profile")
    @Operation(summary = "更新当前登录用户的个人资料")
    //  返回类型也统一为 UserProfileVO
    public ResponseEntity<UserProfileVO> updateUserProfile(@RequestBody @Validated UserProfileUpdateDTO dto, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        Long currentUserId = Long.parseLong(principal.getName());

        try {
            UserProfile updatedProfileEntity = userProfileService.updateUserProfile(currentUserId, dto);

            // 同样将更新后的实体类转换为VO再返回
            UserProfileVO updatedProfileVO = new UserProfileVO();
            BeanUtils.copyProperties(updatedProfileEntity, updatedProfileVO);

            return ResponseEntity.ok(updatedProfileVO);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // --- 【新增】获取个人主页所有数据的聚合接口 ---
    @GetMapping("/dashboard")
    @Operation(summary = "获取当前登录用户的主页仪表盘所有数据")
    public ResponseEntity<UserDashboardVO> getMyDashboard(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        Long currentUserId = Long.parseLong(principal.getName());

        try {
            UserDashboardVO dashboardVO = new UserDashboardVO();

            // 1. 获取个人资料
            dashboardVO.setUserProfile(userProfileService.getUserProfileByUserId(currentUserId));

            // 2. 获取盈亏统计
            dashboardVO.setProfitLossStats(userProfileService.getProfitLossVOByUserId(currentUserId));

            // 3. 获取Top 10持仓
            dashboardVO.setTopHoldings(userHoldingService.getTopNHoldings(currentUserId, 10));

            // 4. 【核心】复用 PageController 中的图表数据准备逻辑
            prepareChartData(currentUserId, dashboardVO);
            prepareHistoricalData(currentUserId, dashboardVO);

            return ResponseEntity.ok(dashboardVO);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    // --- 【移植】将 PageController 中的所有图表数据准备方法完整移植并改造 ---

    /**
     * 为“我的主页”准备所有图表所需的数据。
     * @param userId 当前登录用户的ID
     * @param vo 用于填充数据的视图对象
     */
    private void prepareChartData(Long userId, UserDashboardVO vo) throws Exception {
        // 1. 获取用户当前的所有持仓记录
        List<UserHolding> holdings = userHoldingService.listByuserId(userId);
        if (holdings == null || holdings.isEmpty()) {
            setEmptyChartData(vo);
            return;
        }

        // 2. 计算总资产市值
        BigDecimal totalMarketValue = holdings.stream()
                .map(UserHolding::getMarketValue)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalMarketValue.compareTo(BigDecimal.ZERO) <= 0) {
            setEmptyChartData(vo);
            return;
        }

        // 3. 准备关联数据
        List<String> fundCodes = holdings.stream().map(UserHolding::getFundCode).distinct().collect(Collectors.toList());
        Map<String, FundInfo> fundInfoMap = fundInfoService.listByIds(fundCodes).stream()
                .collect(Collectors.toMap(FundInfo::getFundCode, Function.identity()));

        // 4. 计算“资产类别分布图”数据
        Map<String, BigDecimal> assetAllocationData = holdings.stream()
                .filter(h -> fundInfoMap.get(h.getFundCode()) != null && h.getMarketValue() != null && fundInfoMap.get(h.getFundCode()).getFundType() != null)
                .collect(Collectors.groupingBy(
                    h -> fundInfoMap.get(h.getFundCode()).getFundType(),
                    Collectors.reducing(BigDecimal.ZERO, UserHolding::getMarketValue, BigDecimal::add)
                ));

        // 5. 计算“多维风险洞察雷达图”数据 (注意：风险诊断部分已简化)
        Map<String, BigDecimal> riskInsightData = calculateRiskInsightData(holdings, fundInfoMap, totalMarketValue);

        // 6. 为图表定义颜色映射
        Map<String, String> colorMap = new java.util.LinkedHashMap<>();
        colorMap.put("股票型", "#FF6384");
        colorMap.put("指数型", "#FF9F40");
        colorMap.put("混合型", "#FFCE56");
        colorMap.put("债券型", "#4BC0C0");
        colorMap.put("货币型", "#9966FF");
        // ... 其他颜色 ...

        // 7. 将计算结果转换为JSON字符串并设置到VO中
        vo.setAssetAllocationJson(objectMapper.writeValueAsString(assetAllocationData));
        vo.setRiskInsightJson(objectMapper.writeValueAsString(riskInsightData));
        vo.setColorMapJson(objectMapper.writeValueAsString(colorMap));
    }

    /**
     * 为“我的主页”准备历史走势图和资金流图的数据。
     * @param userId 当前登录用户的ID
     * @param vo 用于填充数据的视图对象
     */
    private void prepareHistoricalData(Long userId, UserDashboardVO vo) throws Exception {
        List<FundTransaction> transactions = fundTransactionService.listByUserId(userId);

        if (transactions == null || transactions.isEmpty()) {
            vo.setHistoricalDataJson("{}");
            vo.setMonthlyFlowJson("{}");
            return;
        }

        // 计算双曲线图数据
        Map<String, Map<String, BigDecimal>> historicalData = getStringMapMap(transactions);

        // 计算月度资金净流数据
        Map<String, BigDecimal> monthlyFlowData = transactions.stream()
            .collect(Collectors.groupingBy(
                tx -> tx.getTransactionTime().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM")),
                Collectors.mapping(
                    tx -> "申购".equals(tx.getTransactionType()) ? tx.getTransactionAmount() : tx.getTransactionAmount().negate(),
                    Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                )
            ));

        // 按月份排序
        Map<String, BigDecimal> sortedMonthlyFlow = new java.util.LinkedHashMap<>();
        monthlyFlowData.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEachOrdered(e -> sortedMonthlyFlow.put(e.getKey(), e.getValue()));

        // 将数据转换为JSON并设置到VO中
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
                totalMarketValue = totalMarketValue.add(
                    entry.getValue().multiply(tx.getSharePrice() != null ? tx.getSharePrice() : BigDecimal.ONE)
                );
            }

            Map<String, BigDecimal> dailyData = new java.util.HashMap<>();
            dailyData.put("assets", totalMarketValue.setScale(2, java.math.RoundingMode.HALF_UP));
            dailyData.put("investment", cumulativeInvestment.setScale(2, java.math.RoundingMode.HALF_UP));
            historicalData.put(date, dailyData);
        }
        return historicalData;
    }

    /**
     * 【辅助方法】计算风险洞察雷达图的数据
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

        // 简化：行为激进程度和流动性风险可以暂时用固定值或简化逻辑
        riskInsightData.put("行为激进程度", new BigDecimal(50)); // 占位符
        riskInsightData.put("流动性风险", hundred.subtract(lowRiskValue.divide(totalMarketValue, 4, java.math.RoundingMode.HALF_UP).multiply(hundred)));

        return riskInsightData;
    }

    /**
     * 【辅助方法】根据基金类型筛选并求和市值
     */
    private BigDecimal filterAndSum(List<UserHolding> holdings, Map<String, FundInfo> fundInfoMap, List<String> types) {
        return holdings.stream()
            .filter(h -> {
                FundInfo info = fundInfoMap.get(h.getFundCode());
                return info != null && info.getFundType() != null && h.getMarketValue() != null &&
                       types.stream().anyMatch(typeKeyword -> info.getFundType().contains(typeKeyword));
            })
            .map(UserHolding::getMarketValue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 【辅助方法】当没有图表数据时，设置空JSON
     */
    private void setEmptyChartData(UserDashboardVO vo) {
        vo.setAssetAllocationJson("{}");
        vo.setRiskInsightJson("{}");
        vo.setColorMapJson("{}");
    }
}

