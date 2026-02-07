package com.gengzi.workflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * HTTP 请求配置
 * 封装 HTTP 调用的全部参数
 * 
 * @author gengzi
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HttpConfig {

    /**
     * 请求 URL
     */
    private String url;

    /**
     * HTTP 方法: GET, POST, PUT, DELETE 等
     */
    @Builder.Default
    private String method = "GET";

    /**
     * 请求头
     */
    @Builder.Default
    private Map<String, String> headers = new HashMap<>();

    /**
     * 请求体 (JSON 字符串或表单数据)
     */
    private String body;

    /**
     * Content-Type
     */
    @Builder.Default
    private String contentType = "application/json";

    /**
     * 连接超时 (毫秒)
     */
    @Builder.Default
    private long connectTimeoutMs = 10000L;

    /**
     * 读取超时 (毫秒)
     */
    @Builder.Default
    private long readTimeoutMs = 30000L;
}
