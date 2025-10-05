package com.gengzi.chat;


import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {


    @Bean
    public OpenAiChatModel openAiChatModel() {
        OpenAiApi openApi = OpenAiApi.builder().apiKey("sk-ltrjtwcekfkowwmdqghjzgfkjhylcocxibuuviorbnfzvqqj")
                .baseUrl("https://api.siliconflow.cn").build();
        return OpenAiChatModel.builder()
                .openAiApi(openApi)
                .defaultOptions(OpenAiChatOptions.builder().model("deepseek-ai/DeepSeek-V3").build())
                .build();
    }


    @Bean
    public ChatClient deepseekChatClient(OpenAiChatModel chatModel) {
        return ChatClient.create(chatModel);
    }


}
