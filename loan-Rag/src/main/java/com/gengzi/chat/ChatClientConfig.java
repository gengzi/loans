package com.gengzi.chat;


import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {

    @Autowired
    @Qualifier("ragAdvisor")
    private Advisor advisor;

    @Autowired
    @Qualifier("jdbcChatMemory")
    private ChatMemory chatMemory;


    /**
     * 执行rag流程
     * @param chatModel
     * @return
     */
    @Bean
    public ChatClient deepseekChatClientByRag(OpenAiChatModel chatModel) {
        return ChatClient.builder(chatModel).defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build(), advisor).build();
    }

    /**
     * 不执行rag流程
     * @param chatModel
     * @return
     */
    @Bean
    public ChatClient deepseekChatClient(OpenAiChatModel chatModel) {
        return ChatClient.builder(chatModel).defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build()).build();
    }


}
