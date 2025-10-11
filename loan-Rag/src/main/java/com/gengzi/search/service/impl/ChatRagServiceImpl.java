package com.gengzi.search.service.impl;

import cn.hutool.core.util.StrUtil;
import com.gengzi.context.RagChatContext;
import com.gengzi.dao.Conversation;
import com.gengzi.dao.repository.ConversationRepository;
import com.gengzi.request.RagChatReq;
import com.gengzi.search.service.ChatRagService;
import com.gengzi.security.UserPrincipal;
import com.gengzi.utils.IdUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
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
        RagChatContext ragChatContext = new RagChatContext(chatId, conversationId,userId);
        Flux<String> content = chatClient.prompt()
                .user(question)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .advisors(advisorSpec -> advisorSpec.param(RagChatContext.RAG_CHAT_CONTEXT, ragChatContext))
                .stream()
                .content();

        // 返回用户问题后，还需要拼接上参考的文档信息，文档链接
        return content;
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
