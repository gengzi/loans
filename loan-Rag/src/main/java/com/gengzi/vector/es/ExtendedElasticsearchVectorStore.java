package com.gengzi.vector.es;

import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStore;

public class ExtendedElasticsearchVectorStore extends ElasticsearchVectorStore {
    protected ExtendedElasticsearchVectorStore(Builder builder) {
        super(builder);
    }


}
