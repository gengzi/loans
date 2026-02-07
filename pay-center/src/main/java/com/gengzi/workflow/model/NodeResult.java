package com.gengzi.workflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 节点执行结果
 * 存储单个节点的执行输出
 * 
 * @author gengzi
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NodeResult {

    /**
     * HTTP 响应状态码 (仅用于 HTTP 节点)
     */
    private int statusCode;

    /**
     * 响应体内容
     */
    private String body;

    /**
     * 响应头
     */
    private Map<String, String> headers;

    /**
     * 执行开始时间
     */
    private LocalDateTime startTime;

    /**
     * 执行结束时间
     */
    private LocalDateTime endTime;

    /**
     * 执行耗时 (毫秒)
     */
    private long durationMs;

    /**
     * 错误信息 (如果执行失败)
     */
    private String errorMessage;

    /**
     * 回调 ID (仅用于 CALLBACK 节点)
     */
    private String callbackId;
}
