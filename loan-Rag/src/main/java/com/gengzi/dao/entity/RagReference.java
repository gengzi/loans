package com.gengzi.dao.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.ai.document.Document;

import java.util.List;

/**
 * 引用的document
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RagReference {

    private List<Document> documents;

}
