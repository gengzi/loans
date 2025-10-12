package com.gengzi.request;


import lombok.Data;

@Data
public class AddDocumentByS3 {

    /**
     * 存储桶名称
     */
    private String bucket;

    /**
     * “目录”
     */
    private String key;

    private String kbId;

}
