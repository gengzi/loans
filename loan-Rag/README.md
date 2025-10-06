## Rag by Spring ai

### Rag流程

### 步骤


 


### 问题
宿主机运行 ollama 运行容器，以docker 运行的ragflow 配置请求地址时，需要配置 http://host.docker.internal:11434/ 才能访问到






* pdf ocr 识别
```angular2html
## 使用paddlex  将paddleocr 服务化，提供服务化接口提供使用 （https://www.paddleocr.ai/latest/version3.x/deployment/serving.html）
paddlex --serve --pipeline  PP-StructureV3   --port 8885

PP-StructureV3  https://www.paddleocr.ai/latest/version3.x/pipeline_usage/PP-StructureV3.html 使用指南
paddlex --serve --pipeline  /home/gengzi/PP-StructureV3.yaml   --port 8885

## 启动命令
进入linux服务
conda env list
conda activate env-vllm

## 启动minio
E:\ruanjian\minio.exe server  F:\sso   --address ":8886"  --console-address ":9006"
access-key: rag_flow
secret-key: infini_rag_flow
```


## es 映射配置
```
{
    "mappings": {
        "properties": {
            "content": {
                "type": "text",
                "fields": {
                    "keyword": {
                        "type": "keyword",
                        "ignore_above": 256
                    }
                }
            },
            "id": {
                "type": "text",
                "fields": {
                    "keyword": {
                        "type": "keyword",
                        "ignore_above": 256
                    }
                }
            },
            "metadata": {
                "properties": {
                    "convertedTimestamp": {
                        "type": "long"
                    },
                    "documentType": {
                        "type": "text",
                        "fields": {
                            "keyword": {
                                "type": "keyword",
                                "ignore_above": 256
                            }
                        }
                    },
                    "hasInputImage": {
                        "type": "boolean"
                    },
                    "inputImageBase64": {
                        "type": "text",
                        "fields": {
                            "keyword": {
                                "type": "keyword",
                                "ignore_above": 256
                            }
                        }
                    },
                    "isParagraphEnd": {
                        "type": "boolean"
                    },
                    "isParagraphStart": {
                        "type": "boolean"
                    },
                    "outputImageCount": {
                        "type": "long"
                    },
                    "outputImageNames": {
                        "type": "text",
                        "fields": {
                            "keyword": {
                                "type": "keyword",
                                "ignore_above": 256
                            }
                        }
                    },
                    "pageNumber": {
                        "type": "long"
                    },
                    "requestLogId": {
                        "type": "text",
                        "fields": {
                            "keyword": {
                                "type": "keyword",
                                "ignore_above": 256
                            }
                        }
                    },
                    "sourceFileUrl": {
                        "type": "text",
                        "copy_to": "content_tokens",  
                        "fields": {
                            "keyword": {
                                "type": "keyword",
                                "ignore_above": 256
                            }
                        }
                    }
                }
            },
         "content_tokens": {"type": "text", "analyzer": "ik_smart"},
            "q_1024_vec": {
                "type": "dense_vector",
                "dims": 1024,
                "index": true,
                "similarity": "cosine",
                "index_options": {
                    "type": "bbq_hnsw",
                    "m": 16,
                    "ef_construction": 100,
                    "rescore_vector": {
                        "oversample": 3
                    }
                }
            }
        }
    }
}
```
--

```angular2html

```