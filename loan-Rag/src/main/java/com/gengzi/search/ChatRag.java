package com.gengzi.search;


import com.gengzi.chat.ChatClientConfig;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class ChatRag {

//    @Autowired
//    private VectorStore vectorStore;

    @Autowired
    @Qualifier("deepseekChatClient")
    private ChatClient chatClient;

    @GetMapping("/chat/rag")
    @ResponseBody
    public ChatResponse chatRag(@RequestParam String question){
//        ChatResponse response = chatClient.prompt()
//                .advisors(new QuestionAnswerAdvisor(vectorStore))
//                .user(question)
//                .call()
//                .chatResponse();
//        return response;
        return null;
    }





}
