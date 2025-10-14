import React, { useState, useEffect, useRef } from 'react';
import { Loader2, FileText, Image as ImageIcon, FileSpreadsheet, FileCode, FileText as FilePdf, Download, ExternalLink, RefreshCw, ArrowLeft } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import MarkdownEditor from '@uiw/react-markdown-editor';
import { api } from '@/lib/api';
import { cn } from '@/lib/utils';

interface FilePreviewProps {
  documentId: string;
  onBack?: () => void;
}

interface DocumentData {
  id?: string;
  name?: string;
  content?: string;
  contentType: string;
  suffix?: string;
  url: string;
  size?: number;
}

const FilePreview: React.FC<FilePreviewProps> = ({ documentId, onBack }) => {
  const [documentData, setDocumentData] = useState<DocumentData | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [pdfError, setPdfError] = useState(false);
  const [isTextLoaded, setIsTextLoaded] = useState(false);
  const [textContent, setTextContent] = useState('');

  // 获取文件扩展名
  const getFileExtension = (filename?: string) => {
    if (!filename) return '';
    const parts = filename.split('.');
    return parts.length > 1 ? parts[parts.length - 1].toLowerCase() : '';
  };

  // 获取文件图标
  const getFileIcon = () => {
    if (!documentData) return <FileText className="h-6 w-6" />;
    
    const { contentType } = documentData;
    
    if (contentType === 'application/pdf') {
      return <FilePdf className="h-6 w-6 text-red-500" />;
    }
    
    if (contentType.startsWith('image/')) {
      return <ImageIcon className="h-6 w-6 text-blue-500" />;
    }
    
    if (contentType.includes('spreadsheet') || contentType.includes('excel')) {
      return <FileSpreadsheet className="h-6 w-6 text-green-500" />;
    }
    
    if (contentType.includes('text/') || 
        contentType.includes('markdown') || 
        contentType.includes('json') || 
        contentType.includes('xml') || 
        contentType.includes('code')) {
      return <FileCode className="h-6 w-6 text-purple-500" />;
    }
    
    return <FileText className="h-6 w-6" />;
  };

  // 格式化文件大小
  const formatFileSize = (bytes?: number) => {
    if (!bytes) return '未知';
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(2)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
  };

  // 加载文档数据
  const fetchDocumentData = async () => {
    if (!documentId) return;
    
    setLoading(true);
    setError(null);
    setPdfError(false);
    
    try {
      const response = await api.get(`/document/${documentId}`);
      setDocumentData(response);
      
      // 尝试获取文本内容用于纯文本预览
      if (response.contentType.includes('text/') || 
          response.contentType.includes('markdown') || 
          response.contentType.includes('json') ||
          response.contentType.includes('xml') ||
          response.contentType.includes('code')) {
        try {
          const textResponse = await fetch(response.url);
          const text = await textResponse.text();
          setTextContent(text);
          setIsTextLoaded(true);
        } catch (textErr) {
          console.warn('Failed to load text content:', textErr);
        }
      }
    } catch (err) {
      setError('获取文件信息失败');
      console.error('Failed to fetch document data:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (!documentId) return;
    
    fetchDocumentData();
    
    // 重置状态
    setIsTextLoaded(false);
    setTextContent('');
  }, [documentId]);

  // 渲染PDF预览（使用iframe）
  const renderPdfPreview = () => {
    if (!documentData) return null;
    
    const cleanUrl = documentData.url.trim().replace(/^`|`$/g, '');
    
    return (
      <div className="flex flex-col items-center w-full h-full">
        <div className="w-full min-h-[80vh] h-[calc(100vh-180px)] overflow-auto">
          <div className="flex justify-center h-full">
            <iframe
              src={cleanUrl}
              title="PDF Preview"
              className={`w-full h-full border border-muted rounded-md ${pdfError ? 'hidden' : ''}`}
              onLoad={() => setPdfError(false)}
              onError={() => {
                console.error('PDF加载失败');
                setPdfError(true);
              }}
            />
            {pdfError && renderFallbackView()}
          </div>
        </div>
        
        <div className="flex flex-wrap gap-2 mt-4 justify-center">
          <Button
            asChild
            variant="default"
            size="sm"
          >
            <a 
              href={cleanUrl} 
              target="_blank" 
              rel="noopener noreferrer"
              className="flex items-center gap-1"
            >
              <ExternalLink className="h-4 w-4" />
              在新窗口打开
            </a>
          </Button>
          
          <Button
            asChild
            variant="secondary"
            size="sm"
          >
            <a 
              href={cleanUrl} 
              download
              rel="noopener noreferrer"
              className="flex items-center gap-1"
            >
              <Download className="h-4 w-4" />
              下载文件
            </a>
          </Button>
          
          <Button
            variant="ghost"
            size="sm"
            onClick={() => {
              setPdfError(false);
              // 刷新iframe可以通过重新设置src实现
            }}
            className="gap-1"
          >
            <RefreshCw className="h-4 w-4" />
            刷新预览
          </Button>
        </div>
      </div>
    );
  };

  // 渲染图像预览
  const renderImagePreview = () => {
    if (!documentData) return null;
    
    const cleanUrl = documentData.url.trim().replace(/^`|`$/g, '');
    
    return (
      <div className="flex flex-col items-center w-full h-full">
        <div className="w-full h-[calc(100vh-180px)] flex items-center justify-center bg-muted/30 rounded-lg p-4">
          <img 
            src={cleanUrl} 
            alt={documentData.name || '预览图片'} 
            className="max-w-full max-h-full object-contain" 
            loading="lazy"
          />
        </div>
        
        <div className="flex gap-2 mt-4 justify-center">
          <Button
            asChild
            variant="default"
            size="sm"
          >
            <a 
              href={cleanUrl} 
              target="_blank" 
              rel="noopener noreferrer"
              className="flex items-center gap-1"
            >
              <ExternalLink className="h-4 w-4" />
              查看大图
            </a>
          </Button>
          
          <Button
            asChild
            variant="secondary"
            size="sm"
          >
            <a 
              href={cleanUrl} 
              download
              rel="noopener noreferrer"
              className="flex items-center gap-1"
            >
              <Download className="h-4 w-4" />
              下载图片
            </a>
          </Button>
        </div>
      </div>
    );
  };

  // 渲染文本文件预览
  const renderTextPreview = () => {
    if (!documentData) return null;
    
    const cleanUrl = documentData.url.trim().replace(/^`|`$/g, '');
    const isMarkdown = documentData.contentType.includes('markdown') || 
                       getFileExtension(documentData.name) === 'md';
    
    return (
      <div className="flex flex-col items-center w-full h-full">
        <div className="w-full h-[calc(100vh-180px)] overflow-auto border border-muted rounded-lg shadow-sm">
          {isMarkdown ? (
            <MarkdownEditor
              value={isTextLoaded ? textContent : ''}
              onChange={() => {}} // 只读模式
              height="100%"
            />
          ) : (
            <pre className="p-6 bg-muted/30 h-full overflow-auto text-sm whitespace-pre-wrap break-words">
              {isTextLoaded ? textContent : (
                <iframe 
                  src={cleanUrl} 
                  title="Text Preview" 
                  className="w-full h-full border-none"
                  sandbox="allow-same-origin"
                  onLoad={() => setIsTextLoaded(true)}
                />
              )}
            </pre>
          )}
        </div>
        
        <div className="flex gap-2 mt-4 justify-center">
          <Button
            asChild
            variant="default"
            size="sm"
          >
            <a 
              href={cleanUrl} 
              target="_blank" 
              rel="noopener noreferrer"
              className="flex items-center gap-1"
            >
              <ExternalLink className="h-4 w-4" />
              查看原始文件
            </a>
          </Button>
          
          <Button
            asChild
            variant="secondary"
            size="sm"
          >
            <a 
              href={cleanUrl} 
              download
              rel="noopener noreferrer"
              className="flex items-center gap-1"
            >
              <Download className="h-4 w-4" />
              下载文件
            </a>
          </Button>
        </div>
      </div>
    );
  };

  // 渲染不支持的文件类型预览
  const renderFallbackView = () => {
    if (!documentData) return null;
    
    const cleanUrl = documentData.url.trim().replace(/^`|`$/g, '');
    
    return (
      <Card className="w-full max-w-md mx-auto">
        <CardContent className="flex flex-col items-center justify-center text-center p-8">
          <div className="mb-6">
            {getFileIcon()}
          </div>
          <h3 className="text-xl font-medium mb-2">
            {pdfError ? 'PDF预览失败' : '不支持的文件类型'}
          </h3>
          <p className="text-muted-foreground mb-6 max-w-md">
            {pdfError 
              ? '无法在线预览PDF文件，可能是由于浏览器扩展阻止了请求或文件格式不兼容。' 
              : '此文件类型不支持在线预览，请下载后查看'}
          </p>
          <div className="flex flex-wrap gap-2 justify-center">
            <Button
              asChild
              variant="default"
              size="sm"
            >
              <a 
                href={cleanUrl} 
                target="_blank" 
                rel="noopener noreferrer"
                className="flex items-center gap-1"
              >
                <ExternalLink className="h-4 w-4" />
                在新窗口打开
              </a>
            </Button>
            
            <Button
              asChild
              variant="secondary"
              size="sm"
            >
              <a 
                href={cleanUrl} 
                download
                rel="noopener noreferrer"
                className="flex items-center gap-1"
              >
                <Download className="h-4 w-4" />
                下载文件
              </a>
            </Button>
            
            {pdfError && (
              <Button
                variant="ghost"
                size="sm"
                onClick={() => {
                  setPdfError(false);
                }}
                className="gap-1"
              >
                <RefreshCw className="h-4 w-4" />
                重试预览
              </Button>
            )}
          </div>
        </CardContent>
      </Card>
    );
  };

  // 根据文件类型渲染不同的预览内容
  const renderFileContent = () => {
    if (!documentData || !documentData.url) return null;

    const { contentType } = documentData;

    // PDF文件预览
    if (contentType === 'application/pdf') {
      return renderPdfPreview();
    }

    // 图像文件预览
    if (contentType.startsWith('image/')) {
      return renderImagePreview();
    }

    // 文本文件预览
    if (contentType.includes('text/plain') || 
        contentType.includes('markdown') || 
        contentType.includes('json') ||
        contentType.includes('xml') ||
        contentType.includes('code')) {
      return renderTextPreview();
    }

    // 其他Office文档
    if (contentType.includes('word') || contentType.includes('msword') || 
        contentType.includes('excel') || contentType.includes('spreadsheet') || 
        contentType.includes('powerpoint')) {
      return renderFallbackView();
    }

    // 默认处理方式
    return renderFallbackView();
  };

  // PDF加载中组件
  const PdfLoading = () => (
    <div className="flex flex-col items-center justify-center py-16">
      <Loader2 className="h-8 w-8 animate-spin text-primary mb-4" />
      <p className="text-sm text-muted-foreground">正在加载PDF文档...</p>
    </div>
  );

  // 主加载中状态
  if (loading) {
    return (
      <div className="min-h-screen flex flex-col bg-background">
        <div className="flex-1 flex items-center justify-center p-4">
          <div className="flex flex-col items-center">
            <Loader2 className="h-10 w-10 animate-spin text-primary mb-4" />
            <p className="text-sm text-muted-foreground">加载文件内容中...</p>
          </div>
        </div>
      </div>
    );
  }

  // 错误状态
  if (error) {
    return (
      <div className="min-h-screen flex flex-col bg-background">
        <div className="flex-1 flex items-center justify-center p-4">
          <div className="max-w-md text-center">
            <div className="mb-6 text-destructive">
              {getFileIcon()}
            </div>
            <h3 className="text-lg font-medium text-destructive mb-2">{error}</h3>
            <p className="text-muted-foreground mb-6">
              请检查网络连接后重试，或联系系统管理员获取帮助。
            </p>
            <Button
              onClick={fetchDocumentData}
              variant="default"
              className="gap-1"
            >
              <RefreshCw className="h-4 w-4" />
              重试
            </Button>
          </div>
        </div>
      </div>
    );
  }

  // 主界面
  return (
    <div className="min-h-screen flex flex-col bg-background">
      {/* 顶部标题栏 */}
      <header className="sticky top-0 left-0 right-0 z-10 bg-background/95 backdrop-blur-md border-b border-muted px-4 sm:px-6 py-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            {onBack && (
              <Button
                variant="ghost"
                size="icon"
                onClick={onBack}
                className="h-8 w-8"
              >
                <ArrowLeft className="h-4 w-4" />
                <span className="sr-only">返回</span>
              </Button>
            )}
            {getFileIcon()}
            <h2 className="text-lg font-medium truncate max-w-[70vw]">
              {documentData?.name || '文件预览'}
            </h2>
            {documentData?.size && (
              <span className="text-xs text-muted-foreground bg-muted rounded-full px-2 py-0.5">
                {formatFileSize(documentData.size)}
              </span>
            )}
          </div>
        </div>
        {documentData?.contentType && (
          <p className="text-sm text-muted-foreground mt-1">
            文件类型: {documentData.contentType}
          </p>
        )}
      </header>
      
      {/* 主内容区 */}
      <main className="flex-1 w-full p-4">
        {renderFileContent()}
      </main>
    </div>
  );
};

export default FilePreview;