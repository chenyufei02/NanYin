package com.whu.nanyin.pojo.vo;

import com.whu.nanyin.pojo.entity.UserProfile;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;


@Data
public class UserDashboardVO {
    private UserProfile userProfile; // 个人资料
    private ProfitLossVO profitLossStats; // 盈亏统计
    private List<UserHoldingVO> topHoldings; // Top N持仓
    private String assetAllocationJson; // 资产分布图数据 (JSON字符串)
    private String riskInsightJson; // 风险洞察图数据 (JSON字符串)
    private String historicalDataJson; // 历史走势图数据 (JSON字符串)
    private String monthlyFlowJson; // 月度资金流图数据 (JSON字符串)
    private String colorMapJson; // 图表颜色映射 (JSON字符串)
    private BigDecimal balance;  // 存放可用余额
}