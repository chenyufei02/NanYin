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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.whu.nanyin.pojo.vo.FundNetValueTrendVO;

@Service
public class FundInfoServiceImpl implements FundInfoService {

    @Autowired
    private FundBasicInfoMapper fundBasicInfoMapper;
    @Autowired
    private FundNetValueMapper fundNetValueMapper;
    @Autowired
    private FundNetValuePerfRankMapper fundNetValuePerfRankMapper;

    /**
     * 获取基金超市的列表页数据
     * 
     * 该方法是基金超市页面的核心业务逻辑，负责：
     * 1. 根据用户输入的搜索条件（基金代码、名称、类型）筛选基金
     * 2. 对筛选结果进行分页处理
     * 3. 为每只基金补充最新的净值和涨跌幅数据
     * 
     * 采用高效的"先过滤ID，再分页查询，最后内存拼接"的策略，
     * 确保了数据正确性（只显示有净值的基金）和高性能（避免慢查询）。
     * 
     * @param page 分页参数对象，包含当前页码和每页显示条数
     * @param fundCode 基金代码（可选，支持模糊查询）
     * @param fundName 基金名称或搜索关键词（可选，支持模糊查询）
     * @param fundType 基金类型（可选，精确匹配）
     * @return 包含分页信息和基金列表的Page对象
     */
    @Override
    public Page<FundBasicInfo> getFundBasicInfoPage(Page<FundBasicInfo> page, String fundCode, String fundName, String fundType) {

        // --- 步骤 1: 构建对 fund_basic_info 主表的查询条件 ---
        QueryWrapper<FundBasicInfo> queryWrapper = new QueryWrapper<>();

        // 处理搜索框输入的关键词，支持同时匹配基金名称、简称和代码
        // 这是基金超市页面的主要搜索功能，用户可以输入任意关键词进行模糊查询
        if (StringUtils.hasText(fundName)) {
            queryWrapper.and(wrapper -> wrapper.like("fund_name", fundName)  // 匹配基金全称
                .or().like("abbreviation", fundName)                      // 匹配基金简称
                .or().like("fund_code", fundName));                      // 匹配基金代码
        }
        
        // 处理基金类型筛选条件
        // 这是基金超市页面的分类筛选功能，用户可以选择特定类型的基金
        if (StringUtils.hasText(fundType)) {
            // 将前端传来的中文类型名称转换为数据库中的代码
            String fundTypeCode = translateFundType(fundType);
            if (fundTypeCode != null) {
                queryWrapper.eq("fund_invest_type", fundTypeCode);
            }
        }
        
        // 默认按基金代码升序排列，保证列表顺序稳定
        queryWrapper.orderByAsc("fund_code");

        // --- 步骤 2: 对 fund_basic_info 表执行快速的分页查询 ---
        // 这一步使用MyBatis-Plus的分页插件进行高效分页
        // 只查询基本信息表，不包含净值和涨跌幅数据
        fundBasicInfoMapper.selectPage(page, queryWrapper);

        // --- 步骤 3: 在内存中，为当前页的数据拼接上最新业绩 ---
        // 获取当前页的基金列表
        List<FundBasicInfo> basicInfoRecords = page.getRecords();
        if (basicInfoRecords.isEmpty()) {
            return page; // 如果当前页没有数据，直接返回空列表
        }

        // 提取当前页所有基金的代码，用于批量查询最新业绩
        List<String> fundCodesOnPage = basicInfoRecords.stream()
                .map(FundBasicInfo::getFundCode)
                .collect(Collectors.toList());

        // 批量查询当前页所有基金的最新业绩数据（净值和涨跌幅）
        // 这里使用自定义SQL查询，一次性获取所有基金的最新业绩，避免N+1查询问题
        Map<String, FundNetValuePerfRank> perfRankMap = fundNetValuePerfRankMapper.findLatestPerfRankByFundCodes(fundCodesOnPage)
                .stream()
                .collect(Collectors.toMap(FundNetValuePerfRank::getFundCode, perf -> perf));

        // 在Java内存中将业绩数据关联到基金基本信息对象
        // 这是基金超市页面显示最新净值和涨跌幅的数据来源
        for (FundBasicInfo basicInfo : basicInfoRecords) {
            FundNetValuePerfRank performance = perfRankMap.get(basicInfo.getFundCode());
            if (performance != null) {
                // 设置最新净值，显示在基金超市列表中
                basicInfo.setLatestNetValue(performance.getUnitNetValue());
                // 设置日涨跌幅，显示在基金超市列表中，用不同颜色标识涨跌
                basicInfo.setDailyGrowthRate(performance.getDailyGrowthRate());
            }
        }

        // 返回完整的分页结果，包含基金基本信息和最新业绩数据
        return page;
    }



