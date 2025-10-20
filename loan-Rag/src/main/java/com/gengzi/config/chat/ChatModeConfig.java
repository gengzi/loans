package com.gengzi.config.chat;

import com.gengzi.reranker.DefaultRerankModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * 自定义构建对话模型配置
 */
@Configuration
public class ChatModeConfig {


    @Autowired
    private ChatModeParamsConfig config;

    @Autowired
    private RerankerParamsConfig rerankerParamsConfig;

    @Bean
    public OpenAiChatModel openAiChatModel() {
        OpenAiApi openApi = OpenAiApi.builder().apiKey(config.getApiKey())
                .baseUrl(config.getBaseUrl()).build();
        return OpenAiChatModel.builder()
                .openAiApi(openApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(config.getModel()).
                        temperature(config.getTemperature())
                        .build())
                .build();
    }


    @Bean
    public DefaultRerankModel defaultRerankModel() {
        return new DefaultRerankModel(rerankerParamsConfig.getApiKey(), rerankerParamsConfig.getBaseUrl(), rerankerParamsConfig.getModel());
    }


}
