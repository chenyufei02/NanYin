package com.whu.nanyin.pojo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "基金赎回请求对象")
public class FundRedeemDTO {

    @NotBlank(message = "基金代码不能为空")
    @Schema(description = "基金代码", example = "000001")
    private String fundCode;

    @NotNull(message = "赎回份额不能为空")
    @Positive(message = "赎回份额必须为正数")
    @Schema(description = "赎回份额", example = "500.00")
    private BigDecimal transactionShares;

    @NotNull(message = "交易申请时间不能为空")
    @Schema(description = "交易申请时间", example = "2025-07-04T14:30:00")
    private LocalDateTime transactionTime;

    // 【【【 新增字段 】】】
    @NotBlank(message = "收款银行卡号不能为空")
    @Schema(description = "赎回资金收款银行卡号", example = "622202*********1234")
    @JsonProperty("bank_account_number") // 确保JSON字段名与前端一致
    private String bankAccountNumber;
}