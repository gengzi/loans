package com.gengzi.workflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 循环配置
 * 配合 LoopPolicy 使用，定义循环执行的具体参数
 * 
 * @author gengzi
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoopConfig {

    /**
     * 最大重试次数 (用于 FIXED_COUNT)
     * 例如: maxRetries=3 表示执行 3 次
     */
    @Builder.Default
    private int maxRetries = 1;

    /**
     * 循环间隔时间 (毫秒)
     * 每次循环执行之间的等待时间
     */
    @Builder.Default
    private long intervalMs = 1000L;

    /**
     * 成功条件表达式 (用于 UNTIL_SUCCESS)
     * 例如: "statusCode == 200" 或 "body.contains('success')"
     * MVP 版本: 简化为 HTTP 200 即视为成功
     */
    private String successCondition;

    /**
     * 最大超时时间 (毫秒)
     * UNTIL_SUCCESS 轮询的最大等待时间，防止无限循环
     */
    @Builder.Default
    private long maxTimeoutMs = 60000L; // 默认 60 秒
}
