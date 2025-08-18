package com.whu.nanyin.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 客户信息响应视图对象（View Object）。
 * 用于封装从后端安全地传递到前端页面的个人资料数据，
 * 只包含前端页面需要展示的字段。
 */
@Data
@Schema(description = "客户信息响应对象")
public class UserProfileVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "姓名")
    private String name;

    @Schema(description = "性别")
    private String gender;

    @Schema(description = "证件类型")
    private String idType;

    @Schema(description = "证件号码")
    private String idNumber;

    @Schema(description = "出生日期")
    private LocalDate birthDate;

    @Schema(description = "国籍")
    private String nationality;

    @Schema(description = "职业")
    private String occupation;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "联系地址")
    private String address;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "账户可用余额")
    private BigDecimal balance;

}