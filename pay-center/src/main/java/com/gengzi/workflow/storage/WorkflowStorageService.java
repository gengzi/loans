package com.gengzi.workflow.storage;

import com.gengzi.workflow.context.WorkflowContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工作流存储服务
 * 
 * MVP 版本: 使用内存 ConcurrentHashMap 存储
 * 未来: 可替换为 Redis 实现
 * 
 * @author gengzi
 */
@Slf4j
@Service
public class WorkflowStorageService {

    /**
     * 存储所有工作流上下文
     * Key: runId, Value: WorkflowContext
     */
    private final ConcurrentHashMap<String, WorkflowContext> storage = new ConcurrentHashMap<>();

    /**
     * 保存工作流上下文
     */
    public void save(WorkflowContext context) {
        storage.put(context.getRunId(), context);
        log.debug("Saved workflow context: runId={}", context.getRunId());
    }

    /**
     * 获取工作流上下文
     */
    public WorkflowContext get(String runId) {
        return storage.get(runId);
    }

    /**
     * 删除工作流上下文
     */
    public void delete(String runId) {
        storage.remove(runId);
        log.debug("Deleted workflow context: runId={}", runId);
    }

    /**
     * 检查是否存在
     */
    public boolean exists(String runId) {
        return storage.containsKey(runId);
    }

    /**
     * 获取所有上下文
     * 用于回调 ID 查找
     */
    public List<WorkflowContext> getAllContexts() {
        return new ArrayList<>(storage.values());
    }

    /**
     * 根据回调 ID 查找上下文
     * 
     * @param callbackId 回调 ID
     * @return 上下文，如果未找到返回 null
     */
    public WorkflowContext findByCallbackId(String callbackId) {
        for (WorkflowContext context : storage.values()) {
            if (context.getCallbackMap().containsKey(callbackId)) {
                return context;
            }
        }
        return null;
    }
}
