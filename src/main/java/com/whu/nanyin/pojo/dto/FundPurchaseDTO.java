package com.whu.nanyin.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "基金申购请求对象")
public class FundPurchaseDTO {


    @Schema(description = "用户ID", example = "1")
    private Long userId;

    @NotBlank(message = "基金代码不能为空")
    @Schema(description = "基金代码", example = "000001")
    private String fundCode;

    @NotNull(message = "申购金额不能为空")
    @Positive(message = "申购金额必须为正数")
    @Schema(description = "申购金额", example = "1000.00")
    private BigDecimal transactionAmount;

    @Schema(description = "交易申请时间（可不传，后端默认当前时间）", example = "2025-07-04T14:30:00")
    private LocalDateTime transactionTime;

    @NotBlank(message = "银行卡号不能为空")
    @Schema(description = "交易银行卡号", example = "6222020200023333")
    private String bankAccountNumber;
}