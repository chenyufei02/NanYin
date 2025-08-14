package com.whu.nanyin.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.whu.nanyin.pojo.entity.FundBasicInfo;
import com.whu.nanyin.pojo.entity.UserHolding;
import com.whu.nanyin.pojo.vo.ProfitLossVO;
import com.whu.nanyin.service.AISuggestionService;
import com.whu.nanyin.service.FundInfoService;
import com.whu.nanyin.service.UserHoldingService;
import com.whu.nanyin.service.UserProfileService;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AISuggestionServiceImpl implements AISuggestionService {

    // 依赖注入
    @Autowired private UserProfileService userProfileService;
    @Autowired private UserHoldingService userHoldingService;
    @Autowired private FundInfoService fundInfoService;
    @Autowired private ObjectMapper objectMapper;

    // API密钥和地址 (生产环境中建议移至配置文件)
    private static final String BEARER_TOKEN = "sk-NCcKP3c4YSsOSt92q5u6Vp3vUFLifWCkZHK7cL9CkAdi9hgl";
    private static final String API_URL = "https://api.moonshot.cn/v1/chat/completions";

    @Override
    public String getAIEnhancedSuggestion(Long userId) {
        // 1. 获取AI建议所需的核心数据
        ProfitLossVO profitLossVO = userProfileService.getProfitLossVOByUserId(userId);
        if (profitLossVO == null) {
            return "抱歉，我们暂时无法获取您的盈亏数据来生成建议。";
        }
        List<UserHolding> holdings = userHoldingService.listByuserId(userId);

        // 2. 计算资产分布数据
        Map<String, BigDecimal> assetAllocationData = calculateAssetAllocation(holdings);

        // 3. 构建智能Prompt
        String prompt = buildAdvancedPrompt(profitLossVO, assetAllocationData);

        // 4. 调用大模型API并返回结果
        try {
            return callMoonshotApi(prompt);
        } catch (Exception e) {
            e.printStackTrace();
            return "【AI建议生成失败】调用API时发生错误：" + e.getMessage();
        }
    }

    /**
     * 辅助方法：计算资产分布
     */
    private Map<String, BigDecimal> calculateAssetAllocation(List<UserHolding> holdings) {
        if (holdings == null || holdings.isEmpty()) {
            return Collections.emptyMap();
        }
        List<String> fundCodes = holdings.stream().map(UserHolding::getFundCode).distinct().toList();
        Map<String, FundBasicInfo> fundInfoMap = fundInfoService.listAllBasicInfos().stream()
                .filter(info -> fundCodes.contains(info.getFundCode()))
                .collect(Collectors.toMap(FundBasicInfo::getFundCode, Function.identity()));

        return holdings.stream()
            .filter(h -> fundInfoMap.get(h.getFundCode()) != null && h.getMarketValue() != null)
            .collect(Collectors.groupingBy(
                h -> translateFundTypeCode(fundInfoMap.get(h.getFundCode()).getFundInvestType()),
                Collectors.reducing(BigDecimal.ZERO, UserHolding::getMarketValue, BigDecimal::add)
            ));
    }

    /**
     * 核心方法：构建面向个人投资者的AI投资助手Prompt
     */
    private String buildAdvancedPrompt(ProfitLossVO profitLossVO, Map<String, BigDecimal> assetAllocationData) {
        StringBuilder sb = new StringBuilder();

        // 1. 设置AI的角色和目标
        sb.append("你是一位专业的、风格亲切的AI投资助手。请根据以下用户的投资数据，为TA生成一份清晰易懂、充满鼓励的个性化投资分析报告。请直接面向用户，使用“您”作为称呼。\n\n");

        // 2. 呈现用户数据
        sb.append("--- 您的投资数据 ---\n\n");
        sb.append("**1. 业绩概览:**\n");
        sb.append(String.format("- **总资产**: %.2f 元\n- **累计投入**: %.2f 元\n- **当前总盈亏**: **%.2f 元 (回报率 %.2f%%)**\n\n",
                profitLossVO.getTotalMarketValue(),
                profitLossVO.getTotalInvestment(),
                profitLossVO.getTotalProfitLoss(),
                profitLossVO.getProfitLossRate()));

        sb.append("**2. 资产配置 (按基金类型):**\n");
        if (assetAllocationData == null || assetAllocationData.isEmpty()) {
            sb.append("- 您当前暂无持仓。\n");
        } else {
            assetAllocationData.forEach((type, value) -> sb.append(String.format("- **%s**: %.2f 元\n", type, value)));
        }
        sb.append("\n");

        // 3. 定义输出格式和要求
        sb.append("--- AI智能分析报告 ---\n\n");
        sb.append("请基于以上数据，生成以下两部分内容，语言要通俗易懂，多用鼓励性话语，避免使用过于专业的术语：\n\n");

        sb.append("**1. 您的投资组合速览:** (简短总结，不超过100字)\n");
        sb.append("   - 对您当前的业绩和资产状况进行一句话总结，给予肯定或鼓励。\n");
        sb.append("   - **【核心分析】根据您的持仓分布，反推出您的投资风格** (例如：'您的持仓主要集中在股票型基金，显示出您是一位追求高增长的积极型投资者。')。\n\n");

        sb.append("**2. AI智能分析与建议:** (分点阐述，每点不超过100字)\n");
        sb.append("   - **持仓分析与优化建议**: 根据您的持仓分布，判断是否存在过于集中或过于分散的问题，并提出具体的优化方向（例如：'建议适当增加一些稳健的债券型基金来平衡风险'或'您的配置非常多元化，能很好地抵御市场波动！'）。\n");
        sb.append("   - **后续操作建议**: 根据当前的盈亏状况，给出一个简单的后续操作建议。如果盈利，可以建议'继续持有，享受长期增长'或'考虑部分止盈'；如果亏损，可以建议'保持耐心，等待市场回暖'或'考虑定投摊低成本'。");

        return sb.toString();
    }

    /**
     * 辅助方法：调用Moonshot API
     */
    private String callMoonshotApi(String prompt) throws Exception {
        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);

        Map<String, Object> requestBodyMap = new HashMap<>();
        requestBodyMap.put("model", "moonshot-v1-8k");
        requestBodyMap.put("messages", Collections.singletonList(message));
        requestBodyMap.put("max_tokens", 4096);

        String requestBody = objectMapper.writeValueAsString(requestBodyMap);

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(API_URL);
            httpPost.setEntity(new StringEntity(requestBody, StandardCharsets.UTF_8));
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setHeader("Authorization", "Bearer " + BEARER_TOKEN);

            try (CloseableHttpResponse response = client.execute(httpPost)) {
                String responseBody = EntityUtils.toString(response.getEntity());
                if (response.getStatusLine().getStatusCode() != 200) {
                    throw new RuntimeException("API返回错误: " + response.getStatusLine().getStatusCode() + " " + responseBody);
                }
                Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, String> responseMessage = (Map<String, String>) choices.get(0).get("message");
                    return responseMessage.get("content");
                } else {
                    if (responseMap.containsKey("error")) {
                         Map<String, String> error = (Map<String, String>)responseMap.get("error");
                         throw new RuntimeException("API返回错误: " + error.get("message"));
                    }
                    throw new RuntimeException("API未返回有效的建议内容。");
                }
            }
        }
    }

    /**
     * 辅助方法：翻译基金类型代码
     */
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