package com.gengzi.advisor;

import com.gengzi.dao.Conversation;
import com.gengzi.dao.repository.ConversationRepository;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.Optional;

/**
 *
 */
public class MessageChatRecordAdvisor implements BaseAdvisor {


    private ConversationRepository conversationRepository;

    private String conversationId;

    public MessageChatRecordAdvisor(ConversationRepository conversationRepository, String conversationId) {
        this.conversationRepository = conversationRepository;
        this.conversationId = conversationId;
    }

    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {

        Optional<Conversation> conversationOptional = conversationRepository.findById(conversationId);
        if(conversationOptional.isPresent()){
            // 存在就设置内容
            Conversation conversation = conversationOptional.get();
            String message = conversation.getMessage();


            UserMessage userMessage = chatClientRequest.prompt().getUserMessage();
            String text = userMessage.getText();

        }else{
            throw new IllegalArgumentException("conversationId is not valid");
        }




        return null;
    }

    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        return null;
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
