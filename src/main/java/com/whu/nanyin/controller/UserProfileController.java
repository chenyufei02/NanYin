package com.whu.nanyin.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whu.nanyin.mapper.UserMapper;
import com.whu.nanyin.pojo.dto.UserProfileUpdateDTO;
import com.whu.nanyin.pojo.entity.*;
import com.whu.nanyin.pojo.vo.ApiResponseVO;
import com.whu.nanyin.pojo.vo.UserDashboardVO;
import com.whu.nanyin.pojo.vo.UserProfileVO;
import com.whu.nanyin.security.CustomUserDetails;
import com.whu.nanyin.service.FundInfoService;
import com.whu.nanyin.service.UserHoldingService;
import com.whu.nanyin.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 个人中心控制器，负责处理与个人资料查询、更新及主页数据聚合相关的API请求。
 */
@RestController
@RequestMapping("/api/user")
@Tag(name = "个人中心", description = "提供个人资料查询、更新及主页数据聚合的接口")
public class UserProfileController {

    @Autowired private UserProfileService userProfileService;
    @Autowired private UserHoldingService userHoldingService;
    @Autowired private FundInfoService fundInfoService;
    @Autowired private ObjectMapper objectMapper; // Jackson库的核心类，用于JSON序列化和反序列化
    @Autowired private UserMapper userMapper;

    /**
     * 获取当前登录用户的个人资料。
     * @param authentication Spring Security提供的认证信息对象。
     * @return 包含UserProfileVO的统一API响应。
     */
    @GetMapping("/profile")
    @Operation(summary = "获取当前登录用户的个人资料")
    public ResponseEntity<ApiResponseVO<UserProfileVO>> getMyProfile(Authentication authentication) {
        // 从认证信息中获取当前登录用户的ID
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long currentUserId = userDetails.getId();

        // 调用Service层获取用户的个人资料实体
        UserProfile userProfileEntity = userProfileService.getUserProfileByUserId(currentUserId);
        // 从users表获取用户登录信息，主要是为了其中的余额字段
        User currentUser = userMapper.selectById(currentUserId);

        // 如果是新注册用户，可能还没有个人资料
        if (userProfileEntity == null) {
            return ResponseEntity.ok(ApiResponseVO.success("新用户，暂无个人资料", null));
        }

        // 创建视图对象(VO)，用于向前端返回数据
        UserProfileVO userProfileVO = new UserProfileVO();
        // 将实体(Entity)的属性复制到视图对象(VO)中
        BeanUtils.copyProperties(userProfileEntity, userProfileVO);

        // 如果用户信息存在，则将余额信息也设置到VO中
        if (currentUser != null) {
            userProfileVO.setBalance(currentUser.getBalance());
        }

        return ResponseEntity.ok(ApiResponseVO.success("个人资料获取成功", userProfileVO));
    }

    /**
     * 更新当前登录用户的个人资料。
     * @param dto 包含待更新字段的数据传输对象(DTO)。
     * @param authentication Spring Security认证信息。
     * @return 更新后的个人资料VO。
     */
    @PutMapping("/profile")
    @Operation(summary = "更新当前登录用户的个人资料")
    public ResponseEntity<ApiResponseVO<UserProfileVO>> updateUserProfile(@RequestBody @Validated UserProfileUpdateDTO dto, Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long currentUserId = userDetails.getId();
        try {
            // 调用Service层执行更新操作
            UserProfile updatedProfileEntity = userProfileService.updateUserProfile(currentUserId, dto);
            UserProfileVO updatedProfileVO = new UserProfileVO();
            // 将更新后的实体属性复制到VO
            BeanUtils.copyProperties(updatedProfileEntity, updatedProfileVO);
            return ResponseEntity.ok(ApiResponseVO.success("个人资料更新成功", updatedProfileVO));
        } catch (RuntimeException e) {
            // 如果Service层抛出异常（例如找不到用户），则返回404错误
            return ResponseEntity.status(404).body(ApiResponseVO.error(e.getMessage()));
        }
    }

