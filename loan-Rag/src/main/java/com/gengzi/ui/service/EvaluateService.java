package com.gengzi.ui.service;


import java.io.IOException;
import java.util.List;

public interface EvaluateService {


    void evaluateGenerate(List<String> documentIds, String batchNum) throws IOException;


    void evaluate(String coonversationId, String batchNum);

    void evaluateCalculate(String batchNum);

    void evaluateStatistics(String batchNum);
}
