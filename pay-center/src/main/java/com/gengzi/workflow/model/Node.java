package com.gengzi.workflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 工作流节点定义
 * 表示 DAG 图中的一个节点
 * 
 * @author gengzi
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Node {

    /**
     * 节点唯一标识符
     */
    private String id;

    /**
     * 节点类型: HTTP 或 CALLBACK
     */
    @Builder.Default
    private NodeType type = NodeType.HTTP;

    /**
     * HTTP 请求配置 (仅用于 HTTP 类型节点)
     */
    private HttpConfig config;

    /**
     * 下游节点 ID 列表
     * 当前节点执行完成后，触发这些节点的执行
     */
    @Builder.Default
    private List<String> nextNodes = new ArrayList<>();

    /**
     * 循环策略: NONE, FIXED_COUNT, UNTIL_SUCCESS
     */
    @Builder.Default
    private LoopPolicy loopPolicy = LoopPolicy.NONE;

    /**
     * 循环配置参数
     */
    private LoopConfig loopConfig;

    /**
     * 汇聚模式: ALL (所有上游完成) 或 ANY (任一上游完成)
     * 仅当节点有多个上游时才生效
     */
    @Builder.Default
    private JoinMode joinMode = JoinMode.ALL;

    /**
     * 节点描述 (可选)
     */
    private String description;
}
