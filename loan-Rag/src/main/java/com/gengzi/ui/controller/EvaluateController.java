package com.gengzi.ui.controller;


import com.gengzi.ui.service.EvaluateService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

/**
 * 评估rag系统
 */
@RestController
@Tag(name = "rag评估", description = "rag评估")
public class EvaluateController {


    @Autowired
    private EvaluateService evaluateService;


    /**
     * 通过llm生成评估集数据+人工修正
     */
    @GetMapping("/evaluate/generate")
    public void evaluateGenerate(@RequestParam(value = "documentIds", required = false) List<String> documentIds,
                                 @RequestParam(value = "batchNum") String batchNum) throws IOException {
        evaluateService.evaluateGenerate(documentIds, batchNum);
    }


    /**
     * 评估训练集和真实回答
     */
    @GetMapping("/evaluate")
    public void evaluate(@RequestParam(value = "coonversationId") String coonversationId ,
                         @RequestParam(value = "batchNum") String batchNum) {
        evaluateService.evaluate(coonversationId,batchNum);
    }


    /**
     * 基于数据库训练集数据，进行指标的计算
     */
    @GetMapping("/evaluate/calculate")
    public void evaluateCalculate(@RequestParam(value = "batchNum") String batchNum) {
        evaluateService.evaluateCalculate(batchNum);
    }

    /**
     * 统计评估结果
     */
    @GetMapping("/evaluate/statistics")
    public void evaluateStatistics(@RequestParam(value = "batchNum") String batchNum) {
        evaluateService.evaluateStatistics(batchNum);
    }


}
