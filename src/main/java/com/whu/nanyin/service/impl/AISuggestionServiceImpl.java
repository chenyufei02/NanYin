package com.whu.nanyin.service.impl;

import com.whu.nanyin.service.AISuggestionService;
import org.springframework.stereotype.Service;

/**
 * AI投资建议服务的空实现类。
 */
@Service
public class AISuggestionServiceImpl implements AISuggestionService {

    // 返回一个固定的提示信息，该功能正在开发中。

    @Override
    public String getAIEnhancedSuggestion(Long userId) {
        // 直接返回一个预设的字符串，不进行任何API调用或复杂计算
        return "AI投资建议功能正在开发中，敬请期待！";
    }
}