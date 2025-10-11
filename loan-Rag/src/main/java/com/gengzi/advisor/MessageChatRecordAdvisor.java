package com.gengzi.advisor;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.gengzi.context.RagChatContext;
import com.gengzi.dao.Conversation;
import com.gengzi.dao.entity.RagAssistantMessageRag;
import com.gengzi.dao.entity.RagChatMessage;
import com.gengzi.dao.repository.ConversationRepository;
import org.springframework.ai.chat.client.ChatClientMessageAggregator;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 对话消息记录存储Advisor
 */
public class MessageChatRecordAdvisor implements BaseAdvisor {

    // 记录表
    private ConversationRepository conversationRepository;

    public MessageChatRecordAdvisor(ConversationRepository conversationRepository) {
        this.conversationRepository = conversationRepository;
    }


    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {
        // Get the scheduler from BaseAdvisor
        Scheduler scheduler = this.getScheduler();

        // Process the request with the before method
        return Mono.just(chatClientRequest)
                .publishOn(scheduler)
                .map(request -> this.before(request, streamAdvisorChain))
                .flatMapMany(streamAdvisorChain::nextStream)
                .transform(flux -> new ChatClientMessageAggregator().aggregateChatClientResponse(flux,
                        response -> this.after(response, streamAdvisorChain)));

    }

    private static RagChatContext getRagChatContext(Map<String, Object> context) {

        RagChatContext ragChatContext = (RagChatContext) context.get(RagChatContext.RAG_CHAT_CONTEXT);
        if (ragChatContext == null) {
            throw new IllegalArgumentException("ragChatContext is not valid");
        }
        return ragChatContext;
    }

    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        RagChatContext ragChatContext = getRagChatContext(chatClientRequest.context());
        final String conversationId = ragChatContext.getConversationId();
        final String chatId = ragChatContext.getChatId();
        Optional<Conversation> conversationOptional = conversationRepository.findById(conversationId);
        if (conversationOptional.isPresent()) {
            // 存在就设置内容
            Conversation conversation = conversationOptional.get();
            String message = conversation.getMessage();
            UserMessage userMessage = chatClientRequest.prompt().getUserMessage();
            String text = userMessage.getText();
            RagChatMessage chatMessage = new RagChatMessage();
            chatMessage.setId(chatId);
            chatMessage.setContent(text);
            chatMessage.setRole(MessageType.USER.name());
            chatMessage.setConversationId(conversationId);
            if (StrUtil.isNotBlank(message)) {
                List<RagChatMessage> list = JSONUtil.toList(message, RagChatMessage.class);
                list.add(chatMessage);
                conversation.setMessage(JSONUtil.toJsonStr(list));
                conversationRepository.save(conversation);
            } else {
                List<RagChatMessage> chatMessages = new ArrayList<>();
                chatMessages.add(chatMessage);
                conversation.setMessage(JSONUtil.toJsonStr(chatMessages));
                conversationRepository.save(conversation);
            }
        } else {
            throw new IllegalArgumentException("conversationId is not valid");
        }

        return chatClientRequest;
    }

    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        // 获取大模型的回答
        List<Message> assistantMessages = new ArrayList<>();
        RagChatContext ragChatContext = getRagChatContext(chatClientResponse.context());
        final String conversationId = ragChatContext.getConversationId();
        final String chatId = ragChatContext.getChatId();
        if (chatClientResponse.chatResponse() != null) {
            assistantMessages = chatClientResponse.chatResponse()
                    .getResults()
                    .stream()
                    .map(g -> (Message) g.getOutput())
                    .toList();
        }

        Optional<Conversation> conversationOptional = conversationRepository.findById(conversationId);
        if (conversationOptional.isPresent()) {
            Conversation conversation = conversationOptional.get();
            String message = conversation.getMessage();
            if (StrUtil.isNotBlank(message)) {
                List<RagChatMessage> list = JSONUtil.toList(message, RagChatMessage.class);
                RagAssistantMessageRag ragAssistantMessage = new RagAssistantMessageRag();
                ragAssistantMessage.setId(chatId);
                String answer = assistantMessages.stream().map(msg -> {
                    if (msg instanceof AssistantMessage) {
                        AssistantMessage assistant = (AssistantMessage) msg;
                        return assistant.getText();
                    }
                    return "";
                }).collect(Collectors.joining());
                ragAssistantMessage.setContent(answer);
                ragAssistantMessage.setRole(MessageType.ASSISTANT.name());
                ragAssistantMessage.setConversationId(conversationId);
//                assistantMessage.setPrompt();
                ragAssistantMessage.setCreatedAt(System.currentTimeMillis());
                list.add(ragAssistantMessage);
                conversation.setMessage(JSONUtil.toJsonStr(list));
                conversationRepository.save(conversation);
            }
        }
        return chatClientResponse;
    }

    @Override
    public int getOrder() {
        return Advisor.DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER + 1000;
    }
}
