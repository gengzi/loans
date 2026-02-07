package com.gengzi.workflow.model;

/**
 * 节点类型枚举
 * 
 * @author gengzi
 */
public enum NodeType {
    /**
     * HTTP 请求节点 - 发起 HTTP 调用
     */
    HTTP,

    /**
     * 回调节点 - 等待外部异步回调
     */
    CALLBACK
}
