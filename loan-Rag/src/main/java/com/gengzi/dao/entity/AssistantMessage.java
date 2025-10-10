package com.gengzi.dao.entity;

import lombok.Data;

@Data
public class AssistantMessage extends ChatMessage {
    private String answer;
    private double createdAt;
    private String prompt;

}
