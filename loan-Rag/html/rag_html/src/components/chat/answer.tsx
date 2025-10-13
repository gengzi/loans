import React, {
  FC,
  useMemo,
  useEffect,
  useState,
  ClassAttributes,
} from "react";
import { AnchorHTMLAttributes } from "react";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { Skeleton } from "@/components/ui/skeleton";
import { Divider } from "@/components/ui/divider";
import Markdown from "react-markdown";
import remarkGfm from "remark-gfm";
import rehypeHighlight from "rehype-highlight";
import { api } from "@/lib/api";
import { FileIcon } from "react-file-icon";

interface Citation {
  id: number;
  text: string;
  metadata: Record<string, any>;
}

interface KnowledgeBaseInfo {
  name: string;
}

interface DocumentInfo {
  file_name: string;
  knowledge_base: KnowledgeBaseInfo;
}

interface CitationInfo {
  knowledge_base: KnowledgeBaseInfo;
  document: DocumentInfo;
}

export const Answer: FC<{
  markdown: string;
  citations?: Citation[];
}> = ({ markdown, citations = [] }) => {
  const [citationInfoMap, setCitationInfoMap] = useState<
    Record<string, CitationInfo>
  >({});

  const processedMarkdown = useMemo(() => {
    return markdown
      .replace(/<think>/g, "## 💭 深度思考\n```think")
      .replace(/<\/think>/g, "```");
  }, [markdown]);

  // 直接从citations的metadata中获取信息，不再请求额外接口
  useEffect(() => {
    const infoMap: Record<string, CitationInfo> = {};

    for (const citation of citations) {
      const { kb_id, document_id, file_name, knowledge_base_name } = citation.metadata;
      if (!kb_id || !document_id) continue;

      const key = `${kb_id}-${document_id}`;
      if (infoMap[key]) continue;

      // 直接从metadata中获取信息，或者使用默认值
      const kbName = knowledge_base_name || `知识库 ${kb_id}`;
      const fileName = file_name || `文档 ${document_id}`;

      infoMap[key] = {
        knowledge_base: {
          name: kbName,
        },
        document: {
          file_name: fileName,
          knowledge_base: {
            name: kbName,
          },
        },
      };
    }

    setCitationInfoMap(infoMap);
  }, [citations]);

  const CitationLink = useMemo(
    () =>
      (
        props: ClassAttributes<HTMLAnchorElement> &
          AnchorHTMLAttributes<HTMLAnchorElement>
      ) => {
        const citationId = props.href?.match(/^(\d+)$/)?.[1];
        const citation = citationId
          ? citations[parseInt(citationId) - 1]
          : null;

        if (!citation) {
          // 找不到引用时，不显示任何引用标记
          return null;
        }

        const citationInfo =
          citationInfoMap[
            `${citation.metadata.kb_id}-${citation.metadata.document_id}`
          ];

        return (
          <Popover>
            <PopoverTrigger asChild>
              <a
                {...props}
                href="#"
                role="button"
                className="inline-flex items-center gap-1 px-1.5 py-0.5 text-xs font-medium text-blue-600 bg-blue-50 rounded hover:bg-blue-100 transition-colors relative"
              >
                <span className="absolute -top-3 -right-1">[{props.href}]</span>
              </a>
            </PopoverTrigger>
            <PopoverContent
              side="top"
              align="start"
              className="max-w-2xl w-[calc(100vw-100px)] p-4 rounded-lg shadow-lg"
            >
              <div className="text-sm space-y-3">
                {citationInfo && (
                  <div className="flex items-center gap-2 text-xs font-medium text-gray-700 bg-gray-50 p-2 rounded">
                    <div className="w-5 h-5 flex items-center justify-center">
                      <FileIcon
                        extension={
                          citationInfo.document.file_name.split(".").pop() || ""
                        }
                        color="#E2E8F0"
                        labelColor="#94A3B8"
                      />
                    </div>
                    <span className="truncate">
                      {citationInfo.knowledge_base.name} /{" "}
                      {citationInfo.document.file_name}
                    </span>
                  </div>
                )}
                <Divider />
                <p className="text-gray-700 leading-relaxed">{citation.text}</p>

              </div>
            </PopoverContent>
          </Popover>
        );
      },
    [citations, citationInfoMap]
  );

  if (!markdown) {
    return (
      <div className="flex flex-col gap-2">
        <Skeleton className="max-w-sm h-4 bg-zinc-200" />
        <Skeleton className="max-w-lg h-4 bg-zinc-200" />
        <Skeleton className="max-w-2xl h-4 bg-zinc-200" />
        <Skeleton className="max-w-lg h-4 bg-zinc-200" />
        <Skeleton className="max-w-xl h-4 bg-zinc-200" />
      </div>
    );
  }

  return (
    <div className="prose prose-sm max-w-full">
      <Markdown
        remarkPlugins={[remarkGfm]}
        rehypePlugins={[rehypeHighlight]}
        components={{
          a: CitationLink,
        }}
      >
        {processedMarkdown}
      </Markdown>
    </div>
  );
};
