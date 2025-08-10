package com.whu.nanyin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.whu.nanyin.enums.RiskLevelEnum;
import com.whu.nanyin.mapper.RiskAssessmentMapper;
import com.whu.nanyin.pojo.dto.RiskAssessmentSubmitDTO;
import com.whu.nanyin.pojo.entity.RiskAssessment;
import com.whu.nanyin.service.RiskAssessmentService;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class RiskAssessmentServiceImpl extends ServiceImpl<RiskAssessmentMapper, RiskAssessment> implements RiskAssessmentService {



    @Override
    public RiskAssessment createAssessment(RiskAssessmentSubmitDTO dto) {
        // 2. 使用枚举类，根据分数计算出风险等级
        RiskLevelEnum riskLevelEnum = RiskLevelEnum.getByScore(dto.getScore());

        // 3. 创建一个完整的、即将存入数据库的实体对象
        RiskAssessment assessment = new RiskAssessment();
        assessment.setUserId(dto.getUserId());
        assessment.setAssessmentDate(dto.getAssessmentDate());
        assessment.setRiskScore(dto.getScore());
        // 4. 从枚举实例中获取规范的等级名称并设置
        assessment.setRiskLevel(riskLevelEnum.getLevelName());

        // 5. 将填充完毕的实体对象保存到数据库
        this.save(assessment);

        // 6. 返回保存好的实体（它现在已经包含了数据库生成的ID）
        return assessment;
    }

    public List<RiskAssessment> listByUserId(Long userId) {
        QueryWrapper<RiskAssessment> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        queryWrapper.orderByDesc("assessment_date"); // 按评估日期降序
        return this.list(queryWrapper);
    }

}