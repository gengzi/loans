package com.gengzi.workflow.context;

import com.gengzi.workflow.model.NodeResult;
import com.gengzi.workflow.model.NodeStatus;
import com.gengzi.workflow.model.WorkflowStatus;
import lombok.Getter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 工作流执行上下文
 * 线程安全的执行状态容器，存储整个 DAG 执行过程的所有状态和结果
 * 
 * 线程安全保证:
 * - 所有 Map 使用 ConcurrentHashMap
 * - 入度使用 AtomicInteger
 * - JOIN_ANY 触发使用 AtomicBoolean
 * - WorkflowStatus 使用 volatile
 * 
 * @author gengzi
 */
@Getter
public class WorkflowContext {

    /**
     * 本次执行唯一 ID
     */
    private final String runId;

    /**
     * 节点执行结果映射
     * Key: 节点 ID, Value: 执行结果
     */
    private final ConcurrentHashMap<String, NodeResult> results;

    /**
     * 节点状态映射
     * Key: 节点 ID, Value: 当前状态
     */
    private final ConcurrentHashMap<String, NodeStatus> nodeStatus;

    /**
     * 节点入度映射 (用于 DAG 调度)
     * Key: 节点 ID, Value: 当前入度 (使用 AtomicInteger 保证原子递减)
     * 
     * 调度逻辑:
     * - 初始化: 计算每个节点的入度
     * - 触发条件: 入度为 0 时可以执行
     * - 更新: 节点完成后，所有下游节点入度 -1
     */
    private final ConcurrentHashMap<String, AtomicInteger> indegreeMap;

    /**
     * JOIN_ANY 模式触发锁
     * Key: 节点 ID, Value: 是否已触发 (AtomicBoolean)
     * 
     * 用于实现竞争模式:
     * - 当节点的 joinMode 为 ANY 时，多个上游节点竞争触发
     * - 使用 compareAndSet(false, true) 确保只有一个上游能成功触发
     */
    private final ConcurrentHashMap<String, AtomicBoolean> anyTriggerMap;

    /**
     * 回调 ID 映射
     * Key: 回调 ID (UUID), Value: 节点 ID
     * 
     * 用于异步回调唤醒:
     * - CALLBACK 节点生成唯一 callbackId 并注册
     * - 外部调用 /api/hooks/{callbackId} 时，根据 ID 找到对应节点
     * - 手动触发节点完成逻辑，恢复下游调度
     */
    private final ConcurrentHashMap<String, String> callbackMap;

    /**
     * 工作流整体状态 (volatile 保证可见性)
     */
    private volatile WorkflowStatus workflowStatus;

    /**
     * 工作流开始时间 (毫秒时间戳)
     */
    private final long startTimeMs;

    /**
     * 节点映射表 (用于回调恢复)
     * Key: 节点 ID, Value: 节点对象
     * 存储原始节点定义，以便 CALLBACK 节点恢复时使用
     */
    private final ConcurrentHashMap<String, com.gengzi.workflow.model.Node> nodeMap;

    /**
     * 邻接表 (用于回调恢复)
     * Key: 节点 ID, Value: 下游节点 ID 列表
     * 存储图的拓扑结构，以便 CALLBACK 节点恢复时触发下游
     */
    private final ConcurrentHashMap<String, java.util.List<String>> adjacencyList;

    public WorkflowContext(String runId) {
        this.runId = runId;
        this.results = new ConcurrentHashMap<>();
        this.nodeStatus = new ConcurrentHashMap<>();
        this.indegreeMap = new ConcurrentHashMap<>();
        this.anyTriggerMap = new ConcurrentHashMap<>();
        this.callbackMap = new ConcurrentHashMap<>();
        this.nodeMap = new ConcurrentHashMap<>();
        this.adjacencyList = new ConcurrentHashMap<>();
        this.workflowStatus = WorkflowStatus.RUNNING;
        this.startTimeMs = System.currentTimeMillis();
    }

    /**
     * 更新工作流状态
     * 
     * @param status 新状态
     */
    public void setWorkflowStatus(WorkflowStatus status) {
        this.workflowStatus = status;
    }

    /**
     * 获取执行总耗时 (毫秒)
     * 
     * @return 耗时
     */
    public long getTotalDurationMs() {
        return System.currentTimeMillis() - startTimeMs;
    }
}
