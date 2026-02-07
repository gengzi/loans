package com.gengzi.workflow.controller;

import com.gengzi.workflow.dto.GraphRequest;
import com.gengzi.workflow.dto.WorkflowRunResponse;
import com.gengzi.workflow.dto.WorkflowStatusResponse;
import com.gengzi.workflow.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 工作流 REST 控制器
 * 
 * API 端点:
 * - POST /api/workflow/run - 启动工作流
 * - GET /api/workflow/{runId}/status - 查询状态
 * - POST /api/hooks/{callbackId} - 异步回调
 * 
 * @author gengzi
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;

    /**
     * 启动工作流执行
     * 
     * POST /api/workflow/run
     * 
     * 请求体示例:
     * {
     * "nodes": [...],
     * "edges": [...]
     * }
     * 
     * @param graphRequest 工作流图定义
     * @return 执行响应 (包含 runId)
     */
    @PostMapping("/workflow/run")
    public ResponseEntity<WorkflowRunResponse> runWorkflow(@RequestBody GraphRequest graphRequest) {
        log.info("Received workflow run request: {} nodes, {} edges",
                graphRequest.getNodes().size(),
                graphRequest.getEdges().size());

        WorkflowRunResponse response = workflowService.runWorkflow(graphRequest);

        return ResponseEntity.ok(response);
    }

    /**
     * 查询工作流状态
     * 
     * GET /api/workflow/{runId}/status
     * 
     * @param runId 执行 ID
     * @return 状态响应
     */
    @GetMapping("/workflow/{runId}/status")
    public ResponseEntity<WorkflowStatusResponse> getStatus(@PathVariable String runId) {
        log.info("Received status query: runId={}", runId);

        WorkflowStatusResponse response = workflowService.getStatus(runId);

        return ResponseEntity.ok(response);
    }

    /**
     * 异步回调端点
     * 
     * POST /api/hooks/{callbackId}
     * 
     * Feature C: Async Callbacks
     * 当外部系统完成异步操作后，调用此接口唤醒 CALLBACK 节点
     * 
     * @param callbackId 回调 ID (由 CALLBACK 节点生成)
     * @param payload    回调数据 (任意 JSON)
     * @return 响应
     */
    @PostMapping("/hooks/{callbackId}")
    public ResponseEntity<String> handleCallback(
            @PathVariable String callbackId,
            @RequestBody String payload) {
        log.info("Received callback: callbackId={}", callbackId);

        workflowService.handleCallback(callbackId, payload);

        return ResponseEntity.ok("Callback received");
    }
}
