package com.gengzi.search;


import com.gengzi.embedding.load.PyPdfReader;
import com.gengzi.response.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class DocumentController {


    @Autowired
    private PyPdfReader pyPdfReader;


    @PostMapping("/document/upload")
    public String document() {
        return null;
    }

    @PostMapping("/document/embedding")
    public Result<Void> documentToEmbedding(@RequestParam String filePath) {
        pyPdfReader.pdfParse(filePath);
        return Result.success("等待解析完成");
    }
}
