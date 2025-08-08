package com.whu.nanyin.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.whu.nanyin.pojo.vo.ProfitLossVO;
import com.whu.nanyin.service.AISuggestionService;
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

@Service
public class AISuggestionServiceImpl implements AISuggestionService {

    @Autowired
    private ObjectMapper objectMapper;

    // 请注意：出于安全考虑，不应将API密钥硬编码在代码中。
    // 在实际生产环境中，应将其存储在外部配置文件或环境变量中。
    private static final String BEARER_TOKEN = "sk-NCcKP3c4YSsOSt92q5u6Vp3vUFLifWCkZHK7cL9CkAdi9hgl";
    private static final String API_URL = "https://api.moonshot.cn/v1/chat/completions";

    /**
     * 为指定客户生成营销建议（已重构为个人投资者视角）
     * @param profitLossVO 客户的盈亏统计
     * @param assetAllocationData 资产类别分布图数据
     * @return AI生成的营销话术建议
     */
    @Override
    public String getMarketingSuggestion(ProfitLossVO profitLossVO, Map<String, BigDecimal> assetAllocationData) {
        // 调用新版的prompt构建方法
        String prompt = buildAdvancedPrompt(profitLossVO, assetAllocationData);
        try {
            return callMoonshotApi(prompt);
        } catch (Exception e) {
            e.printStackTrace();
            return "【AI建议生成失败】调用API时发生错误：" + e.getMessage();
        }
    }

    /**
     * 构建面向个人投资者的AI投资助手提示词（Prompt）
     * @param profitLossVO 用户的盈亏数据
     * @param assetAllocationData 用户的资产配置数据
     * @return 最终发送给大语言模型的提示词字符串
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

        sb.append("**2. 资产配置:**\n- **持仓分布**: ");
        if (assetAllocationData == null || assetAllocationData.isEmpty()) {
            sb.append("您当前暂无持仓。\n");
        } else {
            assetAllocationData.forEach((type, value) -> sb.append(String.format("%s (市值: %.2f), ", type, value)));
            sb.delete(sb.length() - 2, sb.length()).append("。\n");
        }
        sb.append("\n");

        // 3. 定义输出格式和要求
        sb.append("--- AI智能分析报告 ---\n\n");
        sb.append("请基于以上数据，生成以下两部分内容，语言要通俗易懂，多用鼓励性话语，避免使用过于专业的术语：\n");

        sb.append("**1. 您的投资组合速览:** (简短总结，不超过100字)\n   - 对您当前的业绩和资产状况进行一句话总结，给予肯定或鼓励。\n   - 简要分析您的持仓特点（例如：是偏向稳健的债券型基金，还是偏向高增长的股票型基金）。\n\n");

        sb.append("**2. AI智能分析与建议:** (分点阐述，每点不超过100字)\n   - **持仓分析与优化建议**: 根据您的持仓分布，判断是否存在过于集中或过于分散的问题，并提出具体的优化方向（例如：'建议适当增加一些稳健的债券型基金来平衡风险'或'您的配置非常多元化，能很好地抵御市场波动！'）。\n   - **后续操作建议**: 根据当前的盈亏状况，给出一个简单的后续操作建议。如果盈利，可以建议'继续持有，享受长期增长'或'考虑部分止盈'；如果亏损，可以建议'保持耐心，等待市场回暖'或'考虑定投摊低成本'。");

        System.out.println("======【个人投资者版AI请求】发送给大模型的Prompt是：======\n" + sb);
        return sb.toString();
    }

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
}