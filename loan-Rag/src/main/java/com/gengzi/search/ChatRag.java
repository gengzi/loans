package com.gengzi.search;


import cn.hutool.core.util.StrUtil;
import com.gengzi.search.service.IntentAnalysisRagService;
import com.gengzi.utils.IdUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
public class ChatRag {
    private static final Logger logger = LoggerFactory.getLogger(ChatRag.class);
    @Autowired
    private VectorStore vectorStore;

    @Autowired
    @Qualifier("deepseekChatClient")
    private ChatClient chatClient;


    @Autowired
    @Qualifier("deepseekChatClientByRag")
    private ChatClient chatClientByRag;


    @Autowired
    private ChatMemory chatMemory;

    @Autowired
    private IntentAnalysisRagService intentAnalysisRagService;

    @GetMapping("/chat/rag")
    public Flux<String> chatRag(@RequestParam String question) {

        // 会话id
        String conversationId = "002";






        List<Message> messages = chatMemory.get(conversationId);
        logger.info("messages: {}", messages.stream().map(Message::toString).toList());


        String intentAnalysis = intentAnalysisRagService.intentAnalysis(question, conversationId);
        if(StrUtil.isNotBlank(intentAnalysis)){
            return Flux.just(intentAnalysis);
        }


        Flux<String> content = chatClient.prompt()
                .user(question)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .stream()
                .content();

        // 流完成事件
        content.doOnComplete(()->{

        });


        // 返回用户问题后，还需要拼接上参考的文档信息，文档链接
        return content;
    }


}
