import React, { useState, useRef } from 'react';
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import remarkMath from "remark-math";
import rehypeHighlight from "rehype-highlight";
import rehypeRaw from "rehype-raw";
import rehypeKatex from "rehype-katex";
// 导入katex官方CSS文件
import 'katex/dist/katex.min.css';
// 导入highlight.js样式
import 'highlight.js/styles/github.css';
import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/card";
import { File, Eye, CopyIcon as Copy, CheckCircle as Check } from "lucide-react";
import { cn } from "@/lib/utils";

// 自定义表格组件来确保正确的边框样式
const CustomTable = ({ children, ...props }: any) => (
  <table 
    {...props} 
    className="border border-gray-300 border-collapse w-full"
  >
    {children}
  </table>
);

const CustomThead = ({ children, ...props }: any) => (
  <thead {...props}>
    {children}
  </thead>
);

const CustomTr = ({ children, ...props }: any) => (
  <tr {...props}>
    {children}
  </tr>
);

const CustomTh = ({ children, ...props }: any) => (
  <th 
    {...props} 
    className="border border-gray-300 px-4 py-2 bg-gray-50 font-semibold text-left"
  >
    {children}
  </th>
);

const CustomTd = ({ children, ...props }: any) => (
  <td 
    {...props} 
    className="border border-gray-300 px-4 py-2"
  >
    {children}
  </td>
);

// 简单的代码组件实现，兼容ReactMarkdown
const Code = ({ className, children, ...props }: any) => {
  // 检查是否是代码块（有language-前缀的类名）
  const isCodeBlock = className && className.includes('language-');
  
  if (isCodeBlock) {
    const [copied, setCopied] = useState(false);
    const codeRef = useRef<HTMLPreElement>(null);
    
    // 提取语言
    const language = className.replace(/language-/, '') || 'code';
    
    // 复制代码
    const handleCopy = () => {
      if (codeRef.current) {
        const text = codeRef.current.textContent || '';
        navigator.clipboard.writeText(text);
        setCopied(true);
        setTimeout(() => setCopied(false), 2000);
      }
    };
    
    // 创建行号
    const text = String(children).trim();
    const lines = text.split('\n').length;
    const lineNumbers = Array.from({ length: lines }, (_, i) => i + 1);
    
    return (
      <div className="my-4 rounded-md overflow-hidden border border-gray-200 dark:border-gray-700 shadow-sm">
        {/* 代码块头部 */}
        <div className="bg-gray-50 dark:bg-gray-800 px-4 py-2 flex justify-between items-center">
          <div className="flex items-center space-x-2">
            <div className="w-3 h-3 rounded-full bg-red-500"></div>
            <div className="w-3 h-3 rounded-full bg-yellow-500"></div>
            <div className="w-3 h-3 rounded-full bg-green-500"></div>
            <span className="ml-2 text-xs font-medium text-gray-600 dark:text-gray-300">{language}</span>
          </div>
          <button 
            onClick={handleCopy}
            className="p-1.5 rounded-md text-gray-500 dark:text-gray-400 hover:bg-gray-200 dark:hover:bg-gray-700 transition-colors"
            aria-label="复制代码"
          >
            {copied ? <Check size={16} className="text-green-500" /> : <Copy size={16} />}
          </button>
        </div>
        {/* 代码内容和行号 */}
        <div className="bg-white dark:bg-gray-900 flex">
          {/* 行号列 */}
          <div className="bg-gray-50 dark:bg-gray-800 text-right pr-1.5 pl-1.5 py-1 border-r border-gray-200 dark:border-gray-700 select-none">
            {lineNumbers.map(num => (
              <div key={num} className="text-gray-500 dark:text-gray-400 font-mono leading-none" style={{ fontSize: '14px',padding:'1px' }}>
                {num}
              </div>
            ))}
          </div>
          {/* 代码内容 */}
          <pre 
            ref={codeRef}
            className="flex-1 p-1 overflow-x-auto font-mono text-gray-800 dark:text-gray-300"
            style={{ lineHeight: '1.1', margin: 0, padding: '4px', fontSize: '14px' }}
          >
            <code className={className} style={{ lineHeight: '1.1', margin: 0, padding: 0, fontSize: '14px' }}>{children}</code>
          </pre>
        </div>
      </div>
    );
  }
  
  // 内联代码样式
  return (
    <code 
      className="bg-gray-100 dark:bg-gray-800 px-1.5 py-0.5 rounded-md text-sm font-mono text-gray-800 dark:text-gray-200"
      {...props}
    >
      {children}
    </code>
  );
};

