package com.gengzi.workflow.model;

/**
 * 节点执行状态枚举
 * 
 * @author gengzi
 */
public enum NodeStatus {
    /**
     * 待执行 - 节点尚未开始执行
     */
    PENDING,

    /**
     * 执行中 - 节点正在执行
     */
    RUNNING,

    /**
     * 执行成功 - 节点执行完成且成功
     */
    SUCCESS,

    /**
     * 执行失败 - 节点执行失败
     */
    FAILED,

    /**
     * 等待回调 - 节点已挂起，等待外部回调唤醒
     */
    WAITING_CALLBACK
}
