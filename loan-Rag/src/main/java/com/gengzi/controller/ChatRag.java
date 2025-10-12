package com.gengzi.controller;


import com.gengzi.request.RagChatCreateReq;
import com.gengzi.request.RagChatReq;
import com.gengzi.response.Result;
import com.gengzi.search.service.ChatRagService;
import com.gengzi.search.service.IntentAnalysisRagService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@Tag(name = "rag对话", description = "rag对话")
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


    @Autowired
    private ChatRagService chatRagService;

    @PostMapping("/chat/rag/create")
    public Result<Boolean> chatRagCreate(@RequestBody RagChatCreateReq req) {
        chatRagService.chatRagCreate(req);
        return Result.success(true);
    }

    /**
     * 获取的当前创建的所有对话列表
     */
    @GetMapping("/chat/rag/all")
    public Result<?> chatRagAll() {
        return Result.success(chatRagService.chatRagAll());
    }


    @PostMapping("/chat/rag")
    public Flux<String> chatRag(@RequestBody RagChatReq req) {
        return chatRagService.chatRag(req);
    }


    @GetMapping("/chat/rag/msg/list")
    public Result<?> chatRagMsgList(@RequestParam String conversationId) {
        return Result.success(chatRagService.chatRagMsgList(conversationId));
    }


}
