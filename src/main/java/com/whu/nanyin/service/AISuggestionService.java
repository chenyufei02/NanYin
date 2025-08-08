// 文件: src/main/java/com/whu/nanyin/service/AISuggestionService.java
package com.whu.nanyin.service;

import com.whu.nanyin.pojo.vo.ProfitLossVO;
import java.math.BigDecimal;
import java.util.Map;

public interface AISuggestionService {

    /**
     * 为指定客户生成投资建议
     * @param profitLossVO 客户的盈亏统计
     * @param assetAllocationData 资产类别分布图数据
     * @return AI生成的投资分析建议
     */
    String getMarketingSuggestion(ProfitLossVO profitLossVO,
                                  Map<String, BigDecimal> assetAllocationData);
}