package com.gengzi.search;


import com.gengzi.embedding.load.pdf.OcrPdfReader;
import com.gengzi.embedding.load.pdf.PdfReaderTool;
import com.gengzi.response.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DocumentController {


    @Autowired
    private OcrPdfReader pyPdfReader;

    @Autowired
    private PdfReaderTool pdfReaderTool;


    @PostMapping("/document/upload")
    public String document() {
        return null;
    }

    @PostMapping("/document/embedding")
    public Result<Void> documentToEmbedding(@RequestParam String filePath) {
//        pdfReaderTool.pdfReader(filePath);
        pyPdfReader.pdfParse(filePath);
        return Result.success("等待解析完成");
    }
}
