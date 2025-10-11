package com.gengzi.search.service;

import com.gengzi.request.RagChatReq;
import reactor.core.publisher.Flux;

public interface ChatRagService {


    /**
     * rag对话
     *
     * @param ragChatReq rag对话参数
     * @return
     */
    Flux<String> chatRag(RagChatReq ragChatReq);


//    void chatRagCreate(RagChatReq req);
}
