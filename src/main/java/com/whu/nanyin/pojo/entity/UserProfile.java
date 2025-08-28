package com.whu.nanyin.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalDate;

/**
 * 用户个人资料实体类，映射数据库中的 `user_profiles` 表。
 * 用于展示客户的详细基础信息。
 */
@Data // Lombok 注解，自动生成getter/setter/toString等方法
@TableName("user_profiles") // 指定对应的数据库表名
public class UserProfile {

    @TableId(type = IdType.AUTO)
    private Long id; // 主键ID，自增

    private Long userId; // 用户ID，关联到users表

    private String name; // 姓名

    private String gender; // 性别

    private String idType; // 证件类型

    private String idNumber; // 证件号码

    private LocalDate birthDate; // 出生日期

    private String nationality; // 国籍

    private String occupation; // 职业

    private String phone; // 手机号

    private String address; // 联系地址

    @Schema(hidden = true)
    // 配置为在插入记录时自动填充当前时间
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime; // 创建时间

    @Schema(hidden = true)
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime; // 更新时间
}