package com.gengzi.workflow.service;

import com.gengzi.workflow.context.WorkflowContext;
import com.gengzi.workflow.dto.GraphRequest;
import com.gengzi.workflow.dto.WorkflowRunResponse;
import com.gengzi.workflow.dto.WorkflowStatusResponse;
import com.gengzi.workflow.engine.DagEngine;
import com.gengzi.workflow.model.Node;
import com.gengzi.workflow.model.NodeStatus;
import com.gengzi.workflow.model.WorkflowStatus;
import com.gengzi.workflow.storage.WorkflowStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工作流服务 (编排层)
 * 
 * 职责:
 * - 管理工作流生命周期
 * - 协调 DagEngine 和 Storage
 * - 对外提供高层 API
 * 
 * @author gengzi
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowService {

    private final DagEngine dagEngine;
    private final WorkflowStorageService storageService;

    /**
     * 启动工作流执行
     * 
     * @param graphRequest 工作流图定义
     * @return 执行响应
     */
    public WorkflowRunResponse runWorkflow(GraphRequest graphRequest) {
        // 1. 生成 runId
        String runId = UUID.randomUUID().toString();

        log.info("Starting new workflow: runId={}, nodes={}", runId, graphRequest.getNodes().size());

        // 2. 创建执行上下文
        WorkflowContext context = new WorkflowContext(runId);

        // 3. 保存到存储
        storageService.save(context);

        // 4. 异步执行工作流 (不阻塞 API 响应)
        try {
            dagEngine.executeWorkflow(graphRequest, context);

            return WorkflowRunResponse.builder()
                    .runId(runId)
                    .status(WorkflowStatus.RUNNING)
                    .message("Workflow started successfully")
                    .build();

        } catch (Exception e) {
            log.error("Failed to start workflow: runId={}", runId, e);

            context.setWorkflowStatus(WorkflowStatus.FAILED);

            return WorkflowRunResponse.builder()
                    .runId(runId)
                    .status(WorkflowStatus.FAILED)
                    .message("Failed to start workflow: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 获取工作流状态
     * 
     * @param runId 执行 ID
     * @return 状态响应
     */
    public WorkflowStatusResponse getStatus(String runId) {
        WorkflowContext context = storageService.get(runId);

        if (context == null) {
            return WorkflowStatusResponse.builder()
                    .runId(runId)
                    .workflowStatus(WorkflowStatus.FAILED)
                    .build();
        }

        return WorkflowStatusResponse.builder()
                .runId(runId)
                .workflowStatus(context.getWorkflowStatus())
                .nodeStatuses(new HashMap<>(context.getNodeStatus()))
                .results(new HashMap<>(context.getResults()))
                .totalDurationMs(context.getTotalDurationMs())
                .build();
    }

    /**
     * 处理异步回调
     * 
     * Feature C: Async Callbacks - 完整实现
     * 
     * @param callbackId 回调 ID
     * @param payload    回调数据
     */
    public void handleCallback(String callbackId, String payload) {
        log.info("Received callback: callbackId={}", callbackId);

        // 1. 使用 StorageService 查找对应的 context
        WorkflowContext context = storageService.findByCallbackId(callbackId);

        if (context == null) {
            log.warn("Callback ID not found: {}", callbackId);
            return;
        }

        // 2. 获取对应的节点 ID
        String nodeId = context.getCallbackMap().get(callbackId);

        if (nodeId == null) {
            log.warn("Node not found for callback ID: {}", callbackId);
            return;
        }

        log.info("Resuming node {} for callback {}", nodeId, callbackId);

        // 3. 从 context 中恢复 nodeMap 和 adjacencyList
        // 这些数据已经在 DagEngine.executeWorkflow 时存储到 context 中
        Map<String, Node> nodeMap = new HashMap<>(context.getNodeMap());
        Map<String, List<String>> adjacencyList = new HashMap<>();
        context.getAdjacencyList().forEach((key, value) -> adjacencyList.put(key, new ArrayList<>(value)));

        // 4. 调用 DagEngine.resumeNode() 恢复执行
        dagEngine.resumeNode(nodeId, payload, context, nodeMap, adjacencyList);

        log.info("Node {} resumed successfully", nodeId);
    }

    /**
     * 根据回调 ID 获取节点 ID
     * (用于 Controller)
     */
    public String getNodeByCallbackId(String callbackId) {
        WorkflowContext context = storageService.findByCallbackId(callbackId);
        if (context == null) {
            return null;
        }
        return context.getCallbackMap().get(callbackId);
    }
}
