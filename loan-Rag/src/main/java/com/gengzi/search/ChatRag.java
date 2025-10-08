package com.gengzi.search;


import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.template.st.StTemplateRenderer;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class ChatRag {

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    @Qualifier("deepseekChatClient")
    private ChatClient chatClient;

    @GetMapping("/chat/rag")
    @ResponseBody
    public ChatResponse chatRag(@RequestParam String question) {
        PromptTemplate customPromptTemplate = PromptTemplate.builder()
                .renderer(StTemplateRenderer.builder().startDelimiterToken('<').endDelimiterToken('>').build())
                .template("""
                        你是本地知识库的专家，你需要从上下文内容中的答案回答用户问题
                        <query>
                        上下文内容如下:
                        ---------------------
                        <question_answer_context>
                        ---------------------
                        在给定上下文信息且没有其他知识的情况下，回答问题。
                        必须遵循以下规则：
                        1. 如果上下文内容为空，就说此知识库无此信息，不要从不是上下文的内容回答用户问题。
                        2. 避免使用“基于上下文…”或“所提供的信息…”等陈述。。。".
                        """)
                .build();

        var qaAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
                .promptTemplate(customPromptTemplate)
                .searchRequest(SearchRequest.builder()
                        .similarityThreshold(0.6d)
                        .topK(10)
                        .build())
                .build();
        ChatResponse response = chatClient.prompt()
                .advisors(qaAdvisor)
                .user(question)
                .call()
                .chatResponse();
        return response;
    }


}
