package com.whu.nanyin.controller;

import com.whu.nanyin.pojo.entity.UserHolding;
import com.whu.nanyin.pojo.vo.ApiResponseVO; // 用于统一API响应格式的VO
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

/**
 * @description 用户持仓管理的控制器，负责处理与用户持仓相关的HTTP请求
 */
@RestController
@RequestMapping("/api/holding") // 定义该控制器下所有接口的基础路径
@Tag(name = "个人持仓管理", description = "提供个人持仓的查询接口")
public class UserHoldingController {

    // 初始化日志记录器
    private static final Logger logger = LoggerFactory.getLogger(UserHoldingController.class);

    // 自动注入UserHoldingService，用于处理业务逻辑
    @Autowired
    private UserHoldingService userHoldingService;

    /**
     * @description 查询当前登录用户的所有持仓信息，支持按基金代码或名称进行筛选。
     * @param authentication Spring Security提供的认证信息对象，用于获取当前用户信息
     * @param fundCode       前端传入的查询参数：基金代码 (可选)
     * @param fundName       前端传入的查询参数：基金名称 (可选)
     * @return 返回一个包含持仓信息列表的ResponseEntity对象
     */
    @Operation(summary = "查询【当前登录用户】的所有持仓信息")
    @GetMapping("/my-holdings")
    public ResponseEntity<ApiResponseVO<List<UserHoldingVO>>> getMyHoldings(
            Authentication authentication,
            @RequestParam(required = false) String fundCode,
            @RequestParam(required = false) String fundName) {

        // 检查用户是否已登录
        if (authentication == null) {
            return ResponseEntity.status(401).body(ApiResponseVO.error("用户未登录"));
        }

        // 从认证信息中获取自定义的UserDetails对象，进而得到用户ID
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long currentUserId = userDetails.getId();

        // 声明一个用于存储数据库查询结果的列表
        List<UserHolding> holdingEntities;

        // 判断前端是否传入了筛选参数
        if ((fundCode != null && !fundCode.trim().isEmpty()) || (fundName != null && !fundName.trim().isEmpty())) {
            // 如果有筛选参数，则调用带条件查询的service方法
            holdingEntities = userHoldingService.listByUserIdAndFundInfo(currentUserId, fundCode, fundName);
        } else {
            // 如果没有筛选参数，则调用查询该用户所有持仓的service方法
            holdingEntities = userHoldingService.listByuserId(currentUserId);
        }

        // 使用Java Stream API将实体对象(Entity)列表转换为视图对象(VO)列表
        List<UserHoldingVO> holdingVOs = holdingEntities.stream().map(entity -> {
            UserHoldingVO vo = new UserHoldingVO();
            // 使用BeanUtils.copyProperties进行属性的快速复制
            BeanUtils.copyProperties(entity, vo);
            return vo;
        }).collect(Collectors.toList());

        // 在服务器日志中输出返回给前端的数据总数，用于调试和监控
        logger.info("返回给前台的持仓数据总数: {}", holdingVOs.size());

        // 遍历VO列表，在日志中详细输出每项持仓的关键信息，便于问题排查
        for (UserHoldingVO vo : holdingVOs) {
            logger.info("持仓项: 基金代码={}, 基金名称={}, 持有份额={}, 最新净值={}, 市值={}",
                    vo.getFundCode(), vo.getFundName(), vo.getTotalShares(),
                    vo.getLatestNetValue(), vo.getMarketValue());
        }

        // 使用ApiResponseVO对最终结果进行统一格式的包装，并返回成功的HTTP响应
        return ResponseEntity.ok(ApiResponseVO.success("持仓列表获取成功", holdingVOs));
    }
}