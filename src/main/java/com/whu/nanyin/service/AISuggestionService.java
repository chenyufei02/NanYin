package com.whu.nanyin.service;

public interface AISuggestionService {

    /**
     * 为指定的用户ID，整合其投资数据并生成AI投资建议。
     *
     * @param userId 当前登录用户的ID
     * @return AI生成的投资分析与建议字符串
     */
    String getAIEnhancedSuggestion(Long userId);
}