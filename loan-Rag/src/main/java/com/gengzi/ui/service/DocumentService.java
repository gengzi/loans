package com.gengzi.ui.service;


import com.gengzi.response.DocumentPreviewResponse;

/**
 * 文档相关操作service
 */
public interface DocumentService {


    void documentToEmbedding(String documentId);


    DocumentPreviewResponse documentPreview(String documentId);
}
