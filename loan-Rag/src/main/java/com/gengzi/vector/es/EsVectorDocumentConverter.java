package com.gengzi.vector.es;


import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import com.gengzi.context.DocumentMetadataMap;
import com.gengzi.context.FileContext;
import com.gengzi.utils.FileIdGenerator;
import com.gengzi.utils.HanLPUtil;
import com.gengzi.utils.InstantConverter;
import com.gengzi.vector.es.document.ExtendedDocument;
import org.springframework.ai.document.Document;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 将Document 转换为 EsVectorDocument
 */
public class EsVectorDocumentConverter {


    public static List<Document> convert(List<Document> documents, FileContext fileContext) {

        if (documents == null && documents.isEmpty()) {
            throw new IllegalArgumentException("documents cannot be null or empty");
        }
        String fileId = FileIdGenerator.generateFileId(fileContext.getKey());
        ArrayList<Document> esVectorDocuments = new ArrayList<>(documents.size());
        for (int chunkNumber = 0; chunkNumber < documents.size(); chunkNumber++) {
            // 生成每个文本块（chunkid）的id  （docid+"_"+chunknumber）

            Document document = documents.get(chunkNumber);
            EsVectorDocument esVectorDocument = new EsVectorDocument();
            esVectorDocument.setChunkId(String.format("%s_%d", fileId, chunkNumber));
            esVectorDocument.setContent(document.getText());
            esVectorDocument.setMetadata(document.getMetadata());
            esVectorDocument.setDocId(fileId);
            esVectorDocument.setAvailableInt(1);
            esVectorDocument.setCreateTime(InstantConverter.instantToString(fileContext.getLastModified(), ZoneId.systemDefault()));
            esVectorDocument.setCreateTimestampFlt(fileContext.getLastModified().toEpochMilli());
            // TODO 这里不正确
            esVectorDocument.setPageNumInt((String) document.getMetadata().get(DocumentMetadataMap.PAGE_RANGE));
            esVectorDocument.setImgId(String.format("%s_%d", fileId, chunkNumber));
            // TODO 分词需要移除各种格式
            esVectorDocument.setContentLtks(HanLPUtil.nShortSegment(document.getText()).stream().collect(Collectors.joining(" ")));
            esVectorDocument.setContentSmLtks(HanLPUtil.segment(document.getText()).stream().collect(Collectors.joining(" ")));
            esVectorDocument.setTitleSmTks(HanLPUtil.segment(fileContext.getFileName()).stream().collect(Collectors.joining(" ")));
            esVectorDocument.setTitleTks(HanLPUtil.nShortSegment(fileContext.getFileName()).stream().collect(Collectors.joining(" ")));

            ExtendedDocument extendedDocument = new ExtendedDocument(esVectorDocument);

            esVectorDocuments.add(extendedDocument);
        }
        return esVectorDocuments;
    }


}
