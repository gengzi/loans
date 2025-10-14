import React from "react";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import rehypeHighlight from "rehype-highlight";
import rehypeRaw from "rehype-raw";
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from "@/components/ui/card";
import { File } from "lucide-react";
import { cn } from "@/lib/utils";
import { formatDocumentUrl } from '@/lib/utils';

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
                    <a
                      href={formatDocumentUrl(citation.metadata.url)}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="text-xs font-medium text-primary/80"
                    >
                      查看文件
                    </a>
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
                <p className="text-xs text-muted-foreground line-clamp-3 bg-background/30 p-2 rounded-md">
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
      <div className="prose prose-sm max-w-none text-accent-foreground prose-headings:font-medium prose-headings:text-base prose-p:my-2 prose-li:my-1 prose-code:bg-muted/50 prose-code:px-1.5 prose-code:py-0.5 prose-code:rounded prose-code:text-xs prose-ol:pl-5 prose-ul:pl-5">
        <ReactMarkdown
          remarkPlugins={[remarkGfm]}
          rehypePlugins={[rehypeRaw, rehypeHighlight]}
        >
          {content}
        </ReactMarkdown>
      </div>
      {renderCitations()}
    </div>
  );
};

export default Answer;