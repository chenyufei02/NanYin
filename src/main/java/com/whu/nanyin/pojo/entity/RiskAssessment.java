package com.whu.nanyin.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("user_risk_assessments")
public class RiskAssessment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Integer riskScore;
    private String riskLevel;
    private LocalDate assessmentDate;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}