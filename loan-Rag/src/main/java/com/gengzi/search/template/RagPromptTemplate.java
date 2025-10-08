package com.gengzi.search.template;

import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.template.st.StTemplateRenderer;
import org.springframework.stereotype.Component;

/**
 * 适用于rag 系统的提示词模板
 */
@Component
public class RagPromptTemplate {


    public PromptTemplate ragPromptTemplate() {

        PromptTemplate customPromptTemplate = PromptTemplate.builder()
                .renderer(StTemplateRenderer.builder().startDelimiterToken('<').endDelimiterToken('>').build())
                .template("""
                        你是本地知识库的专家，你需要从上下文内容中的答案回答用户问题
                        <query>
                        上下文内容如下:
                        ---------------------
                        <context>
                        ---------------------
                        在给定上下文信息且没有其他知识的情况下，回答问题。当然你可以从之前的聊天记录中获取问题的其他信息
                        必须遵循以下规则：
                        1. 如果上下文内容为空，就说此知识库无此信息，不要从不是上下文的内容回答用户问题。
                        2. 避免使用“基于上下文…”或“所提供的信息…”等陈述。。。".
                        """)
                .build();
        return customPromptTemplate;
    }
}
