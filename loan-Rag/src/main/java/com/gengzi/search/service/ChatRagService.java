package com.gengzi.search.service;

import com.gengzi.request.RagChatCreateReq;
import com.gengzi.request.RagChatReq;
import com.gengzi.response.ConversationDetailsResponse;
import com.gengzi.response.ConversationResponse;
import reactor.core.publisher.Flux;

import java.util.List;

public interface ChatRagService {


    /**
     * rag对话
     *
     * @param ragChatReq rag对话参数
     * @return
     */
    Flux<String> chatRag(RagChatReq ragChatReq);


    void chatRagCreate(RagChatCreateReq req);

    List<ConversationResponse> chatRagAll();

    ConversationDetailsResponse chatRagMsgList(String conversationId);
}
