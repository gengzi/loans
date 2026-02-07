package com.gengzi.workflow.engine;

import com.gengzi.workflow.context.WorkflowContext;
import com.gengzi.workflow.dto.Edge;
import com.gengzi.workflow.dto.GraphRequest;
import com.gengzi.workflow.executor.HttpExecutor;
import com.gengzi.workflow.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * DAG 工作流执行引擎
 * 
 * 核心算法: BFS (广度优先搜索) + 入度表 (Indegree Map)
 * 
 * 调度流程:
 * 1. 初始化: 解析 GraphRequest，构建邻接表和入度表
 * 2. 启动: 将所有入度为 0 的节点提交到线程池
 * 3. 执行: 节点执行完成后，触发 onNodeComplete
 * 4. 依赖处理: 将下游节点入度 -1，入度变为 0 则提交执行
 * 5. 循环: 直到所有节点执行完成
 * 
 * 线程安全保证:
 * - 入度使用 AtomicInteger.decrementAndGet() 原子递减
 * - JOIN_ANY 使用 AtomicBoolean.compareAndSet() CAS 操作
 * - 所有状态存储在 WorkflowContext 的 ConcurrentHashMap 中
 * 
 * @author gengzi
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DagEngine {

    private final ExecutorService executorService;
    private final HttpExecutor httpExecutor;

    /**
     * 执行工作流
     * 
     * @param graphRequest 工作流图定义
     * @param context      执行上下文
     */
    public void executeWorkflow(GraphRequest graphRequest, WorkflowContext context) {
        log.info("Starting workflow execution: runId={}", context.getRunId());

        // 1. 解析图，构建数据结构
        Map<String, Node> nodeMap = buildNodeMap(graphRequest.getNodes());
        Map<String, List<String>> adjacencyList = buildAdjacencyList(graphRequest.getEdges());

        // 1.5 存储到 context 中 (用于 CALLBACK 节点恢复)
        context.getNodeMap().putAll(nodeMap);
        adjacencyList.forEach((key, value) -> context.getAdjacencyList().put(key, new ArrayList<>(value)));

        // 2. 计算入度并初始化
        initializeIndegrees(graphRequest.getNodes(), graphRequest.getEdges(), context);

        // 3. 初始化所有节点状态为 PENDING
        for (Node node : graphRequest.getNodes()) {
            context.getNodeStatus().put(node.getId(), NodeStatus.PENDING);
        }

        // 4. 找出所有入度为 0 的起始节点
        List<String> startNodes = findStartNodes(context);

        log.info("Found {} start nodes: {}", startNodes.size(), startNodes);

        // 5. 提交起始节点到线程池
        for (String nodeId : startNodes) {
            Node node = nodeMap.get(nodeId);
            submitNodeExecution(node, context, nodeMap, adjacencyList);
        }
    }

    /**
     * 构建节点映射表
     * Key: 节点 ID, Value: 节点对象
     */
    private Map<String, Node> buildNodeMap(List<Node> nodes) {
        Map<String, Node> map = new HashMap<>();
        for (Node node : nodes) {
            map.put(node.getId(), node);
        }
        return map;
    }

    /**
     * 构建邻接表 (Adjacency List)
     * Key: 节点 ID, Value: 下游节点 ID 列表
     */
    private Map<String, List<String>> buildAdjacencyList(List<Edge> edges) {
        Map<String, List<String>> adjacencyList = new HashMap<>();

        for (Edge edge : edges) {
            adjacencyList
                    .computeIfAbsent(edge.getFromNodeId(), k -> new ArrayList<>())
                    .add(edge.getToNodeId());
        }

        return adjacencyList;
    }

    /**
     * 初始化入度表
     * 遍历所有边，统计每个节点的入度数量
     */
    private void initializeIndegrees(List<Node> nodes, List<Edge> edges, WorkflowContext context) {
        // 初始化所有节点入度为 0
        for (Node node : nodes) {
            context.getIndegreeMap().put(node.getId(), new AtomicInteger(0));
        }

        // 统计入度
        for (Edge edge : edges) {
            AtomicInteger indegree = context.getIndegreeMap().get(edge.getToNodeId());
            if (indegree != null) {
                indegree.incrementAndGet();
            }
        }

        // 打印入度信息 (用于调试)
        log.debug("Indegree map initialization:");
        context.getIndegreeMap()
                .forEach((nodeId, indegree) -> log.debug("  Node {}: indegree = {}", nodeId, indegree.get()));
    }

    /**
     * 找出所有入度为 0 的起始节点
     */
    private List<String> findStartNodes(WorkflowContext context) {
        List<String> startNodes = new ArrayList<>();

        context.getIndegreeMap().forEach((nodeId, indegree) -> {
            if (indegree.get() == 0) {
                startNodes.add(nodeId);
            }
        });

        return startNodes;
    }

    /**
     * 提交节点到线程池执行
     * 
     * @param node          节点
     * @param context       上下文
     * @param nodeMap       节点映射表
     * @param adjacencyList 邻接表
     */
    private void submitNodeExecution(
            Node node,
            WorkflowContext context,
            Map<String, Node> nodeMap,
            Map<String, List<String>> adjacencyList) {
        log.info("Submitting node {} for execution", node.getId());

        CompletableFuture.runAsync(() -> {
            executeNode(node, context, nodeMap, adjacencyList);
        }, executorService).exceptionally(ex -> {
            log.error("Node {} execution failed with exception", node.getId(), ex);
            context.getNodeStatus().put(node.getId(), NodeStatus.FAILED);

            NodeResult errorResult = NodeResult.builder()
                    .errorMessage(ex.getMessage())
                    .build();
            context.getResults().put(node.getId(), errorResult);

            return null;
        });
    }

    /**
     * 执行单个节点
     * 
     * 核心逻辑:
     * 1. 更新状态为 RUNNING
     * 2. 根据节点类型执行 (HTTP 或 CALLBACK)
     * 3. 存储结果
     * 4. 更新状态为 SUCCESS/FAILED
     * 5. 触发下游节点调度 (onNodeComplete)
     */
    private void executeNode(
            Node node,
            WorkflowContext context,
            Map<String, Node> nodeMap,
            Map<String, List<String>> adjacencyList) {
        String nodeId = node.getId();
        log.info("Executing node: {}, type: {}", nodeId, node.getType());

        // 1. 更新状态为 RUNNING
        context.getNodeStatus().put(nodeId, NodeStatus.RUNNING);

        try {
            NodeResult result;

            // 2. 根据节点类型执行
            if (node.getType() == NodeType.HTTP) {
                // HTTP 节点: 调用 HttpExecutor (支持循环策略)
                result = httpExecutor.execute(node);

                // 3. 存储结果
                context.getResults().put(nodeId, result);

                // 4. 更新状态
                if (result.getStatusCode() == 200 || result.getStatusCode() > 0) {
                    context.getNodeStatus().put(nodeId, NodeStatus.SUCCESS);
                    log.info("Node {} completed successfully", nodeId);
                } else {
                    context.getNodeStatus().put(nodeId, NodeStatus.FAILED);
                    log.warn("Node {} failed: {}", nodeId, result.getErrorMessage());
                }

                // 5. 触发下游节点
                onNodeComplete(nodeId, context, nodeMap, adjacencyList);

            } else if (node.getType() == NodeType.CALLBACK) {
                // Feature C: Async Callbacks
                // CALLBACK 节点: 生成 callbackId，挂起执行
                String callbackId = UUID.randomUUID().toString();

                result = NodeResult.builder()
                        .callbackId(callbackId)
                        .build();

                context.getResults().put(nodeId, result);
                context.getNodeStatus().put(nodeId, NodeStatus.WAITING_CALLBACK);
                context.getCallbackMap().put(callbackId, nodeId);

                log.info("Node {} is waiting for callback: callbackId={}", nodeId, callbackId);

                // 注意: CALLBACK 节点不触发 onNodeComplete
                // 下游节点需要等待外部调用 resumeNode 才会继续
            }

        } catch (Exception e) {
            log.error("Node {} execution exception", nodeId, e);

            NodeResult errorResult = NodeResult.builder()
                    .errorMessage(e.getMessage())
                    .build();
            context.getResults().put(nodeId, errorResult);
            context.getNodeStatus().put(nodeId, NodeStatus.FAILED);
        }
    }

    /**
     * 节点完成回调
     * 
     * 核心调度逻辑:
     * 1. 获取当前节点的所有下游节点
     * 2. 对每个下游节点:
     * - 如果是 JOIN_ALL 模式: 入度 -1，入度为 0 则提交执行
     * - 如果是 JOIN_ANY 模式: CAS 竞争，成功则提交执行
     * 
     * @param nodeId        完成的节点 ID
     * @param context       上下文
     * @param nodeMap       节点映射表
     * @param adjacencyList 邻接表
     */
    private void onNodeComplete(
            String nodeId,
            WorkflowContext context,
            Map<String, Node> nodeMap,
            Map<String, List<String>> adjacencyList) {
        log.debug("Node {} completed, triggering downstream nodes", nodeId);

        // 1. 获取下游节点列表
        List<String> downstreamNodes = adjacencyList.getOrDefault(nodeId, Collections.emptyList());

        if (downstreamNodes.isEmpty()) {
            log.debug("Node {} has no downstream nodes", nodeId);
            checkWorkflowCompletion(context);
            return;
        }

        // 2. 处理每个下游节点
        for (String childId : downstreamNodes) {
            Node childNode = nodeMap.get(childId);

            if (childNode == null) {
                log.warn("Child node {} not found in nodeMap", childId);
                continue;
            }

            // Feature B: Race Condition / Join ANY
            if (childNode.getJoinMode() == JoinMode.ANY) {
                // JOIN_ANY 模式: 使用 CAS 竞争触发
                triggerJoinAnyNode(childId, childNode, context, nodeMap, adjacencyList);
            } else {
                // JOIN_ALL 模式 (默认): 使用入度递减
                triggerJoinAllNode(childId, childNode, context, nodeMap, adjacencyList);
            }
        }
    }

    /**
     * 触发 JOIN_ALL 节点
     * 
     * 使用入度递减逻辑:
     * - decrementAndGet() 原子递减入度
     * - 如果入度变为 0，则提交执行
     */
    private void triggerJoinAllNode(
            String nodeId,
            Node node,
            WorkflowContext context,
            Map<String, Node> nodeMap,
            Map<String, List<String>> adjacencyList) {
        AtomicInteger indegree = context.getIndegreeMap().get(nodeId);

        if (indegree == null) {
            log.warn("Indegree not found for node {}", nodeId);
            return;
        }

        // 原子递减入度
        int newIndegree = indegree.decrementAndGet();

        log.debug("Node {} indegree decremented: {} -> {}", nodeId, newIndegree + 1, newIndegree);

        // 入度为 0 时，提交执行
        if (newIndegree == 0) {
            log.info("Node {} all dependencies satisfied (JOIN_ALL), submitting execution", nodeId);
            submitNodeExecution(node, context, nodeMap, adjacencyList);
        }
    }

    /**
     * 触发 JOIN_ANY 节点
     * 
     * Feature B: Race Condition / Join ANY
     * 
     * 使用 CAS (Compare-And-Set) 保证只触发一次:
     * - 为每个 JOIN_ANY 节点分配一个 AtomicBoolean
     * - compareAndSet(false, true): 只有第一个到达的父节点能成功
     * - 后续父节点的 CAS 操作会失败，从而被忽略
     */
    private void triggerJoinAnyNode(
            String nodeId,
            Node node,
            WorkflowContext context,
            Map<String, Node> nodeMap,
            Map<String, List<String>> adjacencyList) {
        // 获取或创建 CAS 锁
        AtomicBoolean triggered = context.getAnyTriggerMap()
                .computeIfAbsent(nodeId, k -> new AtomicBoolean(false));

        // CAS 操作: 尝试将 false 改为 true
        // 只有第一个到达的线程能成功
        if (triggered.compareAndSet(false, true)) {
            log.info("Node {} first parent completed (JOIN_ANY), submitting execution", nodeId);
            submitNodeExecution(node, context, nodeMap, adjacencyList);
        } else {
            log.debug("Node {} already triggered by another parent (JOIN_ANY), ignoring", nodeId);
        }
    }

    /**
     * 恢复 CALLBACK 节点执行
     * 
     * Feature C: Async Callbacks
     * 
     * 当外部调用 POST /api/hooks/{callbackId} 时触发
     * 手动完成节点，并恢复下游调度
     * 
     * @param nodeId        节点 ID
     * @param payload       回调数据
     * @param context       上下文
     * @param nodeMap       节点映射表
     * @param adjacencyList 邻接表
     */
    public void resumeNode(
            String nodeId,
            String payload,
            WorkflowContext context,
            Map<String, Node> nodeMap,
            Map<String, List<String>> adjacencyList) {
        log.info("Resuming callback node: {}", nodeId);

        // 更新节点结果
        NodeResult result = context.getResults().get(nodeId);
        if (result != null) {
            result.setBody(payload);
        }

        // 更新状态为 SUCCESS
        context.getNodeStatus().put(nodeId, NodeStatus.SUCCESS);

        // 触发下游节点
        onNodeComplete(nodeId, context, nodeMap, adjacencyList);
    }

    /**
     * 检查工作流是否完成
     */
    private void checkWorkflowCompletion(WorkflowContext context) {
        boolean allCompleted = context.getNodeStatus().values().stream()
                .allMatch(status -> status == NodeStatus.SUCCESS ||
                        status == NodeStatus.FAILED ||
                        status == NodeStatus.WAITING_CALLBACK);

        if (allCompleted) {
            boolean hasWaiting = context.getNodeStatus().values().stream()
                    .anyMatch(status -> status == NodeStatus.WAITING_CALLBACK);

            if (hasWaiting) {
                context.setWorkflowStatus(WorkflowStatus.PARTIALLY_SUSPENDED);
                log.info("Workflow {} partially suspended (has waiting callbacks)", context.getRunId());
            } else {
                boolean hasFailed = context.getNodeStatus().values().stream()
                        .anyMatch(status -> status == NodeStatus.FAILED);

                context.setWorkflowStatus(hasFailed ? WorkflowStatus.FAILED : WorkflowStatus.COMPLETED);
                log.info("Workflow {} completed with status: {}", context.getRunId(), context.getWorkflowStatus());
            }
        }
    }
}
