package com.whu.nanyin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.whu.nanyin.enums.RiskLevelEnum;
import com.whu.nanyin.mapper.RiskAssessmentMapper;
import com.whu.nanyin.pojo.dto.RiskAssessmentSubmitDTO;
import com.whu.nanyin.pojo.entity.Customer;
import com.whu.nanyin.pojo.entity.RiskAssessment;
import com.whu.nanyin.pojo.vo.RiskAssessmentVO;
import com.whu.nanyin.service.CustomerService;
import com.whu.nanyin.service.RiskAssessmentService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RiskAssessmentServiceImpl extends ServiceImpl<RiskAssessmentMapper, RiskAssessment> implements RiskAssessmentService {

    @Autowired
    private CustomerService customerService;



    @Override
    public RiskAssessment createAssessment(RiskAssessmentSubmitDTO dto) {
        // 2. 使用枚举类，根据分数计算出风险等级
        RiskLevelEnum riskLevelEnum = RiskLevelEnum.getByScore(dto.getScore());

        // 3. 创建一个完整的、即将存入数据库的实体对象
        RiskAssessment assessment = new RiskAssessment();
        assessment.setCustomerId(dto.getCustomerId());
        assessment.setAssessmentDate(dto.getAssessmentDate());
        assessment.setRiskScore(dto.getScore());
        // 4. 从枚举实例中获取规范的等级名称并设置
        assessment.setRiskLevel(riskLevelEnum.getLevelName());

        // 5. 将填充完毕的实体对象保存到数据库
        this.save(assessment);

        // 6. 返回保存好的实体（它现在已经包含了数据库生成的ID）
        return assessment;
    }

    /**
     * 【最终完整版】同时支持多维度复杂查询与动态排序
     */
    @Override
    @Transactional(readOnly = true)
    public Page<RiskAssessmentVO> getAssessmentPage(
            Page<RiskAssessmentVO> page, String customerName, String riskLevel,
            String actualRiskLevel, String riskDiagnosis,
            String sortField, String sortOrder)
    {
        // 步骤 1: 基于所有筛选条件（客户姓名、实盘风险、风险诊断），预先筛选出符合条件的客户ID集合。
        Set<Long> customerIdsToFilter = null;

        // a. 如果有按“实盘风险”或“风险诊断”这两个标签的筛选条件
        if (StringUtils.hasText(actualRiskLevel) || StringUtils.hasText(riskDiagnosis)) {
            // 1. 将有效的筛选条件（标签名）收集到一个List中
            List<String> tagFilters = new ArrayList<>();
            if (StringUtils.hasText(actualRiskLevel)) {
                tagFilters.add(actualRiskLevel);
            }
            if (StringUtils.hasText(riskDiagnosis)) {
                tagFilters.add(riskDiagnosis);
            }


            if (customerIdsToFilter.isEmpty()) {
                page.setRecords(Collections.emptyList());
                page.setTotal(0);
                return page;
            }
        }

        // b. 如果有按客户姓名的筛选条件
        if (StringUtils.hasText(customerName)) {
            Set<Long> idsByName = customerService.lambdaQuery()
                    .like(Customer::getName, customerName)
                    .list().stream().map(Customer::getId).collect(Collectors.toSet());

            if (customerIdsToFilter == null) {
                customerIdsToFilter = idsByName;
            } else {
                customerIdsToFilter.retainAll(idsByName); // 取交集
            }

            if (customerIdsToFilter.isEmpty()) {
                page.setRecords(Collections.emptyList());
                page.setTotal(0);
                return page;
            }
        }

        // 步骤 2: 构建对 risk_assessment 表的主查询
        QueryWrapper<RiskAssessment> assessmentQueryWrapper = new QueryWrapper<>();

        // a. 应用上面预筛选出的客户ID集合作为查询条件
        if (customerIdsToFilter != null) {
            assessmentQueryWrapper.in("customer_id", customerIdsToFilter);
        }

        // b. 应用对“申报风险”的筛选（这是表内字段）
        if (StringUtils.hasText(riskLevel)) {
            assessmentQueryWrapper.eq("risk_level", riskLevel);
        }

        // c. 【 动态排序逻辑 】
        if (StringUtils.hasText(sortField)) {
            String dbColumn;
            switch (sortField) {
                case "customerId":
                    dbColumn = "customer_id";
                    break;
                case "riskScore":
                    dbColumn = "risk_score";
                    break;
                case "assessmentDate":
                    dbColumn = "assessment_date";
                    break;
                default:
                    dbColumn = "assessment_date";
                    sortOrder = "desc";
            }
            if ("asc".equalsIgnoreCase(sortOrder)) {
                assessmentQueryWrapper.orderByAsc(dbColumn);
            } else {
                assessmentQueryWrapper.orderByDesc(dbColumn);
            }
        } else {
            assessmentQueryWrapper.orderByDesc("assessment_date"); // 默认按评估日期降序
        }

        // 步骤 3: 执行分页查询，此时查询结果是已经排序好的
        Page<RiskAssessment> assessmentPage = new Page<>(page.getCurrent(), page.getSize());
        this.page(assessmentPage, assessmentQueryWrapper);

        List<RiskAssessment> assessmentRecords = assessmentPage.getRecords();
        if (assessmentRecords.isEmpty()) {
            return page.setRecords(Collections.emptyList());
        }



        page.setTotal(assessmentPage.getTotal());
        return page;
    }
}