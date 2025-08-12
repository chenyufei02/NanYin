package com.whu.nanyin.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("fund_net_value")
public class FundNetValue {
    @TableId
    private Long seq;
    private String fundCode;
    private LocalDateTime endDate;
    private BigDecimal unitNetValue;
    private BigDecimal accumNetValue;
}