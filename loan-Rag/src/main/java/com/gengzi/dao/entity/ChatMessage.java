package com.gengzi.dao.entity;

import lombok.Data;

import java.util.List;

@Data
public class ChatMessage {
    private String content;
    private String id;
    private String role;
    private String conversationId;
    private String sessionId;
    private List<String> docIds;

}
