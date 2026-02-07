package com.gengzi.workflow.executor;

import com.gengzi.workflow.model.*;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * HTTP 执行器
 * 负责执行 HTTP 请求，并处理循环策略
 * 
 * @author gengzi
 */
@Slf4j
@Component
public class HttpExecutor {

    private final OkHttpClient httpClient;

    public HttpExecutor() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 执行节点 (支持循环策略)
     * 
     * @param node 工作流节点
     * @return 执行结果
     */
    public NodeResult execute(Node node) {
        if (node.getType() != NodeType.HTTP) {
            throw new IllegalArgumentException("HttpExecutor can only execute HTTP type nodes");
        }

        LocalDateTime startTime = LocalDateTime.now();
        long startMs = System.currentTimeMillis();

        NodeResult result;

        // 根据循环策略执行
        switch (node.getLoopPolicy()) {
            case FIXED_COUNT:
                result = executeFixedCount(node);
                break;
            case UNTIL_SUCCESS:
                result = executeUntilSuccess(node);
                break;
            case NONE:
            default:
                result = executeSingle(node.getConfig());
                break;
        }

        LocalDateTime endTime = LocalDateTime.now();
        long endMs = System.currentTimeMillis();

        result.setStartTime(startTime);
        result.setEndTime(endTime);
        result.setDurationMs(endMs - startMs);

        return result;
    }

    /**
     * 执行单次 HTTP 请求
     */
    private NodeResult executeSingle(HttpConfig config) {
        try {
            Request request = buildRequest(config);

            log.info("Executing HTTP request: {} {}", config.getMethod(), config.getUrl());

            try (Response response = httpClient.newCall(request).execute()) {
                return buildResult(response);
            }
        } catch (IOException e) {
            log.error("HTTP request failed: {}", e.getMessage(), e);
            return NodeResult.builder()
                    .statusCode(0)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    /**
     * 固定次数循环执行
     * Feature A: In-Node Loops - FIXED_COUNT
     */
    private NodeResult executeFixedCount(Node node) {
        LoopConfig loopConfig = node.getLoopConfig();
        int maxRetries = loopConfig != null ? loopConfig.getMaxRetries() : 1;
        long intervalMs = loopConfig != null ? loopConfig.getIntervalMs() : 1000L;

        NodeResult lastResult = null;

        log.info("Executing FIXED_COUNT loop: node={}, retries={}", node.getId(), maxRetries);

        for (int i = 0; i < maxRetries; i++) {
            log.debug("Loop iteration {}/{} for node {}", i + 1, maxRetries, node.getId());

            lastResult = executeSingle(node.getConfig());

            // 如果不是最后一次循环，则等待间隔时间
            if (i < maxRetries - 1) {
                try {
                    Thread.sleep(intervalMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("Loop interrupted for node {}", node.getId());
                    break;
                }
            }
        }

        return lastResult;
    }

    /**
     * 轮询直到成功
     * Feature A: In-Node Loops - UNTIL_SUCCESS
     * 
     * 成功条件: HTTP 200 或自定义断言通过
     * MVP 版本: 简化为 HTTP 200 即视为成功
     */
    private NodeResult executeUntilSuccess(Node node) {
        LoopConfig loopConfig = node.getLoopConfig();
        long intervalMs = loopConfig != null ? loopConfig.getIntervalMs() : 1000L;
        long maxTimeoutMs = loopConfig != null ? loopConfig.getMaxTimeoutMs() : 60000L;

        long startMs = System.currentTimeMillis();
        NodeResult lastResult = null;
        int attemptCount = 0;

        log.info("Executing UNTIL_SUCCESS loop: node={}, maxTimeout={}ms", node.getId(), maxTimeoutMs);

        while (System.currentTimeMillis() - startMs < maxTimeoutMs) {
            attemptCount++;
            log.debug("Loop attempt {} for node {}", attemptCount, node.getId());

            lastResult = executeSingle(node.getConfig());

            // 成功条件: HTTP 200 (MVP 简化版本)
            if (isSuccess(lastResult, loopConfig)) {
                log.info("UNTIL_SUCCESS succeeded on attempt {} for node {}", attemptCount, node.getId());
                break;
            }

            // 等待间隔
            try {
                Thread.sleep(intervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Loop interrupted for node {}", node.getId());
                break;
            }
        }

        if (lastResult != null && !isSuccess(lastResult, loopConfig)) {
            log.warn("UNTIL_SUCCESS timeout after {}ms for node {}", maxTimeoutMs, node.getId());
        }

        return lastResult;
    }

    /**
     * 判断是否成功
     * MVP: HTTP 200 即视为成功
     * 未来扩展: 支持自定义表达式 (successCondition)
     */
    private boolean isSuccess(NodeResult result, LoopConfig loopConfig) {
        if (result == null || result.getStatusCode() == 0) {
            return false;
        }

        // MVP: 简化为 200 即成功
        if (result.getStatusCode() == 200) {
            return true;
        }

        // 未来: 支持自定义条件表达式
        // if (loopConfig != null && loopConfig.getSuccessCondition() != null) {
        // return evaluateCondition(result, loopConfig.getSuccessCondition());
        // }

        return false;
    }

    /**
     * 构建 OkHttp Request
     */
    private Request buildRequest(HttpConfig config) {
        Request.Builder builder = new Request.Builder()
                .url(config.getUrl());

        // 添加请求头
        if (config.getHeaders() != null) {
            config.getHeaders().forEach(builder::addHeader);
        }

        // 设置请求体
        RequestBody body = null;
        if (config.getBody() != null && !config.getBody().isEmpty()) {
            MediaType mediaType = MediaType.parse(config.getContentType());
            body = RequestBody.create(config.getBody(), mediaType);
        }

        // 设置 HTTP 方法
        switch (config.getMethod().toUpperCase()) {
            case "GET":
                builder.get();
                break;
            case "POST":
                builder.post(body != null ? body : RequestBody.create("", null));
                break;
            case "PUT":
                builder.put(body != null ? body : RequestBody.create("", null));
                break;
            case "DELETE":
                builder.delete(body);
                break;
            case "PATCH":
                builder.patch(body != null ? body : RequestBody.create("", null));
                break;
            default:
                throw new IllegalArgumentException("Unsupported HTTP method: " + config.getMethod());
        }

        return builder.build();
    }

    /**
     * 构建执行结果
     */
    private NodeResult buildResult(Response response) throws IOException {
        // 提取响应头
        Map<String, String> headers = new HashMap<>();
        response.headers().forEach(pair -> headers.put(pair.getFirst(), pair.getSecond()));

        // 读取响应体
        String bodyStr = null;
        if (response.body() != null) {
            bodyStr = response.body().string();
        }

        return NodeResult.builder()
                .statusCode(response.code())
                .body(bodyStr)
                .headers(headers)
                .build();
    }
}
