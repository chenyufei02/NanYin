package com.whu.nanyin.enums;

import lombok.Getter;

/**
 * 风险等级枚举类
 * 用于统一定义和管理所有的风险等级及其相关的业务逻辑
 */
@Getter
public enum RiskLevelEnum {

    // 定义所有的风险等级实例，直接使用具体数值
    CONSERVATIVE("申报-保守型", 0, 20),
    STEADY("申报-稳健型", 21, 40),
    BALANCED("申报-平衡型", 41, 60),
    GROWTH("申报-成长型", 61, 80),
    AGGRESSIVE("申报-激进型", 81, 100);

    // --- 枚举的属性 ---
    private final String levelName; // 等级名称 (e.g., "保守型")
    private final int minScore;    // 包含的最低分
    private final int maxScore;    // 包含的最高分

    // --- 构造函数 ---
    RiskLevelEnum(String levelName, int minScore, int maxScore) {
        this.levelName = levelName;
        this.minScore = minScore;
        this.maxScore = maxScore;
    }

    // --- 公共的静态方法 (核心业务逻辑) ---
    /**
     * 根据分数，匹配并返回对应的风险等级枚举实例
     * @param score 风险评估得分
     * @return 对应的RiskLevelEnum实例
     */
    public static RiskLevelEnum getByScore(int score) {
        for (RiskLevelEnum level : values()) {
            if (score >= level.minScore && score <= level.maxScore) {
                return level;
            }
        }
        // 如果分数超出范围，可以根据业务需求返回一个默认值或抛出异常
        // 这里我们选择抛出异常，因为分数必须在0-100之间
        throw new IllegalArgumentException("无效的风险评估分数，分数必须在 0 到 100 之间，实际传入：" + score);
    }
}