package com.gengzi.search.processors;


import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.postretrieval.document.DocumentPostProcessor;

import java.util.List;


/**
 * 对检索到的文档进行后处理
 */
public class RagDocumentPostProcessor implements DocumentPostProcessor {


    @Override
    public List<Document> process(Query query, List<Document> documents) {

        // 查询父文档内容，将相关的内容都塞给大模型


        return List.of();
    }
}
