package com.gengzi.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 边定义 (用于描述节点之间的依赖关系)
 * 
 * @author gengzi
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Edge {

    /**
     * 起始节点 ID
     */
    private String fromNodeId;

    /**
     * 目标节点 ID
     */
    private String toNodeId;
}
