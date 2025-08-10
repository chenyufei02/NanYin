package com.whu.nanyin.controller;

import com.whu.nanyin.pojo.entity.UserHolding;
import com.whu.nanyin.pojo.vo.UserHoldingVO;
import com.whu.nanyin.service.UserHoldingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
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
    public ResponseEntity<List<UserHoldingVO>> getMyHoldings(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        Long currentUserId = Long.parseLong(principal.getName());

        List<UserHolding> holdingEntities = userHoldingService.listByuserId(currentUserId);

        // 将Entity列表转换为VO列表
        List<UserHoldingVO> holdingVOs = holdingEntities.stream().map(entity -> {
            UserHoldingVO vo = new UserHoldingVO();
            BeanUtils.copyProperties(entity, vo);
            // 注意：这里未来可能需要关联查询基金名称(fundName)等额外信息并设置到vo中
            return vo;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(holdingVOs);
    }
}