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
E:\ruanjian\minio.exe server  F:\sso   --address ":8886"  --console-address ":9006""
access-key: rag_flow
secret-key: infini_rag_flow
```
