package com.whu.nanyin.controller;

import com.whu.nanyin.pojo.dto.RiskAssessmentSubmitDTO;
import com.whu.nanyin.pojo.entity.RiskAssessment;
import com.whu.nanyin.pojo.vo.ApiResponseVO;
import com.whu.nanyin.pojo.vo.RiskAssessmentVO;
import com.whu.nanyin.security.CustomUserDetails;
import com.whu.nanyin.service.RiskAssessmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/risk-assessment")
@Tag(name = "个人风险评估", description = "提供个人风险评估的提交与查询接口")
public class RiskAssessmentController {

    @Autowired
    private RiskAssessmentService riskAssessmentService;

    @Operation(summary = "提交一条新的风险评估记录")
    @PostMapping("/submit")
    public ResponseEntity<ApiResponseVO<RiskAssessmentVO>> submitAssessment(@RequestBody @Validated RiskAssessmentSubmitDTO dto, Authentication authentication) {
        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Long currentUserId = userDetails.getId();
            dto.setUserId(currentUserId);
            RiskAssessment entity = riskAssessmentService.createAssessment(dto);

            RiskAssessmentVO vo = new RiskAssessmentVO();
            BeanUtils.copyProperties(entity, vo);

            return ResponseEntity.ok(ApiResponseVO.success("风险评估提交成功", vo));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponseVO.error("提交失败: " + e.getMessage()));
        }
    }

    @Operation(summary = "查询【当前登录用户】的所有风险评估记录")
    @GetMapping("/my-assessments")
    public ResponseEntity<ApiResponseVO<List<RiskAssessmentVO>>> getMyAssessments(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long currentUserId = userDetails.getId();
        List<RiskAssessment> assessmentEntities = riskAssessmentService.listByUserId(currentUserId);

        List<RiskAssessmentVO> assessmentVOs = assessmentEntities.stream().map(entity -> {
            RiskAssessmentVO vo = new RiskAssessmentVO();
            BeanUtils.copyProperties(entity, vo);
            // 注意：这里未来可能需要关联查询客户姓名(customerName)等
            return vo;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponseVO.success("风险评估记录获取成功", assessmentVOs));
    }
}