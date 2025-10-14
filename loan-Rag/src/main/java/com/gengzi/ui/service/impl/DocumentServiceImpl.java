package com.gengzi.ui.service.impl;

import cn.hutool.core.util.StrUtil;
import com.gengzi.config.S3Properties;
import com.gengzi.dao.Document;
import com.gengzi.dao.repository.DocumentRepository;
import com.gengzi.embedding.load.pdf.OcrPdfReader;
import com.gengzi.enums.FileProcessStatusEnum;
import com.gengzi.enums.S3FileType;
import com.gengzi.response.DocumentPreviewResponse;
import com.gengzi.response.ResultCode;
import com.gengzi.s3.S3ClientUtils;
import com.gengzi.security.BusinessException;
import com.gengzi.ui.service.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

import java.net.URL;
import java.util.Optional;

@Service
public class DocumentServiceImpl implements DocumentService {


    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private S3ClientUtils s3ClientUtils;

    @Autowired
    private S3Properties s3Properties;


    @Autowired
    private OcrPdfReader reader;

    /**
     * 将doc进行embeding 处理
     *
     * @param documentId
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void documentToEmbedding(String documentId) {
        if (StrUtil.isBlankIfStr(documentId)) {
            return;
        }
        Optional<Document> documentOptional = documentRepository.findById(documentId);
        if (!documentOptional.isPresent()) {
            return;
        }
        Document document = documentOptional.get();
        String file = document.getLocation();
        HeadObjectResponse headObjectResponse = s3ClientUtils.headObject(s3Properties.getDefaultBucketName(), file);
        String contentType = headObjectResponse.contentType();
        S3FileType s3FileType = S3FileType.fromMimeType(contentType);
        // 使用switch处理不同文件类型
        switch (s3FileType) {
            case PDF:
                // 处理pdf的解析流程
                reader.pdfParse(file, documentId);
                documentRepository.updateStatusById(documentId, String.valueOf(FileProcessStatusEnum.PROCESSING.getCode()));
                break;
            case UNKNOWN:
                break;
            // 不需要default，因为枚举已覆盖所有可能
        }
    }

    /**
     * @param documentId
     * @return
     */
    @Override
    public DocumentPreviewResponse documentPreview(String documentId) {
        Optional<Document> documentOptional = documentRepository.findById(documentId);
        if (!documentOptional.isPresent()) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }
        DocumentPreviewResponse documentPreviewResponse = new DocumentPreviewResponse();
        Document document = documentOptional.get();
        String file = document.getLocation();
        HeadObjectResponse headObjectResponse = s3ClientUtils.headObject(s3Properties.getDefaultBucketName(), file);
        URL url = s3ClientUtils.generatePresignedUrl(s3Properties.getDefaultBucketName(), file);
        String contentType = headObjectResponse.contentType();
        documentPreviewResponse.setContentType(contentType);
        documentPreviewResponse.setUrl(url.toString());
        return documentPreviewResponse;
    }
}
