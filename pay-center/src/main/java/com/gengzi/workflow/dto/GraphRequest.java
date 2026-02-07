package com.gengzi.workflow.dto;

import com.gengzi.workflow.model.Node;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 工作流图请求 DTO
 * 接收前端传递的 JSON DAG 图定义
 * 
 * @author gengzi
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GraphRequest {

    /**
     * 节点列表
     */
    private List<Node> nodes;

    /**
     * 边列表 (描述节点依赖关系)
     */
    private List<Edge> edges;
}
