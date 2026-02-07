package com.gengzi.workflow.dto;

import com.gengzi.workflow.model.NodeResult;
import com.gengzi.workflow.model.NodeStatus;
import com.gengzi.workflow.model.WorkflowStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 工作流状态响应 DTO
 * 
 * @author gengzi
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowStatusResponse {

    /**
     * 执行 ID
     */
    private String runId;

    /**
     * 工作流整体状态
     */
    private WorkflowStatus workflowStatus;

    /**
     * 各节点执行状态
     */
    private Map<String, NodeStatus> nodeStatuses;

    /**
     * 各节点执行结果
     */
    private Map<String, NodeResult> results;

    /**
     * 执行总耗时 (毫秒)
     */
    private long totalDurationMs;
}
