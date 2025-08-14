package com.whu.nanyin.pojo.vo;

import com.whu.nanyin.pojo.entity.FundBasicInfo;
import com.whu.nanyin.pojo.entity.FundNetValue;
import com.whu.nanyin.pojo.entity.FundNetValuePerfRank;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 基金详情页视图对象
 * 用于聚合展示基金详情页所需的所有数据。
 */
@Data
@Schema(description = "基金详情页的聚合数据视图对象")
public class FundDetailVO {

    @Schema(description = "基金的静态基础信息（名称、代码、类型、费率等）")
    private FundBasicInfo basicInfo;

    @Schema(description = "基金最新的业绩表现与同类排名")
    private FundNetValuePerfRank performance;

    @Schema(description = "用于绘制历史净值走势图的数据列表")
    private List<FundNetValue> netValueHistory;

}