package com.whu.nanyin.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonProperty;

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

    @Schema(description = "购买所用银行卡号（前端传入，后端用于资金来源标识）", example = "622202*********1234")
    @JsonProperty("bank_account_number")
    private String bankAccountNumber;
}