    /**
     * 获取当前登录用户的主页（仪表盘）所需的全部聚合数据。
     * @param authentication Spring Security认证信息。
     * @return 包含所有主页数据的UserDashboardVO。
     */
    @GetMapping("/dashboard")
    @Operation(summary = "获取当前登录用户的主页仪表盘所有数据")
    public ResponseEntity<ApiResponseVO<UserDashboardVO>> getMyDashboard(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).body(ApiResponseVO.error("用户未登录"));
        }
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long currentUserId = userDetails.getId();

        try {
            // 创建主页的聚合VO对象
            UserDashboardVO dashboardVO = new UserDashboardVO();

            // 1. 获取并设置账户余额
            User currentUser = userMapper.selectById(currentUserId);
            if (currentUser != null) {
                dashboardVO.setBalance(currentUser.getBalance());
            }
            // 2. 获取并设置个人基础资料
            dashboardVO.setUserProfile(userProfileService.getUserProfileByUserId(currentUserId));
            // 3. 获取并设置盈亏统计数据
            dashboardVO.setProfitLossStats(userProfileService.getProfitLossVOByUserId(currentUserId));
            // 4. 调用私有辅助方法，准备图表所需的数据
            prepareChartData(currentUserId, dashboardVO);

            return ResponseEntity.ok(ApiResponseVO.success("主页数据获取成功", dashboardVO));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(ApiResponseVO.error("获取主页数据时发生内部错误"));
        }
    }


    // ===================私有辅助方法=========================
    /**
     * 准备前端图表（如环形图、雷达图）所需的数据，并序列化为JSON字符串。
     * @param userId      当前用户ID。
     * @param vo          主页聚合数据VO对象。
     * @throws JsonProcessingException 如果JSON序列化失败。
     */
    private void prepareChartData(Long userId, UserDashboardVO vo) throws JsonProcessingException {
        List<UserHolding> holdings = userHoldingService.listByuserId(userId);
        if (holdings == null || holdings.isEmpty()) {
            setEmptyChartData(vo);
            return;
        }


        List<String> fundCodes = holdings.stream()
            .map(h -> h.getFundCode()) // 强制trim
            .distinct()
            .toList();

        // 在构建用于快速查找的Map时，也对作为Key的fund_code进行trim()
        Map<String, FundBasicInfo> fundInfoMap = fundInfoService.listAllBasicInfos().stream()
                .filter(info -> fundCodes.contains(info.getFundCode()))
                .collect(Collectors.toMap(
                    info -> info.getFundCode(), // 使用trim后的结果作为Key
                    Function.identity()
                ));

        // 核心修正：根据持仓的市值和基金类型进行分组聚合，计算各类资产的总市值
        Map<String, BigDecimal> assetAllocationData = holdings.stream()
                .filter(h -> {
                    // 查找时也对fund_code进行trim()
                    FundBasicInfo info = fundInfoMap.get(h.getFundCode());
                    return info != null && h.getMarketValue() != null && h.getMarketValue().compareTo(BigDecimal.ZERO) > 0;
                })
                .collect(Collectors.groupingBy(
                    // 分组的Key是翻译后的基金类型名称
                    h -> translateFundTypeCode(fundInfoMap.get(h.getFundCode()).getFundInvestType()),
                    // 对同一类型下的所有持仓市值进行求和
                    Collectors.reducing(BigDecimal.ZERO, UserHolding::getMarketValue, BigDecimal::add)
                ));

        // 将聚合后的数据序列化为JSON字符串，存入VO
        vo.setAssetAllocationJson(objectMapper.writeValueAsString(assetAllocationData));
    }


    /**
     * 当用户没有持仓数据时，设置图表数据为空JSON对象。
     * @param vo 主页聚合数据VO对象。
     */
    private void setEmptyChartData(UserDashboardVO vo) {
        vo.setAssetAllocationJson("{}");
    }

    /**
     * 将数据库中的基金投资类型代码翻译为可读的中文名称。
     * @param typeCode 基金类型代码。
     * @return 对应的中文名称。
     */
    private String translateFundTypeCode(String typeCode) {
         if (typeCode == null) return "其他";
         return switch (typeCode) {
             case "0" -> "股票型";
             case "1" -> "债券型";
             case "2" -> "混合型";
             case "3" -> "货币型";
             case "6" -> "基金型";
             case "7" -> "保本型";
             case "8" -> "REITs";
             default -> "其他";
         };
    }
}