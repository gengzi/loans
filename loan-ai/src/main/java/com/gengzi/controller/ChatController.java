package com.gengzi.controller;


import com.gengzi.entity.UserChat;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
public class ChatController {


    @Autowired
    @Qualifier("deepseekChatClient")
    private ChatClient chatClient;


    @PostMapping("/chat")
    public Flux<String> chat(@RequestBody UserChat userChat){
        return chatClient.prompt()
                .system("你是一名翻译助手，请把用户发送的内容翻译为繁体中文")
                .user(userChat.getQuestion())
                .stream()
                .content();
    }




}
