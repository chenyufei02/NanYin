package com.whu.nanyin.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "基金净值走势数据视图对象")
public class FundNetValueTrendVO {
    @Schema(description = "基金代码")
    private String fundCode;
    
    @Schema(description = "日期")
    private LocalDateTime date;
    
    @Schema(description = "单位净值")
    private BigDecimal unitNetValue;
    
    @Schema(description = "累计净值")
    private BigDecimal accumNetValue;
    
    @Schema(description = "复权净值")
    private BigDecimal adjustNetValue;
}