"use client";

import { useState } from "react";
import { FileText, Share2, Download, Info } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";

interface DocumentPreviewProps {
  previewData: {
    url: string;
    contentType: string;
  } | null;
  fileName: string;
  filteredChunks?: Array<{
    id: string;
    content: string;
    index: number;
    metadata: {
      pageNumber?: number;
      pageNumbers?: number[];
      chunkNumber?: number;
      startLine?: number;
      endLine?: number;
      section?: string;
      [key: string]: any;
    };
  }>;
}

const DocumentPreview = ({ previewData, fileName, filteredChunks = [] }: DocumentPreviewProps) => {
  // PDF预览的高度计算函数
  const getPdfHeight = () => {
    // 确保在客户端环境中运行
    if (typeof window !== 'undefined') {
      return `calc(100vh - 320px)`;
    }
    return '500px'; // 默认值
  };

  if (!previewData) {
    return (
      <div className="w-full h-full flex items-center justify-center">
        <div className="text-center">
          <Info className="h-12 w-12 mx-auto text-muted-foreground mb-4" />
          <h3 className="text-lg font-medium mb-2">无法预览文档</h3>
          <p className="text-muted-foreground">文档预览数据暂不可用</p>
        </div>
      </div>
    );
  }

  return (
    <div className="w-full h-full">
      {/* PDF内容预览 */}
      {previewData.contentType === 'application/pdf' && (
        <div className="w-full h-full">
          <div className="bg-white h-full w-full rounded-lg shadow-sm overflow-hidden flex flex-col">
            {/* 标题栏 */}
            <div className="px-4 py-2 border-b bg-muted/10 flex justify-between items-center">
              <div className="text-lg font-medium">PDF 文档预览</div>
            </div>
            {/* 使用iframe加载PDF预览URL */}
            <iframe 
              src={previewData.url}
              className="w-full h-[calc(100vh-320px)]"
              title="PDF 文档预览"
              frameBorder="0"
            />
          </div>
        </div>
      )}
      
      {/* Word文档预览 */}
      {['application/msword', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'].includes(previewData.contentType) && (
        <div className="w-full h-full">
          <div className="bg-white h-full w-full rounded-lg shadow-sm overflow-hidden flex flex-col">
            {/* 标题栏 */}
            <div className="px-4 py-2 border-b bg-muted/10 flex justify-between items-center">
              <div className="text-lg font-medium">Word 文档预览</div>
            </div>
            {/* Word预览区域 */}
            <div className="flex-1 flex items-center justify-center p-6">
              <div className="text-center max-w-md">
                <FileText className="h-16 w-16 mx-auto text-blue-500 mb-4" />
                <h4 className="font-medium mb-2">Word 文档</h4>
                <p className="text-sm text-muted-foreground mb-6">
                  由于浏览器安全限制，Word文档需要在新窗口打开预览
                </p>
                <Button 
                  variant="default"
                  onClick={() => window.open(previewData.url, '_blank')}
                >
                  <Share2 className="h-4 w-4 mr-2" />
                  在新窗口打开预览
                </Button>
              </div>
            </div>
          </div>
        </div>
      )}
      
      {/* Excel文档预览 */}
      {['application/vnd.ms-excel', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'].includes(previewData.contentType) && (
        <div className="w-full h-full">
          <div className="bg-white h-full w-full rounded-lg shadow-sm overflow-hidden flex flex-col">
            {/* 标题栏 */}
            <div className="px-4 py-2 border-b bg-muted/10 flex justify-between items-center">
              <div className="text-lg font-medium">Excel 文档预览</div>
            </div>
            {/* Excel预览区域 */}
            <div className="flex-1 flex items-center justify-center p-6">
              <div className="text-center max-w-md">
                <FileText className="h-16 w-16 mx-auto text-green-500 mb-4" />
                <h4 className="font-medium mb-2">Excel 文档</h4>
                <p className="text-sm text-muted-foreground mb-6">
                  由于浏览器安全限制，Excel文档需要在新窗口打开预览
                </p>
                <Button 
                  variant="default"
                  onClick={() => window.open(previewData.url, '_blank')}
                >
                  <Share2 className="h-4 w-4 mr-2" />
                  在新窗口打开预览
                </Button>
              </div>
            </div>
          </div>
        </div>
      )}
      
      {/* PowerPoint文档预览 */}
      {['application/vnd.ms-powerpoint', 'application/vnd.openxmlformats-officedocument.presentationml.presentation'].includes(previewData.contentType) && (
        <div className="w-full h-full">
          <div className="bg-white h-full w-full rounded-lg shadow-sm overflow-hidden flex flex-col">
            {/* 标题栏 */}
            <div className="px-4 py-2 border-b bg-muted/10 flex justify-between items-center">
              <div className="text-lg font-medium">PowerPoint 文档预览</div>
            </div>
            {/* PowerPoint预览区域 */}
            <div className="flex-1 flex items-center justify-center p-6">
              <div className="text-center max-w-md">
                <FileText className="h-16 w-16 mx-auto text-orange-500 mb-4" />
                <h4 className="font-medium mb-2">PowerPoint 文档</h4>
                <p className="text-sm text-muted-foreground mb-6">
                  由于浏览器安全限制，PowerPoint文档需要在新窗口打开预览
                </p>
                <Button 
                  variant="default"
                  onClick={() => window.open(previewData.url, '_blank')}
                >
                  <Share2 className="h-4 w-4 mr-2" />
                  在新窗口打开预览
                </Button>
              </div>
            </div>
          </div>
        </div>
      )}
      
      {/* 图片预览 */}
      {previewData.contentType.startsWith('image/') && (
        <div className="w-full h-full">
          <div className="bg-white h-full w-full rounded-lg shadow-sm overflow-hidden flex flex-col">
            {/* 标题栏 */}
            <div className="px-4 py-2 border-b bg-muted/10 flex justify-between items-center">
              <div className="text-lg font-medium">图片预览</div>
            </div>
            {/* 图片预览区域 */}
            <div className="flex-1 flex items-center justify-center p-4">
              <img 
                src={previewData.url} 
                alt="文档预览" 
                className="max-w-full max-h-full object-contain"
                onClick={() => window.open(previewData.url, '_blank')}
                style={{ cursor: 'pointer' }}
              />
            </div>
            <div className="px-4 py-2 border-t text-xs text-muted-foreground text-center">
              点击图片查看大图
            </div>
          </div>
        </div>
      )}
      
      {/* Markdown内容预览 */}
      {previewData.contentType === 'text/markdown' && (
        <div className="w-full h-full">
          <div className="bg-white h-full w-full rounded-lg shadow-sm overflow-hidden flex flex-col">
            {/* 标题栏 */}
            <div className="px-4 py-2 border-b bg-muted/10 flex justify-between items-center">
              <div className="text-lg font-medium">Markdown 文档预览</div>
            </div>
            {/* 内容区域 */}
            <div className="flex-1 overflow-auto p-4">
              <div className="whitespace-pre-wrap leading-relaxed">
                {filteredChunks.map((chunk, index) => (
                  <div key={chunk.id} className={`mb-6 ${index > 0 ? 'border-t pt-6' : ''}`}>
                    <div className="whitespace-pre-wrap">
                      {chunk.content}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      )}
      
      {/* 文本预览 */}
      {previewData.contentType.startsWith('text/') && (
        <div className="w-full h-full">
          <div className="bg-white h-full w-full rounded-lg shadow-sm overflow-hidden flex flex-col">
            {/* 标题栏 */}
            <div className="px-4 py-2 border-b bg-muted/10 flex justify-between items-center">
              <div className="text-lg font-medium">文本预览</div>
            </div>
            {/* 内容区域 */}
            <div className="flex-1 overflow-auto p-4">
              <div className="whitespace-pre-wrap leading-relaxed font-mono text-sm">
                {filteredChunks.map((chunk, index) => (
                  <div key={chunk.id} className={`mb-4 ${index > 0 ? 'border-t pt-4' : ''}`}>
                    <div className="whitespace-pre-wrap">
                      {chunk.content}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      )}
      
      {/* 其他文件类型的预览 */}
      {!['application/pdf', 'text/markdown'].includes(previewData.contentType) && 
       !previewData.contentType.startsWith('image/') && 
       !previewData.contentType.startsWith('text/') &&
       !['application/msword', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
         'application/vnd.ms-excel', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
         'application/vnd.ms-powerpoint', 'application/vnd.openxmlformats-officedocument.presentationml.presentation'].includes(previewData.contentType) && (
        <div className="w-full h-full">
          <div className="bg-white h-full w-full rounded-lg shadow-sm overflow-hidden flex flex-col">
            {/* 标题栏 */}
            <div className="px-4 py-2 border-b bg-muted/10 flex justify-between items-center">
              <div className="text-lg font-medium">文档预览</div>
            </div>
            {/* 内容区域 */}
            <div className="flex-1 flex flex-col justify-center items-center p-6">
              <div className="text-center mb-4">
                <Badge variant="outline">{previewData.contentType}</Badge>
              </div>
              <FileText className="h-16 w-16 mx-auto text-muted-foreground mb-4" />
              <div className="text-sm text-muted-foreground mb-6 max-w-lg text-center">
                此文件类型无法在浏览器中直接预览
              </div>
              <div className="flex gap-4">
                <Button 
                  variant="default" 
                  onClick={() => window.open(previewData.url, '_blank')}
                >
                  <Share2 className="h-4 w-4 mr-2" />
                  在新窗口打开
                </Button>
                <Button 
                  variant="default" 
                  onClick={() => window.open(previewData.url, '_self')}
                >
                  <Download className="h-4 w-4 mr-2" />
                  下载文件
                </Button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default DocumentPreview;