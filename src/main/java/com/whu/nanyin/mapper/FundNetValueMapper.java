package com.whu.nanyin.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.whu.nanyin.pojo.entity.FundNetValue;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface FundNetValueMapper extends BaseMapper<FundNetValue> {
    /**
     * 获取指定时间范围内的基金净值数据
     * @param fundCodes 基金代码列表
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 基金净值列表
     */
    List<FundNetValue> findNetValueTrendByDateRange(
        @Param("fundCodes") List<String> fundCodes,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
}