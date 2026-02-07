package com.gengzi.workflow.model;

/**
 * 节点循环策略枚举
 * 控制单个节点内部的循环执行逻辑
 * 
 * @author gengzi
 */
public enum LoopPolicy {
    /**
     * 无循环 - 节点执行一次
     */
    NONE,

    /**
     * 固定次数循环 - 执行 N 次
     * 需配合 LoopConfig.maxRetries 使用
     */
    FIXED_COUNT,

    /**
     * 轮询直到成功 - 重复执行直到满足成功条件
     * 成功条件: HTTP 200 或自定义断言通过
     * 需配合 LoopConfig.successCondition 使用
     */
    UNTIL_SUCCESS
}
