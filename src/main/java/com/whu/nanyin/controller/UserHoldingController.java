package com.whu.nanyin.controller;

import com.whu.nanyin.pojo.entity.UserHolding;
import com.whu.nanyin.pojo.vo.ApiResponseVO; // <-- 【新增】导入ApiResponseVO
import com.whu.nanyin.pojo.vo.UserHoldingVO;
import com.whu.nanyin.security.CustomUserDetails;
import com.whu.nanyin.service.UserHoldingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/holding")
@Tag(name = "个人持仓管理", description = "提供个人持仓的查询接口")
public class UserHoldingController {

    @Autowired
    private UserHoldingService userHoldingService;

    @Operation(summary = "查询【当前登录用户】的所有持仓信息")
    @GetMapping("/my-holdings")
    // 将返回类型用 ApiResponseVO 包装起来
    public ResponseEntity<ApiResponseVO<List<UserHoldingVO>>> getMyHoldings(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).body(ApiResponseVO.error("用户未登录"));
        }
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long currentUserId = userDetails.getId();

        List<UserHolding> holdingEntities = userHoldingService.listByuserId(currentUserId);

        // 将Entity列表转换为VO列表
        List<UserHoldingVO> holdingVOs = holdingEntities.stream().map(entity -> {
            UserHoldingVO vo = new UserHoldingVO();
            BeanUtils.copyProperties(entity, vo);
            // 注意：这里未来可能需要关联查询基金名称(fundName)等额外信息并设置到vo中
            return vo;
        }).collect(Collectors.toList());

        // 将VO列表作为data，包装在ApiResponseVO中返回
        return ResponseEntity.ok(ApiResponseVO.success("持仓列表获取成功", holdingVOs));
    }
}