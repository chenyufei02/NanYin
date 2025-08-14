package com.whu.nanyin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.whu.nanyin.pojo.entity.FundNetValuePerfRank;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FundNetValuePerfRankMapper extends BaseMapper<FundNetValuePerfRank> {
    List<FundNetValuePerfRank> findLatestPerfRankByFundCodes(@Param("fundCodes") List<String> fundCodes);
}