    /**
     * 获取基金详情
     * 
     * 该方法是基金详情页的核心业务逻辑，负责聚合基金的所有相关数据：
     * 1. 从fund_basic_info表获取基金的基本信息
     * 2. 从fund_net_value_perf_rank表获取最新的业绩表现与排名数据
     * 3. 从fund_net_value表获取过去一年的净值历史数据
     * 
     * 这些数据被组装到FundDetailVO对象中，用于在基金详情页展示
     * 
     * @param fundCode 基金代码
     * @return 包含完整基金详情数据的FundDetailVO对象
     */
    @Override
    public FundDetailVO getFundDetail(String fundCode) {
        // 创建返回对象
        FundDetailVO detailVO = new FundDetailVO();
        
        // 1. 获取基金基本信息
        FundBasicInfo basicInfo = fundBasicInfoMapper.selectOne(
            new QueryWrapper<FundBasicInfo>().eq("fund_code", fundCode)
        );
        // 如果基金不存在，返回null
        if (basicInfo == null) {
            return null;
        }
        // 设置基金基本信息
        detailVO.setBasicInfo(basicInfo);
        
        // 2. 获取基金最新的业绩表现与排名数据
        // 通过按日期倒序并限制为1条记录，获取最新的业绩数据
        FundNetValuePerfRank latestPerf = fundNetValuePerfRankMapper.selectOne(
            new QueryWrapper<FundNetValuePerfRank>()
                .eq("fund_code", fundCode)
                .orderByDesc("end_date")
                .last("LIMIT 1")
        );
        // 设置业绩表现数据
        detailVO.setPerformance(latestPerf);
        
        // 3. 获取过去一年的净值历史数据，用于绘制净值走势图
        List<FundNetValue> netValueHistory = fundNetValueMapper.selectList(
            new QueryWrapper<FundNetValue>()
                .eq("fund_code", fundCode)
                // 只查询过去一年的数据，减少数据量
                .ge("end_date", LocalDate.now().minusYears(1))
                // 按日期升序排列，便于前端直接绘制图表
                .orderByAsc("end_date")
        );
        // 设置净值历史数据
        detailVO.setNetValueHistory(netValueHistory);
        
        // 返回完整的基金详情数据
        return detailVO;
    }

    /**
     * 获取所有基金的基础信息列表。
     */
    @Override
    public List<FundBasicInfo> listAllBasicInfos() {
        return fundBasicInfoMapper.selectList(null);
    }

    @Override
    public Map<String, List<FundNetValueTrendVO>> getFundNetValueTrends(
        List<String> fundCodes,
        LocalDateTime startDate,
        LocalDateTime endDate
    ) {
        // 获取原始净值数据
        List<FundNetValue> netValues = fundNetValueMapper.findNetValueTrendByDateRange(fundCodes, startDate, endDate);
        
        // 转换为VO对象并按基金代码分组
        return netValues.stream()
            .map(nv -> {
                FundNetValueTrendVO vo = new FundNetValueTrendVO();
                vo.setFundCode(nv.getFundCode());
                vo.setDate(nv.getEndDate());
                vo.setUnitNetValue(nv.getUnitNetValue());
                vo.setAccumNetValue(nv.getAccumNetValue());
                return vo;
            })
            .collect(Collectors.groupingBy(
                FundNetValueTrendVO::getFundCode,
                Collectors.toList()
            ));
    }

    /**
     * 将前端的中文基金类型名称翻译为数据库中的代码
     * 
     * 该方法用于基金超市页面的类型筛选功能，将用户选择的中文基金类型
     * 转换为数据库中存储的代码值，用于构建SQL查询条件
     * 
     * 基金类型对照表：
     * - 股票型(0)：以股票为主要投资对象的基金，风险较高，收益潜力大
     * - 债券型(1)：以债券为主要投资对象的基金，风险较低，收益相对稳定
     * - 混合型(2)：同时投资股票和债券的基金，风险和收益适中
     * - 货币型(3)：投资于货币市场工具的基金，风险最低，流动性高
     * - 基金型(6)：FOF基金，投资于其他基金的基金
     * - 保本型(7)：承诺保障投资者本金安全的基金
     * - REITs(8)：房地产投资信托基金
     * - 指数型(0)：跟踪特定指数的基金，通常归类为股票型
     * 
     * @param fundType 前端传入的中文基金类型名称
     * @return 对应的数据库代码，如果类型未知则返回null
     */
    private String translateFundType(String fundType) {
        if (fundType == null) return null;
        return switch (fundType) {
            case "股票型" -> "0";  // 股票型基金，主要投资股票市场
            case "债券型" -> "1";  // 债券型基金，主要投资债券市场
            case "混合型" -> "2";  // 混合型基金，同时投资股票和债券
            case "货币型" -> "3";  // 货币市场基金，投资短期货币市场工具
            case "基金型" -> "6";  // FOF基金，投资于其他基金的基金
            case "保本型" -> "7";  // 保本型基金，承诺保障投资者本金安全
            case "REITs" -> "8";   // 房地产投资信托基金
            case "指数型" -> "0";  // 指数型基金，在数据库中也归类为股票型(0)
            default -> null;      // 如果是未知的类型，则不进行筛选
        };
    }
}