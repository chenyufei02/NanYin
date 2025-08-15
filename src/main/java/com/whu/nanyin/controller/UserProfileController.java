// 文件路径: src/main/java/com/whu/nanyin/controller/UserProfileController.java
package com.whu.nanyin.controller;

import com.whu.nanyin.mapper.UserMapper;
import com.whu.nanyin.pojo.dto.UserProfileUpdateDTO;
import com.whu.nanyin.pojo.entity.*;
import com.whu.nanyin.pojo.vo.ApiResponseVO;
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


@RestController
@RequestMapping("/api/user")
@Tag(name = "个人中心", description = "提供个人资料查询、更新及主页数据聚合的接口")
public class UserProfileController {

    @Autowired private UserProfileService userProfileService;
    @Autowired private UserMapper userMapper;

    // ... (getMyProfile, updateUserProfile, getMyDashboard 方法保持不变) ...
    @GetMapping("/profile")
    @Operation(summary = "获取当前登录用户的个人资料")
    public ResponseEntity<ApiResponseVO<UserProfileVO>> getMyProfile(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long currentUserId = userDetails.getId();
        UserProfile userProfileEntity = userProfileService.getUserProfileByUserId(currentUserId);
        User currentUser = userMapper.selectById(currentUserId);

        if (userProfileEntity == null) {
            return ResponseEntity.ok(ApiResponseVO.success("新用户，暂无个人资料", null));
        }

        UserProfileVO userProfileVO = new UserProfileVO();
        BeanUtils.copyProperties(userProfileEntity, userProfileVO);

        if (currentUser != null) {
            userProfileVO.setBalance(currentUser.getBalance());
        }

        return ResponseEntity.ok(ApiResponseVO.success("个人资料获取成功", userProfileVO));
    }

    @PutMapping("/profile")
    @Operation(summary = "更新当前登录用户的个人资料")
    public ResponseEntity<ApiResponseVO<UserProfileVO>> updateUserProfile(@RequestBody @Validated UserProfileUpdateDTO dto, Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long currentUserId = userDetails.getId();
        try {
            UserProfile updatedProfileEntity = userProfileService.updateUserProfile(currentUserId, dto);
            UserProfileVO updatedProfileVO = new UserProfileVO();
            BeanUtils.copyProperties(updatedProfileEntity, updatedProfileVO);
            return ResponseEntity.ok(ApiResponseVO.success("个人资料更新成功", updatedProfileVO));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(ApiResponseVO.error(e.getMessage()));
        }
    }

}