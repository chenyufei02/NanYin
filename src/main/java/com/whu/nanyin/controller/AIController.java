package com.whu.nanyin.controller;

import com.whu.nanyin.pojo.vo.ApiResponseVO;
import com.whu.nanyin.security.CustomUserDetails;
import com.whu.nanyin.service.AISuggestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@Tag(name = "AI助手", description = "提供基于大模型的智能分析与建议")
public class AIController {

    @Autowired
    private AISuggestionService aiSuggestionService;

    @PostMapping("/suggestion")
    @Operation(summary = "为【当前登录用户】生成投资建议")
    public ResponseEntity<ApiResponseVO<String>> generateSuggestionForCurrentUser(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).body(ApiResponseVO.error("用户未登录，无法生成建议。"));
        }

        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Long currentUserId = userDetails.getId();

            // 调用我们新的AI服务
            String suggestion = aiSuggestionService.getAIEnhancedSuggestion(currentUserId);

            return ResponseEntity.ok(ApiResponseVO.success("建议生成成功", suggestion));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(ApiResponseVO.error("生成建议时发生后端错误：" + e.getMessage()));
        }
    }
}