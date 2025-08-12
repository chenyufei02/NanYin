package com.whu.nanyin.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.whu.nanyin.pojo.entity.FundBasicInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FundBasicInfoMapper extends BaseMapper<FundBasicInfo> {

    Page<FundBasicInfo> searchFundList(
            Page<FundBasicInfo> page, @Param("fundCode") String fundCode,
            @Param("fundName") String fundName, @Param("fundType") String fundType);

}