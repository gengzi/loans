package com.gengzi.workflow.model;

/**
 * 节点汇聚模式枚举
 * 用于多个上游节点汇聚到同一个下游节点时的触发策略
 * 
 * @author gengzi
 */
public enum JoinMode {
    /**
     * 所有上游节点都完成后才触发下游节点 (默认)
     * 使用入度计数器实现
     */
    ALL,

    /**
     * 任意一个上游节点完成即触发下游节点 (竞争模式)
     * 使用 CAS (AtomicBoolean) 保证只触发一次
     */
    ANY
}
