package com.gengzi.search.advisor;


import com.gengzi.search.template.RagPromptTemplate;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * RAG 检索增强器
 */
@Component
public class RagRetrievalAugmenttationAdvisor {


    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private RagPromptTemplate ragPromptTemplate;


    @Bean("ragAdvisor")
    public Advisor createAdvisor() {
        Advisor retrievalAugmentationAdvisor = RetrievalAugmentationAdvisor.builder()
                // 用于转换输入查询，使得更有效的执行检索
                .queryTransformers()
                // 检索器
                .documentRetriever(VectorStoreDocumentRetriever.builder()
                        // 相似度
                        .similarityThreshold(0.70)
                        // topk
                        .topK(10)
                        // 使用的向量数据库
                        .vectorStore(vectorStore)
                        .build())
                // 查询参数 这里设置模板
                .queryAugmenter(ContextualQueryAugmenter.builder()
                        // 设置提示词模板
                        .promptTemplate(ragPromptTemplate.ragPromptTemplate())
                        .emptyContextPromptTemplate(new PromptTemplate("知识库无此信息"))
                        .build())
                // 将检索到的文档处理之后，再传递给大模型
                .documentPostProcessors()
                // 将多个数据源检索到的文档合并为单个文档
//                .documentJoiner()
                .build();
        return retrievalAugmentationAdvisor;
    }

}
