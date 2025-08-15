package com.whu.nanyin.controller;

import com.whu.nanyin.pojo.entity.UserHolding;
import com.whu.nanyin.pojo.vo.ApiResponseVO; // <-- 【新增】导入ApiResponseVO
import com.whu.nanyin.pojo.vo.UserHoldingVO;
import com.whu.nanyin.security.CustomUserDetails;
import com.whu.nanyin.service.UserHoldingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/holding")
@Tag(name = "个人持仓管理", description = "提供个人持仓的查询接口")
public class UserHoldingController {
    
    private static final Logger logger = LoggerFactory.getLogger(UserHoldingController.class);

    @Autowired
    private UserHoldingService userHoldingService;

    @Operation(summary = "查询【当前登录用户】的所有持仓信息")
    @GetMapping("/my-holdings")
    // 将返回类型用 ApiResponseVO 包装起来
    public ResponseEntity<ApiResponseVO<List<UserHoldingVO>>> getMyHoldings(
            Authentication authentication,
            @RequestParam(required = false) String fundCode,
            @RequestParam(required = false) String fundName) {
        if (authentication == null) {
            return ResponseEntity.status(401).body(ApiResponseVO.error("用户未登录"));
        }
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long currentUserId = userDetails.getId();

        List<UserHolding> holdingEntities;
        
        // 根据是否提供查询参数决定使用哪个查询方法
        if ((fundCode != null && !fundCode.trim().isEmpty()) || (fundName != null && !fundName.trim().isEmpty())) {
            holdingEntities = userHoldingService.listByUserIdAndFundInfo(currentUserId, fundCode, fundName);
        } else {
            holdingEntities = userHoldingService.listByuserId(currentUserId);
        }

        // 将Entity列表转换为VO列表
        List<UserHoldingVO> holdingVOs = holdingEntities.stream().map(entity -> {
            UserHoldingVO vo = new UserHoldingVO();
            BeanUtils.copyProperties(entity, vo);
            return vo;
        }).collect(Collectors.toList());
        
        // 在控制台输出返回给前台的值
        logger.info("返回给前台的持仓数据总数: {}", holdingVOs.size());
        
        // 输出每个持仓项的详细信息，特别是最新净值
        for (UserHoldingVO vo : holdingVOs) {
            logger.info("持仓项: 基金代码={}, 基金名称={}, 持有份额={}, 最新净值={}, 市值={}", 
                    vo.getFundCode(), vo.getFundName(), vo.getTotalShares(), 
                    vo.getLatestNetValue(), vo.getMarketValue());
        }
        
        // 将VO列表作为data，包装在ApiResponseVO中返回
        return ResponseEntity.ok(ApiResponseVO.success("持仓列表获取成功", holdingVOs));
    }
}