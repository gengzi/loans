package com.gengzi.search;


import com.gengzi.search.advisor.RagRetrievalAugmenttationAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
public class ChatRag {

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    @Qualifier("deepseekChatClient")
    private ChatClient chatClient;

    @Autowired
    @Qualifier("ragAdvisor")
    private Advisor advisor;

    @GetMapping("/chat/rag")
    public Flux<String> chatRag(@RequestParam String question) {

        Flux<String> content = chatClient.prompt(question)
                .advisors(advisor)
                .stream()
                .content();
        return content;
    }


}
