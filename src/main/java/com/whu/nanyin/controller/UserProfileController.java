package com.whu.nanyin.controller;

import com.whu.nanyin.mapper.UserMapper;
import com.whu.nanyin.pojo.dto.UserProfileUpdateDTO;
import com.whu.nanyin.pojo.entity.*;
import com.whu.nanyin.pojo.vo.ApiResponseVO;
import com.whu.nanyin.pojo.vo.UserDashboardVO;
import com.whu.nanyin.pojo.vo.UserProfileVO;
import com.whu.nanyin.security.CustomUserDetails;
import com.whu.nanyin.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


/**
 * 个人中心控制器，负责处理与个人资料查询、更新及主页数据聚合相关的API请求。
 */
@RestController
@RequestMapping("/api/user")
@Tag(name = "个人中心", description = "提供个人资料查询、更新及主页数据聚合的接口")
public class UserProfileController {

    @Autowired private UserProfileService userProfileService;
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


            return ResponseEntity.ok(ApiResponseVO.success("主页数据获取成功", dashboardVO));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(ApiResponseVO.error("获取主页数据时发生内部错误"));
        }
    }

}