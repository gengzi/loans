package com.gengzi.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gengzi.context.DocumentMetadataMap;
import com.gengzi.context.FileContext;
import com.gengzi.response.LayoutParsingPageItem;
import com.gengzi.response.LayoutParsingResponse;
import com.gengzi.s3.S3ClientUtils;
import com.gengzi.utils.Base64ImageConverter;
import com.gengzi.utils.FileIdGenerator;
import org.apache.hc.core5.http.ContentType;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 版面解析响应结果 → Spring AI Document 转换工具类
 */
@Component
public class LayoutResponseToDocumentConverter {

    @Autowired
    private S3ClientUtils s3ClientUtils;

    /**
     * 将版面解析响应转换为 Spring AI Document 列表
     * （1个PDF多页 → 多个Document；1个图像 → 1个Document）
     *
     * @param response 版面解析接口响应结果
     * @param context  原始文件信息
     * @return 转换后的 Document 列表（失败场景返回含错误信息的Document）
     */
    public List<Document> convert(LayoutParsingResponse response, FileContext context) {
        List<Document> documentList = new ArrayList<>();

        // 1. 处理响应失败场景：返回含错误信息的Document
        if (response == null || !response.isSuccess()) {
            throw new RuntimeException("版面解析接口调用失败！错误码：" + response.getErrorCode() + "，错误信息：" + response.getErrorMsg());
        }

        // 2. 处理响应成功场景：按页码拆分生成Document（1页1个Document）
        String logId = response.getLogId(); // 接口返回的唯一请求ID（用于Document.id前缀）
        List<LayoutParsingPageItem> pageItems = response.getResult().getLayoutParsingResults();

        for (int i = 0; i < pageItems.size(); i++) {
            LayoutParsingPageItem pageItem = pageItems.get(i);
            int pageNum = i + 1; // 页码（从1开始）


            // 2.1 构建Document的唯一ID（logId + 页码，确保全局唯一）
            String fileId = FileIdGenerator.generateFileId(context.getKey());
            String filePageId = String.format("%s_%d", fileId, pageNum);

            saveParseResult(pageItem, fileId, context, pageNum);

            // 2.2 提取核心文本内容（优先用Markdown.text，无则用prunedResult的JSON字符串）
            String content = extractContent(pageItem);
//
            // 2.3 构建元数据（存储页码、文件类型、图像Base64等额外信息）
            Map<String, Object> metadata = buildMetadata(filePageId, pageNum);


//            // 2.4 创建Document对象并添加到列表
//            EsVectorDocument esVectorDocument = new EsVectorDocument();
//            esVectorDocument.setChunkId(filePageId);
//            esVectorDocument.setDocId(fileId);
//            esVectorDocument.setContent(content);
//            esVectorDocument.setMetadata(metadata);
//            ExtendedDocument document = new ExtendedDocument(esVectorDocument);
            Document document = new Document(content, metadata);
            documentList.add(document);
        }

        return documentList;
    }


    /**
     * 存储markdown 文件和json文件和img 图片
     */
    private void saveParseResult(LayoutParsingPageItem pageItem, String fileId, FileContext fileContext, int pageNum) {

        String fileKey = String.format("%s/%s_%d", fileId, fileId, pageNum);
        // json文件信息
        if (pageItem.getPrunedResult() != null) {
            String json = null;
            try {
                json = new ObjectMapper()
                        .writerWithDefaultPrettyPrinter()
                        .writeValueAsString(pageItem.getPrunedResult());
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            s3ClientUtils.putObjectByContentBytes(fileContext.getBucketName(), fileKey + ".json", json.getBytes(), "application/json");
        }

        // 优先取Markdown文本（结构化结果，最适合作为Document内容）
        if (pageItem.getMarkdown() != null && pageItem.getMarkdown().getText() != null) {
            String markdown = pageItem.getMarkdown().getText().trim();
            s3ClientUtils.putObjectByContentBytes(fileContext.getBucketName(), fileKey + ".md", markdown.getBytes(), "text/markdown");
        }

        // 存储图片
        if (pageItem.getOutputImages() != null && !pageItem.getOutputImages().isEmpty()) {
            for (Map.Entry<String, String> entry : pageItem.getOutputImages().entrySet()) {
                String imageName = entry.getKey();
                String imageBase64 = entry.getValue();
                s3ClientUtils.putObjectByContentBytes(fileContext.getBucketName(), fileKey + "_" + imageName + ".jpeg", Base64ImageConverter.base64ToBytes(imageBase64), "image/jpeg");
            }
        }

    }

    /**
     * 提取单页的核心文本内容
     * 优先级：Markdown.text → prunedResult（转为JSON字符串）→ "无文本内容"
     */
    private String extractContent(LayoutParsingPageItem pageItem) {
        // 优先取Markdown文本（结构化结果，最适合作为Document内容）
        if (pageItem.getMarkdown() != null && pageItem.getMarkdown().getText() != null) {
            return pageItem.getMarkdown().getText().trim();
        }

        // 若无Markdown，取prunedResult（转为JSON字符串，保留原始解析结果）
        if (pageItem.getPrunedResult() != null) {
            try {
                // 使用Jackson将prunedResult转为JSON字符串（需注入ObjectMapper）
                return new com.fasterxml.jackson.databind.ObjectMapper()
                        .writerWithDefaultPrettyPrinter()
                        .writeValueAsString(pageItem.getPrunedResult());
            } catch (Exception e) {
                return "PrunedResult解析失败：" + e.getMessage();
            }
        }

        // 无任何文本时的默认值
        return "";
    }

    /**
     * 构建单页Document的元数据（存储额外信息，便于后续处理）
     */
    private Map<String, Object> buildMetadata(String fileId, int pageNum) {

        DocumentMetadataMap documentMetadataMap = new DocumentMetadataMap(fileId, ContentType.APPLICATION_PDF.getMimeType(), true, String.valueOf(pageNum));
        documentMetadataMap.setPageRange(String.valueOf(pageNum));
        return documentMetadataMap.toMap();

    }

}