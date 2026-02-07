package com.gengzi.workflow.dto;

import com.gengzi.workflow.model.WorkflowStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 工作流执行响应 DTO
 * 
 * @author gengzi
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowRunResponse {

    /**
     * 执行 ID
     */
    private String runId;

    /**
     * 工作流状态
     */
    private WorkflowStatus status;

    /**
     * 消息
     */
    private String message;
}
