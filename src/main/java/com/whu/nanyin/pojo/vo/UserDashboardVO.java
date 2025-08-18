package com.whu.nanyin.pojo.vo;

import com.whu.nanyin.pojo.entity.UserProfile;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 用户主页（仪表盘）的视图对象（View Object）。
 * 这是一个聚合类，用于一次性封装所有主页上需要展示的数据模块。
 */
@Data
public class UserDashboardVO {
    private UserProfile userProfile; // 用户的个人基础资料
    private ProfitLossVO profitLossStats; // 用户的整体盈亏统计
    private List<UserHoldingVO> topHoldings; // 用户市值排名前N的持仓列表
    private String assetAllocationJson; // 资产分布图（如环形图）的数据 (JSON字符串)
    private String riskInsightJson; // 风险洞察图的数据 (JSON字符串)
    private String historicalDataJson; // 历史走势图的数据 (JSON字符串)
    private String monthlyFlowJson; // 月度资金流图的数据 (JSON字符串)
    private String colorMapJson; // 图表颜色映射 (JSON字符串)
    private BigDecimal balance;  // 用户的账户可用余额
}