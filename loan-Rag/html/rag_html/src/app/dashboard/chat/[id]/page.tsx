"use client";

import { useEffect, useRef, useMemo } from "react";
import { useRouter } from "next/navigation";
import { useChat } from "ai/react";
import { Send, User, Bot } from "lucide-react";
import DashboardLayout from "@/components/layout/dashboard-layout";
import { api, ApiError } from "@/lib/api";
import { useToast } from "@/components/ui/use-toast";
import { Answer } from "@/components/chat/answer";

interface Message {
  id: string;
  role: "assistant" | "user" | "system" | "data";
  content: string;
  citations?: Citation[];
}

interface ChatMessage {
  id: string;
  content: string;
  role: "assistant" | "user";
  created_at: string;
}

interface Chat {
  id: string;
  name: string;
  createDate: string;
  knowledgebaseId: string;
  dialogId: string;
  messages: ChatMessage[];
}

interface Citation {
  id: number;
  text: string;
  metadata: Record<string, any>;
}

// Extend the default useChat message type
declare module "ai/react" {
  interface Message {
    citations?: Citation[];
  }
}

export default function ChatPage({ params }: { params: { id: string } }) {
  const router = useRouter();
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);
  const { toast } = useToast();

  const {
    messages,
    data,
    input,
    handleInputChange,
    isLoading,
    setMessages,
    setInput
  } = useChat({
    // 禁用默认的API调用，我们将使用自定义的提交函数
    api: undefined,
    headers: {
      Authorization: `Bearer ${
        typeof window !== "undefined"
          ? window.localStorage.getItem("token")
          : ""
      }`,
    },
  });

  // 自定义提交函数，使用新的API接口发送消息
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!input.trim()) return;
    
    // 创建当前消息对象
    const currentMessage = {
      id: Date.now().toString(),
      role: 'user' as const,
      content: input.trim(),
    };
    
    // 先在前端显示用户消息
    setMessages([...messages, currentMessage]);
    
    try {
      // 使用新的API接口发送消息
      // 格式：POST /chat/rag
      // 请求体：{ "question": "string", "conversationId": "string" }
      const response = await api.post(`/chat/rag`, {
        question: input.trim(),
        conversationId: params.id
      });
      
      // 根据新API的响应格式处理助手消息
      // 假设响应格式与之前的fetchChat函数处理的格式类似
      const data = response.success ? response.data : response;
      
      // 解析嵌套的JSON字符串消息
      let assistantMessage = null;
      if (data.message) {
        try {
          // message字段是JSON字符串，需要解析
          const parsedMessages = JSON.parse(data.message);
          // 假设最后一条消息是助手的回复
          if (parsedMessages.length > 0) {
            const lastMsg = parsedMessages[parsedMessages.length - 1];
            if (lastMsg.role === 'ASSISTANT') {
              assistantMessage = {
                id: lastMsg.id || Date.now().toString() + '-assistant',
                role: 'assistant' as const,
                content: lastMsg.content || '',
                citations: []
              };
            }
          }
        } catch (e) {
          console.error("Failed to parse assistant message:", e);
        }
      }
      
      // 添加助手消息到消息列表
      if (assistantMessage) {
        setMessages(prev => [...prev, assistantMessage]);
      }
      
      // 处理引用信息
      let references = [];
      if (data.reference) {
        try {
          // reference字段是JSON字符串，需要解析
          references = JSON.parse(data.reference);
        } catch (e) {
          console.error("Failed to parse reference JSON string:", e);
          references = [];
        }
      }
      
      // 只有当确实有有效的引用信息时，才为助手消息添加引用
      if (assistantMessage && references.length > 0) {
        // 从parsedMessages中找到对应助手消息的索引
        let assistantMessageIndex = -1;
        if (data.message) {
          try {
            const parsedMessages = JSON.parse(data.message);
            // 从后往前找，找到最后一条助手消息的位置
            for (let i = parsedMessages.length - 1; i >= 0; i--) {
              if (parsedMessages[i].role === 'ASSISTANT') {
                assistantMessageIndex = i;
                break;
              }
            }
          } catch (e) {
            console.error("Failed to parse message for reference matching:", e);
          }
        }
        
        // 如果找到了匹配的索引，使用该索引对应的引用；否则使用最后一个引用
        const referenceToUse = assistantMessageIndex >= 0 && assistantMessageIndex < references.length 
          ? references[assistantMessageIndex]
          : references[references.length - 1];
        
        // 确保referenceToUse和documents存在且不为空
        if (!referenceToUse?.documents || referenceToUse.documents.length === 0) {
          console.log("No valid documents found in reference");
          return;
        }
        
        // 处理文档引用
        const citations = referenceToUse.documents.map((doc, docIndex) => ({
          id: docIndex + 1,
          text: doc.text,
          // 确保metadata包含Answer组件需要的字段
          metadata: {
            ...doc.metadata,
            // 使用documentId作为document_id
            document_id: doc.id,
            // 使用knowledgebaseId作为kb_id
            kb_id: data.knowledgebaseId,
            // 添加知识库名称（如果有）或使用知识库ID作为名称
            knowledge_base_name: data.knowledgebaseName || `知识库 ${data.knowledgebaseId}`,
            // 添加文件名（如果有doc.name）或使用文档ID作为文件名
            file_name: doc.name || `文档 ${doc.id}`
          }
        }));
        
        // 为助手消息添加引用标记，以便在UI中显示引用信息
          let contentWithCitations = assistantMessage.content || '';
          if (citations && citations.length > 0) {
            // 只有当确实有引用时才添加引用标记
            const citationMarkers = citations.map(citation => `[citation](${citation.id})`).join(', ');
            contentWithCitations += `\n\n参考资料来源：${citationMarkers}`;
          }
        
        // 更新助手消息
        assistantMessage = {
          ...assistantMessage,
          content: contentWithCitations,
          citations: citations || []
        };
        
        // 重新设置消息列表，确保使用更新后的助手消息
        setMessages(prev => {
          // 找到最后一条消息（助手消息）并更新它
          const newMessages = [...prev];
          if (newMessages.length > 0) {
            newMessages[newMessages.length - 1] = assistantMessage;
          }
          return newMessages;
        });
      }
      
    } catch (error) {
      console.error('Failed to send message:', error);
      toast({
        title: "Error",
        description: "发送消息失败",
        variant: "destructive",
      });
    } finally {
      // 使用setInput清空输入框
      setInput('');
      // 同时通过ref直接清空输入框元素，确保输入框被清空
      if (inputRef.current) {
        inputRef.current.value = '';
      }
    }
  };

  // 使用useRef和useEffect确保在React 18严格模式下只调用一次fetchChat
  const hasFetchedChat = useRef(false);
  
  useEffect(() => {
    if (!hasFetchedChat.current) {
      hasFetchedChat.current = true;
      fetchChat();
    }
  }, []);

  // 消息更新时滚动到底部
  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  // 调用指定的API接口加载聊天记录
  const fetchChat = async () => {
    try {
      console.log(`Fetching chat data from API for chat ID: ${params.id}`);
      
      // 使用全局API配置调用接口
      const response = await api.get("/chat/rag/msg/list", {
        params: {
          conversationId: params.id
        }
      });
      
      // 解析API返回的数据
      // 注意：这里根据用户提供的实际数据结构进行了调整
      const data = response.success ? response.data : response;
      
      // 解析嵌套的JSON字符串消息
      let chatMessages = [];
      if (data.message) {
        try {
          // message字段是JSON字符串，需要解析
          chatMessages = JSON.parse(data.message);
        } catch (e) {
          console.error("Failed to parse message JSON string:", e);
          chatMessages = [];
        }
      }
      
      // 解析嵌套的JSON字符串引用
      let references = [];
      if (data.reference) {
        try {
          // reference字段是JSON字符串，需要解析
          references = JSON.parse(data.reference);
        } catch (e) {
          console.error("Failed to parse reference JSON string:", e);
          references = [];
        }
      }
      
      // 转换为应用需要的Message格式
      const messagesToSet = chatMessages.map((msg, index) => {
        // 为助手消息添加引用
        if (msg.role === 'ASSISTANT' && references.length > 0) {
          // 对于历史消息，我们假设references数组中的索引与chatMessages数组中的索引相对应
          // 但要确保索引不越界
          const referenceToUse = index < references.length 
            ? references[index]
            : references.find(ref => ref?.documents && ref.documents.length > 0) || null;
          
          // 确保referenceToUse和documents存在且不为空
          if (!referenceToUse?.documents || referenceToUse.documents.length === 0) {
            // 如果没有有效的引用文档，直接返回消息，不添加引用
            return {
              id: msg.id || `msg-${index}`,
              role: 'assistant',
              content: msg.content || '',
              citations: []
            };
          }
          
          // 处理文档引用
          const citations = referenceToUse.documents.map((doc, docIndex) => ({
            id: docIndex + 1,
            text: doc.text,
            // 确保metadata包含Answer组件需要的字段
            metadata: {
              ...doc.metadata,
              // 使用documentId作为document_id
              document_id: doc.id,
              // 使用knowledgebaseId作为kb_id
              kb_id: data.knowledgebaseId,
              // 添加知识库名称（如果有）或使用知识库ID作为名称
              knowledge_base_name: data.knowledgebaseName || `知识库 ${data.knowledgebaseId}`,
              // 添加文件名（如果有doc.name）或使用文档ID作为文件名
              file_name: doc.name || `文档 ${doc.id}`
            }
          }));
          
          // 为助手消息添加引用标记，以便在UI中显示引用信息
          let contentWithCitations = msg.content || '';
          if (citations && citations.length > 0) {
            // 只有当确实有引用时才添加引用标记，为每个引用生成对应的标记
            const citationMarkers = citations.map(citation => `[citation](${citation.id})`).join(', ');
            contentWithCitations += `\n\n参考资料来源：${citationMarkers}`;
          }
          
          return {
            id: msg.id || `msg-${index}`,
            role: 'assistant',
            content: contentWithCitations,
            citations: citations || []
          };
        }
        
        return {
          id: msg.id || `msg-${index}`,
          role: msg.role === 'ASSISTANT' ? 'assistant' : 'user',
          content: msg.content || '',
          citations: []
        };
      });
      
      setMessages(messagesToSet);
    } catch (error) {
      console.error("Failed to load chat data from API:", error);
      toast({
        title: "Error",
        description: "加载聊天数据失败",
        variant: "destructive",
      });
      router.push("/dashboard/chat");
    }
  };

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };

  const processMessageContent = (message: Message): Message => {
    if (message.role !== "assistant" || !message.content) return message;

    try {
      if (!message.content.includes("__LLM_RESPONSE__")) {
        return message;
      }

      const [base64Part, responseText] =
        message.content.split("__LLM_RESPONSE__");

      const contextData = base64Part
        ? (JSON.parse(atob(base64Part.trim())) as {
            context: Array<{
              page_content: string;
              metadata: Record<string, any>;
            }>;
          })
        : null;

      const citations: Citation[] =
        contextData?.context.map((citation, index) => ({
          id: index + 1,
          text: citation.page_content,
          metadata: citation.metadata,
        })) || [];

      return {
        ...message,
        content: responseText || "",
        citations,
      };
    } catch (e) {
      console.error("Failed to process message:", e);
      return message;
    }
  };

  // 全新实现的markdownParse函数，避免正则表达式语法问题
  const markdownParse = (text: string) => {
    let result = text;
    
    // 替换模式1: [[Citation -> [citation
    result = result.split('[[Citation').join('[citation');
    result = result.split('[[citation').join('[citation');
    
    // 替换模式2: Citation:123]] -> citation:123]
    result = result.replace(/Citation:(\d+)]]/g, 'citation:$1]');
    result = result.replace(/citation:(\d+)]]/g, 'citation:$1]');
    
    // 替换模式3: [[Citation:123]] -> [Citation:123]
    result = result.replace(/\[\[Citation:(\d+)]]/g, '[Citation:$1]');
    result = result.replace(/\[\[citation:(\d+)]]/g, '[citation:$1]');
    
    // 替换模式4: [Citation:123] -> [citation](123)
    result = result.replace(/\[Citation:(\d+)]/g, '[citation]($1)');
    result = result.replace(/\[citation:(\d+)]/g, '[citation]($1)');
    
    return result;
  };

  const processedMessages = useMemo(() => {
    return messages.map((message) => {
      if (message.role !== "assistant" || !message.content) return message;

      try {
        if (!message.content.includes("__LLM_RESPONSE__")) {
          return {
            ...message,
            content: markdownParse(message.content),
          };
        }

        const [base64Part, responseText] =
          message.content.split("__LLM_RESPONSE__");

        const contextData = base64Part
          ? (JSON.parse(atob(base64Part.trim())) as {
              context: Array<{
                page_content: string;
                metadata: Record<string, any>;
              }>;
            })
          : null;

        const citations: Citation[] =
          contextData?.context.map((citation, index) => ({
            id: index + 1,
            text: citation.page_content,
            metadata: citation.metadata,
          })) || [];

        return {
          ...message,
          content: markdownParse(responseText || ""),
          citations,
        };
      } catch (e) {
        console.error("Failed to process message:", e);
        return message;
      }
    });
  }, [messages]);

  return (
    <DashboardLayout>
      <div className="flex flex-col h-[calc(100vh-5rem)] relative">
        <div className="flex-1 overflow-y-auto p-4 space-y-4 pb-[80px]">
          {processedMessages.map((message) =>
            message.role === "assistant" ? (
              <div
                key={message.id}
                className="flex justify-start items-start space-x-2"
              >
                <div className="w-8 h-8 flex items-center justify-center">
                  <img
                    src="/logo.png"
                    className="h-8 w-8 rounded-full"
                    alt="logo"
                  />
                </div>
                <div className="max-w-[80%] rounded-lg px-4 py-2 text-accent-foreground">
                  <Answer
                    key={message.id}
                    markdown={message.content}
                    citations={message.citations}
                  />
                </div>
              </div>
            ) : (
              <div
                key={message.id}
                className="flex justify-end items-start space-x-2"
              >
                <div className="max-w-[80%] rounded-lg px-4 py-2 bg-primary text-primary-foreground">
                  {message.content}
                </div>
                <div className="w-8 h-8 rounded-full bg-primary flex items-center justify-center">
                  <User className="h-5 w-5 text-primary-foreground" />
                </div>
              </div>
            )
          )}
          <div className="flex justify-start">
            {isLoading &&
              processedMessages[processedMessages.length - 1]?.role !=
                "assistant" && (
                <div className="max-w-[80%] rounded-lg px-4 py-2 text-accent-foreground">
                  <div className="flex items-center space-x-1">
                    <div className="w-2 h-2 rounded-full bg-primary animate-bounce" />
                    <div className="w-2 h-2 rounded-full bg-primary animate-bounce [animation-delay:0.2s]" />
                    <div className="w-2 h-2 rounded-full bg-primary animate-bounce [animation-delay:0.4s]" />
                  </div>
                </div>
              )}
          </div>
          <div ref={messagesEndRef} />
        </div>
        <form
          onSubmit={handleSubmit}
          className="border-t p-4 flex items-center space-x-4 bg-background absolute bottom-0 left-0 right-0"
        >
          <input
            ref={inputRef}
            id="chat-input"
            value={input}
            onChange={handleInputChange}
            placeholder="Type your message..."
            className="flex-1 min-w-0 h-10 rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
          />
          <button
            type="submit"
            disabled={isLoading || !input.trim()}
            className="inline-flex items-center justify-center rounded-md text-sm font-medium ring-offset-background transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50 bg-primary text-primary-foreground hover:bg-primary/90 h-10 px-4 py-2"
          >
            <Send className="h-4 w-4" />
          </button>
        </form>
      </div>
    </DashboardLayout>
  );
}
