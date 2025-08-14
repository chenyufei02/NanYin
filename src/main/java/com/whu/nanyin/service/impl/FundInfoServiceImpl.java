package com.whu.nanyin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.whu.nanyin.mapper.FundBasicInfoMapper;
import com.whu.nanyin.mapper.FundNetValueMapper;
import com.whu.nanyin.mapper.FundNetValuePerfRankMapper;
import com.whu.nanyin.pojo.entity.FundBasicInfo;
import com.whu.nanyin.pojo.entity.FundNetValue;
import com.whu.nanyin.pojo.entity.FundNetValuePerfRank;
import com.whu.nanyin.pojo.vo.FundDetailVO;
import com.whu.nanyin.service.FundInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FundInfoServiceImpl implements FundInfoService {

    @Autowired
    private FundBasicInfoMapper fundBasicInfoMapper;
    @Autowired
    private FundNetValueMapper fundNetValueMapper;
    @Autowired
    private FundNetValuePerfRankMapper fundNetValuePerfRankMapper;


    @Override
    public Page<FundBasicInfo> getFundBasicInfoPage(Page<FundBasicInfo> page, String fundCode, String fundName, String fundType) {
        QueryWrapper<FundBasicInfo> queryWrapper = new QueryWrapper<>();
        if (StringUtils.hasText(fundName)) {
            queryWrapper.and(wrapper -> wrapper.like("fund_name", fundName)
                .or().like("abbreviation", fundName)
                .or().like("fund_code", fundName));
        }
        if (StringUtils.hasText(fundType)) {
            String fundTypeCode = translateFundType(fundType);
            if (fundTypeCode != null) {
                queryWrapper.eq("fund_invest_type", fundTypeCode);
            }
        }
        queryWrapper.orderByAsc("fund_code");

        fundBasicInfoMapper.selectPage(page, queryWrapper);

        List<FundBasicInfo> basicInfoRecords = page.getRecords();
        if (basicInfoRecords.isEmpty()) {
            return page;
        }
        List<String> fundCodesOnPage = basicInfoRecords.stream()
                .map(FundBasicInfo::getFundCode)
                .collect(Collectors.toList());
        Map<String, FundNetValuePerfRank> perfRankMap = fundNetValuePerfRankMapper.findLatestPerfRankByFundCodes(fundCodesOnPage)
                .stream()
                .collect(Collectors.toMap(FundNetValuePerfRank::getFundCode, perf -> perf));

        for (FundBasicInfo basicInfo : basicInfoRecords) {
            FundNetValuePerfRank performance = perfRankMap.get(basicInfo.getFundCode());
            if (performance != null) {
                basicInfo.setLatestNetValue(performance.getUnitNetValue());
                basicInfo.setDailyGrowthRate(performance.getDailyGrowthRate());
            }
        }
        return page;
    }


    /**
     * 【核心优化】获取基金详情页的聚合数据
     */
    @Override
    public FundDetailVO getFundDetail(String fundCode) {
        FundDetailVO detailVO = new FundDetailVO();

        // 1. 获取基金基础信息
        FundBasicInfo basicInfo = fundBasicInfoMapper.selectOne(new QueryWrapper<FundBasicInfo>().eq("fund_code", fundCode));
        if (basicInfo == null) {
            return null; // 如果基金不存在，直接返回null
        }
        detailVO.setBasicInfo(basicInfo);

        // 2. 获取最新的业绩表现
        FundNetValuePerfRank latestPerf = fundNetValuePerfRankMapper.selectOne(
            new QueryWrapper<FundNetValuePerfRank>().eq("fund_code", fundCode).orderByDesc("end_date").last("LIMIT 1")
        );
        detailVO.setPerformance(latestPerf);

        // --- 【【【 历史净值查询逻辑优化 - 最终版 】】】 ---

        // 3. 先查询最新的一条净值记录，以确定查询范围的结束日期
        FundNetValue latestNetValue = fundNetValueMapper.selectOne(
                new QueryWrapper<FundNetValue>().eq("fund_code", fundCode).orderByDesc("end_date").last("LIMIT 1")
        );

        List<FundNetValue> netValueHistory;
        if (latestNetValue != null) {
            // 如果存在净值数据，则以此为基准，计算一年前的开始日期
            LocalDate endDate = latestNetValue.getEndDate().toLocalDate();
            LocalDate startDate = endDate.minusYears(1);

            // 查询这个动态计算出的一年区间内的数据
            netValueHistory = fundNetValueMapper.selectList(
                new QueryWrapper<FundNetValue>()
                    .eq("fund_code", fundCode)
                    .between("end_date", startDate, endDate) // 使用.between()更精确
                    .orderByAsc("end_date")
            );
        } else {
            // 如果该基金没有任何净值数据，则返回一个空列表
            netValueHistory = Collections.emptyList();
        }

        detailVO.setNetValueHistory(netValueHistory);
        // --- 优化结束 ---

        return detailVO;
    }


    @Override
    public List<FundBasicInfo> listAllBasicInfos() {
        return fundBasicInfoMapper.selectList(null);
    }


    private String translateFundType(String fundType) {
        if (fundType == null) return null;
        return switch (fundType) {
            case "股票型" -> "0";
            case "债券型" -> "1";
            case "混合型" -> "2";
            case "货币型" -> "3";
            case "基金型" -> "6";
            case "保本型" -> "7";
            case "REITs" -> "8";
            case "指数型" -> "0";
            default -> null;
        };
    }
}