interface Citation {
  id: number;
  text: string;
  metadata: Record<string, any>;
}

interface AnswerProps {
  content: string;
  citations?: Citation[];
  ragReference?: any;
  isStreaming?: boolean;
}

const Answer: React.FC<AnswerProps> = ({ content, citations, ragReference, isStreaming }) => {
  
  // 处理引用信息，合并citations和ragReference
  const getCitations = () => {
    // 首先使用传入的citations
    if (citations && citations.length > 0) {
      return citations;
    }
    
    // 如果没有citations但有ragReference，从ragReference中提取
    if (ragReference && ragReference.reference && Array.isArray(ragReference.reference)) {
      return ragReference.reference.map((ref: any, index: number) => ({
        id: index + 1,
        text: ref.text || '',
        metadata: {
          title: ref.documentName || `引用文档 ${index + 1}`,
          source: ref.contentType || '文档',
          page: ref.pageRange,
          url: ref.documentUrl,
          documentId: ref.documentId
        }
      }));
    }
    
    return [];
  };

  const renderCitations = () => {
    const allCitations = getCitations();
    if (allCitations.length === 0) return null;

    return (
      <div className="mt-4 space-y-3">
        <h4 className="text-sm font-semibold text-muted-foreground flex items-center gap-1.5">
          <File className="h-3.5 w-3.5" />
          引用的文档
        </h4>
        <div className="grid gap-2 sm:grid-cols-1 md:grid-cols-1 lg:grid-cols-1">
          {allCitations.map((citation: Citation) => (
            <Card key={`${citation.id}-${citation.metadata.documentId || Math.random().toString(36).substr(2, 5)}`} className="border border-muted/30 bg-muted/50 hover:border-primary/50 hover:shadow-sm transition-all duration-300 transform hover:-translate-y-0.5">
              <CardHeader className="p-3 pb-0">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <File className="h-4 w-4 text-primary" />
                    <CardTitle className="text-sm font-medium">
                      {citation.metadata.title || `文档 ${citation.id}`}
                    </CardTitle>
                    <button
                      onClick={() => {
                        // 在新窗口打开文件预览
                        if (citation.metadata.documentId) {
                          // 假设文件预览路由为 /file-preview/:documentId
                          const previewUrl = `/file-preview/${citation.metadata.documentId}`;
                          window.open(previewUrl, '_blank');
                        }
                      }}
                      className="text-xs font-medium text-primary/80 hover:text-primary transition-colors flex items-center gap-1"
                      aria-label="查看文件"
                    >
                      <Eye className="h-3 w-3" />
                      查看文件
                    </button>
                  </div>
                  <span className="text-xs font-medium text-primary/80">
                    [引用 {citation.id}]
                  </span>
                </div>
                <div className="text-xs mt-0.5 text-muted-foreground">
                  {citation.metadata.documentId ? `文档ID: ${citation.metadata.documentId}` : ''}
                  {citation.metadata.page ? `，页码: ${citation.metadata.page}` : ''}
                  {citation.metadata.source ? `，类型: ${citation.metadata.source}` : ''}
                </div>
              </CardHeader>
              <CardContent className="p-3 pt-2">
                <p className="text-xs text-muted-foreground max-h-[120px] overflow-y-auto bg-background/30 p-2 rounded-md">
                  {citation.text}
                </p>
              </CardContent>
            </Card>
          ))}
        </div>
      </div>
    );
  };

  return (
      <div className={cn("space-y-2", isStreaming && "animate-pulse")}>
        {/* 为数学公式添加局部样式容器 */}
        <div className="prose prose-sm max-w-none text-accent-foreground prose-headings:font-medium prose-headings:text-base prose-p:my-2 prose-li:my-1 prose-code:bg-muted/50 prose-code:px-1.5 prose-code:py-0.5 prose-code:rounded prose-code:text-xs prose-ol:pl-5 prose-ul:pl-5 break-words">
            <ReactMarkdown
              remarkPlugins={[remarkGfm, remarkMath]}
                rehypePlugins={[rehypeRaw, rehypeHighlight, rehypeKatex]}
                components={{
                  table: CustomTable,
                  thead: CustomThead,
                  tr: CustomTr,
                  th: CustomTh,
                  td: CustomTd,
                  code: Code
                }}
            >
              {content}
            </ReactMarkdown>
        </div>
        {renderCitations()}
      </div>
    );
  };

export default Answer;