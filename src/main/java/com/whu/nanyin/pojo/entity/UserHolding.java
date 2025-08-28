package com.whu.nanyin.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户持仓实体类，映射数据库中的 `user_holdings` 表。
 */
@Data
@TableName("user_holdings") // 指定对应的数据库表名
public class UserHolding {

    @TableId(type = IdType.AUTO)
    private Long id; // 主键ID，自增

    private Long userId; // 用户ID，关联到users表

    private String fundCode; // 基金代码
    private BigDecimal totalShares; // 用户持有的该基金的总份额
    private BigDecimal marketValue; // 当前持仓的总市值
    private BigDecimal averageCost; // 持仓的平均成本价
    private LocalDateTime lastUpdateDate; // 持仓记录的最后更新时间
    private String fundName; // 基金名称

    // 该字段不与数据库表中的任何列直接对应，它是通过SQL连接查询得到的。
    @TableField(exist = false)
    private BigDecimal latestNetValue; // 基金的最新净值

    // 配置为在插入记录时自动填充当前时间
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}