package com.gengzi.ui.service;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.util.List;

public interface EvaluateService {


    void evaluateGenerate(List<String> documentIds, String batchNum) throws IOException;


    void evaluate(String coonversationId, String batchNum);

    void evaluateCalculate(String batchNum);

    void evaluateStatistics(String batchNum);

    /**
     * 获取统计折线图信息
     *
     * @return
     */
    List<?> evaluateStatisticsLineChart();

    Page<?> evaluateStatisticsByBatchNum(String batchNum, Pageable pageable);


    List<?> evaluateStatisticsBatchNums();
}
