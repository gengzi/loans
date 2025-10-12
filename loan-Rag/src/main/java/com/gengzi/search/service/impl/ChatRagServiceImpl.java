package com.gengzi.search.service.impl;

import cn.hutool.core.util.StrUtil;
import com.gengzi.context.RagChatContext;
import com.gengzi.dao.Conversation;
import com.gengzi.dao.repository.ConversationRepository;
import com.gengzi.request.RagChatCreateReq;
import com.gengzi.request.RagChatReq;
import com.gengzi.response.ConversationDetailsResponse;
import com.gengzi.response.ConversationResponse;
import com.gengzi.search.service.ChatRagService;
import com.gengzi.security.UserPrincipal;
import com.gengzi.utils.IdUtils;
import com.gengzi.utils.UserDetails;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ChatRagServiceImpl implements ChatRagService {


    @Autowired
    private VectorStore vectorStore;

    @Autowired
    @Qualifier("deepseekChatClient")
    private ChatClient chatClient;

    @Autowired
    @Qualifier("deepseekChatClientByRag")
    private ChatClient chatClientByRag;


    @Autowired
    private ConversationRepository conversationRepository;


    /**
     * rag对话
     *
     * @param ragChatReq rag对话参数
     * @return
     */
    @Override
    public Flux<String> chatRag(RagChatReq ragChatReq) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal details = (UserPrincipal) authentication.getPrincipal();

        final String userId = details.getId();

        if (StrUtil.isBlank(ragChatReq.getConversationId())) {
            // 新建一个聊天窗口会话
            String conversationId = chatRagCreate(ragChatReq, userId);
            ragChatReq.setConversationId(conversationId);
        }
        // 获取用户信息
        // 获取用户问题
        String question = ragChatReq.getQuestion();
        // 会话id
        String conversationId = ragChatReq.getConversationId();
        String chatId = IdUtils.generate();
        RagChatContext ragChatContext = new RagChatContext(chatId, conversationId, userId);
        Flux<String> content = chatClientByRag.prompt()
                .user(question)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .advisors(advisorSpec -> advisorSpec.param(RagChatContext.RAG_CHAT_CONTEXT, ragChatContext))
                .stream()
                .content();
        Flux<ChatResponse> chatResponseFlux = chatClientByRag.prompt()
                .user(question)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .advisors(advisorSpec -> advisorSpec.param(RagChatContext.RAG_CHAT_CONTEXT, ragChatContext))
                .stream()
                .chatResponse();

        // 返回用户问题后，还需要拼接上参考的文档信息，文档链接
        return content;
    }

    /**
     * rag对话窗口创建
     *
     * @param req
     */
    @Override
    public void chatRagCreate(RagChatCreateReq req) {
        // 向会话表插入一条会话信息
        // 会话id
        String conversationId = IdUtils.generate();
        LocalDateTime now = LocalDateTime.now();
        // 为LocalDateTime指定时区（这里使用系统默认时区）
        ZonedDateTime zonedDateTime = now.atZone(ZoneId.systemDefault());
        // 获取毫秒级时间戳（从1970-01-01T00:00:00Z开始的毫秒数）
        long millis = zonedDateTime.toInstant().toEpochMilli();
        conversationRepository.save(Conversation.builder()
                .id(conversationId)
                .knowledgebaseId(req.getKbId())
                .dialogId("")
                .createDate(now)
                .createTime(millis)
                .updateDate(now)
                .updateTime(millis)
                .name(req.getChatName())
                .message("[]")
                .reference("[]")
                .userId(UserDetails.getUser().getId())
                .build());
    }

    /**
     * @return
     */
    @Override
    public List<ConversationResponse> chatRagAll() {
        UserPrincipal user = UserDetails.getUser();
        List<Conversation> conversations = conversationRepository.findByUserId(user.getId());
        ArrayList<ConversationResponse> conversationResponses = new ArrayList<>();
        conversations.forEach(conversation -> {
            ConversationResponse response = ConversationResponse.builder()
                    .id(conversation.getId())
                    .dialogId(conversation.getDialogId())
                    .knowledgebaseId(conversation.getKnowledgebaseId())
                    .name(conversation.getName())
                    .createTime(conversation.getCreateTime())
                    .createDate(conversation.getCreateDate())
                    .updateTime(conversation.getUpdateTime())
                    .updateDate(conversation.getUpdateDate())
                    .build();
            conversationResponses.add(response);
        });
        return conversationResponses;
    }

    /**
     * @param conversationId
     * @return
     */
    @Override
    public ConversationDetailsResponse chatRagMsgList(String conversationId) {
        ConversationDetailsResponse conversationDetailsResponse = new ConversationDetailsResponse();
        Optional<Conversation> conversationRepositoryById = conversationRepository.findById(conversationId);
        if (conversationRepositoryById.isPresent()) {
            Conversation conversation = conversationRepositoryById.get();
            conversationDetailsResponse.setId(conversationId);
            conversationDetailsResponse.setName(conversation.getName());
            conversationDetailsResponse.setMessage(conversation.getMessage());
            conversationDetailsResponse.setReference(conversation.getReference());
            conversationDetailsResponse.setUpdateTime(conversation.getUpdateTime());
            conversationDetailsResponse.setUpdateDate(conversation.getUpdateDate());
            conversationDetailsResponse.setCreateTime(conversation.getCreateTime());
            conversationDetailsResponse.setCreateDate(conversation.getCreateDate());
            conversationDetailsResponse.setDialogId(conversation.getDialogId());
            conversationDetailsResponse.setKnowledgebaseId(conversation.getKnowledgebaseId());
            conversationDetailsResponse.setUserId(conversation.getUserId());
        }
        return conversationDetailsResponse;
    }

    public String chatRagCreate(RagChatReq req, String userId) {
        // 向会话表插入一条会话信息
        // 会话id
        String conversationId = IdUtils.generate();
        LocalDateTime now = LocalDateTime.now();
        // 为LocalDateTime指定时区（这里使用系统默认时区）
        ZonedDateTime zonedDateTime = now.atZone(ZoneId.systemDefault());
        // 获取毫秒级时间戳（从1970-01-01T00:00:00Z开始的毫秒数）
        long millis = zonedDateTime.toInstant().toEpochMilli();
        conversationRepository.save(Conversation.builder()
                .id(conversationId)
                .dialogId(userId)
                .createDate(now)
                .createTime(millis)
                .updateDate(now)
                .updateTime(millis)
                .name(req.getQuestion())
                .message("[]")
                .reference("[]")
                .userId(userId)
                .build());
        return conversationId;

    }
}
