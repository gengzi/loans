package com.gengzi.workflow.model;

/**
 * 工作流执行状态枚举
 * 
 * @author gengzi
 */
public enum WorkflowStatus {
    /**
     * 运行中 - 工作流正在执行
     */
    RUNNING,

    /**
     * 已完成 - 工作流所有节点执行完成
     */
    COMPLETED,

    /**
     * 失败 - 工作流因错误终止
     */
    FAILED,

    /**
     * 部分暂停 - 存在等待回调的节点
     */
    PARTIALLY_SUSPENDED
}
