package com.gengzi.dao.entity;

import com.gengzi.context.DocumentMetadataMap;
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

    /**
     * 本地对话id
     */
    private String chatid;

    /**
     * 引用文档块信息
     */
    private List<ReferenceDocument> reference;

    /**
     * 根据检索到的文档转换为引用映射
     *
     * @param documents
     * @return
     */
    public static RagReference listDocumentToRagReference(List<Document> documents) {
        RagReference ragReference = new RagReference();
        ragReference.setReference(documents.stream().map(document -> {
            ReferenceDocument referenceDocument = new ReferenceDocument();
            referenceDocument.setChunkId(document.getId());
            referenceDocument.setDocumentId((String) document.getMetadata().get(DocumentMetadataMap.DOCUMENT_ID));
            referenceDocument.setDocumentName((String) document.getMetadata().get(DocumentMetadataMap.DOCUMENT_NAME));
            referenceDocument.setText(document.getText());
            referenceDocument.setScore(String.valueOf(document.getScore()));
            referenceDocument.setPageRange((String) document.getMetadata().get(DocumentMetadataMap.PAGE_RANGE));
            referenceDocument.setContentType((String) document.getMetadata().get(DocumentMetadataMap.CONTENT_TYPE));
            // 根据documentId 获取文档的url
            referenceDocument.setDocumentUrl("/document/" + document.getMetadata().get(DocumentMetadataMap.DOCUMENT_ID));
            return referenceDocument;
        }).toList());
        return ragReference;
    }


}

