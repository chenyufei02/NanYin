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

    /**
     * 【最终版实现】获取基金超市的列表页数据。
     * 采用高效的“先过滤ID，再分页查询，最后内存拼接”的策略，
     * 确保了数据正确性（只显示有净值的基金）和高性能（避免慢查询）。
     */
    @Override
    public Page<FundBasicInfo> getFundBasicInfoPage(Page<FundBasicInfo> page, String fundCode, String fundName, String fundType) {

        // --- 步骤 1: 构建对 fund_basic_info 主表的查询条件 ---
        QueryWrapper<FundBasicInfo> queryWrapper = new QueryWrapper<>();

        // 约定前端会将搜索框的内容统一放在 fundName 这个参数里传过来
        if (StringUtils.hasText(fundName)) {
            // .and()确保这组 OR 条件是一个整体
            queryWrapper.and(wrapper -> wrapper
                .like("fund_name", fundName)       // 模糊匹配基金名称
                .or().like("abbreviation", fundName) // 或者，模糊匹配基金简称
                .or().like("fund_code", fundName)    // 或者，模糊匹配基金代码
            );
        }
        if (StringUtils.hasText(fundType)) {
            String fundTypeCode = translateFundType(fundType);
            if (fundTypeCode != null) {
                queryWrapper.eq("fund_invest_type", fundTypeCode);
            }
        }

        queryWrapper.orderByAsc("fund_code");

        // --- 步骤 2: 对 fund_basic_info 表执行快速的分页查询 ---
        fundBasicInfoMapper.selectPage(page, queryWrapper);

        // --- 步骤 3: 在内存中，为当前页的数据拼接上最新业绩 ---
        List<FundBasicInfo> basicInfoRecords = page.getRecords();
        if (basicInfoRecords.isEmpty()) {
            return page; // 如果当前页没有数据，直接返回
        }

        List<String> fundCodesOnPage = basicInfoRecords.stream()
                .map(FundBasicInfo::getFundCode)
                .collect(Collectors.toList());

        // 批量获取这10只基金的最新业绩，这个查询也非常快。
        Map<String, FundNetValuePerfRank> perfRankMap = fundNetValuePerfRankMapper.findLatestPerfRankByFundCodes(fundCodesOnPage)
                .stream()
                .collect(Collectors.toMap(FundNetValuePerfRank::getFundCode, perf -> perf));

        // 在Java内存中进行数据拼接
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
     * 获取基金详情页的聚合数据（此方法保持不变）。
     */
    @Override
    public FundDetailVO getFundDetail(String fundCode) {
        FundDetailVO detailVO = new FundDetailVO();
        FundBasicInfo basicInfo = fundBasicInfoMapper.selectOne(new QueryWrapper<FundBasicInfo>().eq("fund_code", fundCode));
        if (basicInfo == null) {
            return null;
        }
        detailVO.setBasicInfo(basicInfo);
        FundNetValuePerfRank latestPerf = fundNetValuePerfRankMapper.selectOne(
            new QueryWrapper<FundNetValuePerfRank>().eq("fund_code", fundCode).orderByDesc("end_date").last("LIMIT 1")
        );
        detailVO.setPerformance(latestPerf);
        List<FundNetValue> netValueHistory = fundNetValueMapper.selectList(
            new QueryWrapper<FundNetValue>().eq("fund_code", fundCode)
                .ge("end_date", LocalDate.now().minusYears(1))
                .orderByAsc("end_date")
        );
        detailVO.setNetValueHistory(netValueHistory);
        return detailVO;
    }

    /**
     * 获取所有基金的基础信息列表（此方法保持不变）。
     */
    @Override
    public List<FundBasicInfo> listAllBasicInfos() {
        return fundBasicInfoMapper.selectList(null);
    }

    /**
     * 【新增辅助方法】用于将前端的基金类型文本翻译为数据库代码。
     */
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
            case "指数型" -> "0"; // 指数型也属于股票型
            default -> null; // 如果是未知的类型，则不进行筛选
        };
    }
}