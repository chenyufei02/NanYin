package com.whu.nanyin.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 客户持仓视图对象（View Object）。
 * 用于封装从后端传递到前端页面的数据，结构与前端展示的需求保持一致。
 */
@Data
@Schema(description = "客户持仓视图对象")
public class UserHoldingVO {

    // --- 核心持仓信息 ---
    @Schema(description = "持仓记录ID")
    private Long id;

    @Schema(description = "总持有份额")
    private BigDecimal totalShares;

    @Schema(description = "持仓平均成本")
    private BigDecimal averageCost;

    @Schema(description = "当前市值")
    private BigDecimal marketValue;
    
    @Schema(description = "最新净值")
    private BigDecimal latestNetValue;

    @Schema(description = "持仓最后更新日期")
    private LocalDateTime lastUpdateDate;

    // --- 关联的客户信息 ---
    @Schema(description = "客户ID")
    private Long userId;

    @Schema(description = "客户姓名")
    private String userName;

    // --- 关联的基金信息 ---
    @Schema(description = "基金代码")
    private String fundCode;

    @Schema(description = "基金名称")
    private String fundName;
}