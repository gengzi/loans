"use client";

import { useState, useEffect } from "react";
import { useParams } from "next/navigation";
import { ChevronLeft, FileText, Share2, Download, Info, Search, AlertCircle } from "lucide-react";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Divider } from "@/components/ui/divider";

import DashboardLayout from "@/components/layout/dashboard-layout";
import { useToast } from "@/components/ui/use-toast";
import { FileIcon, defaultStyles } from "react-file-icon";
import { api } from "@/lib/api";

interface DocumentDetail {
  id: string;
  name: string;
  size: number;
  suffix: string;
  createDate: string;
  status: string;
  chunks: DocumentChunk[];
  pages?: number;
  totalChunks?: number;
  updateTime?: string;
}

interface ApiChunkDetail {
  id: string;
  content: string;
  pageNumInt: string;
  img: string[];
}

interface ApiDocumentResponse {
  code: number;
  success: boolean;
  message: string;
  data: {
    id: string;
    name: string;
    createTime: number;
    size: number;
    chunkNum: number;
    chunkDetails: ApiChunkDetail[];
  };
}

interface DocumentPreview {
  url: string;
  contentType: string;
}

interface DocumentChunk {
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
  imgUrls?: string[];
}

interface KnowledgeBaseInfo {
  id: string;
  name: string;
  description: string;
}

export default function DocumentDetailPage() {
  const params = useParams();
  const knowledgeBaseId = params.id as string;
  const documentId = params.docId as string;
  const [document, setDocument] = useState<DocumentDetail | null>(null);
  const [knowledgeBase, setKnowledgeBase] = useState<KnowledgeBaseInfo | null>(null);
  const [documentPreview, setDocumentPreview] = useState<DocumentPreview | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedSection, setSelectedSection] = useState<string | null>(null);
  const [expandedSections, setExpandedSections] = useState<Set<string>>(new Set());
  const { toast } = useToast();
  
  // 辅助函数：从文件扩展名获取后缀
  const getFileExtension = (fileName: string): string => {
    const parts = fileName.split('.');
    return parts.length > 1 ? parts[parts.length - 1].toLowerCase() : '';
  };
  
  // 辅助函数：格式化日期
  const formatDate = (timestamp: number): string => {
    const date = new Date(timestamp);
    return date.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    });
  };
  
  // 模拟数据 - 文档信息
  const mockDocument = {
    id: documentId,
    name: "02_快速入门.pdf",
    size: 2097152,
    suffix: "pdf",
    createDate: "2024-01-15T10:30:00Z",
    updateTime: "2024-01-15T10:35:00Z",
    status: "completed",
    pages: 25,
    totalChunks: 48
  };
  
  // 模拟数据 - 文档块
  const mockChunks = [
    {
      id: "chunk-1",
      content: "容器虚拟化技术和自动化部署\n\n随着云计算和DevOps的发展，容器技术已经成为现代应用部署的重要基础设施。Docker作为最流行的容器技术，提供了轻量级的虚拟化解决方案。\n\n本文将介绍容器虚拟化的基本概念、Docker的核心组件以及如何使用Jenkins和GitLab实现自动化部署流程。\n\n主要内容包括：\n1. 容器基础概念\n2. Docker核心组件\n3. Kubernetes入门\n4. CI/CD自动化部署\n5. 最佳实践与案例分析",
      index: 0,
      metadata: {
        pageNumber: 1,
        section: "引言",
        chunkIndex: 0
      }
    },
    {
      id: "chunk-2",
      content: "Kubernetes快速入门之命令行\n\n1. Namespace介绍\n\nNamespace是Kubernetes中的资源隔离机制，它允许在一个物理集群上创建多个虚拟集群。在多租户环境中，Namespace可以很好地隔离不同团队或项目的资源。\n\n默认情况下，Kubernetes会创建一个名为default的命名空间。实际上，还有几个系统命名空间：\n- kube-system: 用于存放系统组件\n- kube-public: 公共资源\n- kube-node-lease: 节点租约信息\n\n命名空间可以通过标签(Label)进行管理，例如：\n```bash\nkubectl create namespace myproject\nkubectl get namespaces\nkubectl label namespace myproject env=development\n```",
      index: 1,
      metadata: {
        pageNumber: 2,
        section: "Kubernetes基础",
        chunkIndex: 1
      }
    },
    {
      id: "chunk-3",
      content: "Pod与Deployment管理\n\nPod是Kubernetes中最小的部署单元，通常包含一个或多个容器。Deployment则提供了Pod的声明式更新能力。\n\n创建Deployment的基本命令：\n```bash\nkubectl create deployment nginx-deployment --image=nginx:latest\nkubectl get deployments\nkubectl get pods\n```\n\n查看Pod详细信息：\n```bash\nkubectl describe pod [pod-name]\nkubectl logs [pod-name]\n```\n\n扩缩容操作：\n```bash\nkubectl scale deployment nginx-deployment --replicas=3\n```",
      index: 2,
      metadata: {
        pageNumber: 3,
        section: "Pod管理",
        chunkIndex: 2
      }
    },
    {
      id: "chunk-4",
      content: "Service与Ingress\n\nService为Pod提供稳定的网络访问方式，而Ingress则管理外部访问。\n\n创建Service：\n```bash\nkubectl expose deployment nginx-deployment --port=80 --type=NodePort\nkubectl get services\n```\n\nIngress配置示例：\n```yaml\napiVersion: networking.k8s.io/v1\nkind: Ingress\nmetadata:\n  name: nginx-ingress\nspec:\n  rules:\n  - host: example.com\n    http:\n      paths:\n      - path: /\n        pathType: Prefix\n        backend:\n          service:\n            name: nginx-service\n            port:\n              number: 80\n```",
      index: 3,
      metadata: {
        pageNumber: 4,
        section: "网络配置",
        chunkIndex: 3
      }
    },
    {
      id: "chunk-5",
      content: "存储管理与配置\n\nKubernetes提供了多种存储方案，包括EmptyDir、HostPath、PersistentVolume等。\n\n创建PersistentVolumeClaim：\n```yaml\napiVersion: v1\nkind: PersistentVolumeClaim\nmetadata:\n  name: mysql-pvc\nspec:\n  accessModes:\n    - ReadWriteOnce\n  resources:\n    requests:\n      storage: 10Gi\n```\n\n在Pod中使用存储：\n```yaml\nvolumes:\n- name: mysql-data\n  persistentVolumeClaim:\n    claimName: mysql-pvc\n```\n\n配置管理通过ConfigMap和Secret实现：\n```bash\nkubectl create configmap app-config --from-literal=APP_ENV=production\nkubectl create secret generic db-secret --from-literal=DB_PASSWORD=securepass\n```",
      index: 4,
      metadata: {
        pageNumber: 5,
        section: "存储与配置",
        chunkIndex: 4
      }
    }
  ];

  const mockKnowledgeBase = {
    id: knowledgeBaseId,
    name: "技术文档库",
    description: "存放各类技术文档和教程的知识库"
  };

  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true);
        setError(null);
        
        // 调用分块详情API
        try {
          // 使用公共定义的API配置
          const chunksData = await api.get(`/document/chunks/details?documentId=${documentId}`);
          console.log("分块数据:", chunksData);
          
          // 解析API响应
          if (chunksData.success && chunksData.data) {
            const apiResponse = chunksData as ApiDocumentResponse;
            const { data } = apiResponse;
            
            // 转换为文档详情格式
             const chunks: DocumentChunk[] = data.chunkDetails.map((chunkDetail, index) => ({
               id: chunkDetail.id,
               content: chunkDetail.content,
               index: index, // 添加index属性以匹配接口定义
               metadata: {
                 pageNumber: chunkDetail.pageNumInt ? parseInt(chunkDetail.pageNumInt.split(',')[0]) + 1 : index + 1,
                 pageNumbers: chunkDetail.pageNumInt.split(',').map(p => parseInt(p) + 1),
                 chunkNumber: index + 1
               },
               embedding: [], // 添加空的embedding数组以匹配接口定义
               imgUrls: chunkDetail.img
             }));
            
            // 构建文档详情对象
            const documentDetail: DocumentDetail = {
              id: data.id,
              name: data.name,
              size: data.size,
              suffix: getFileExtension(data.name),
              createDate: formatDate(data.createTime),
              status: "已处理",
              chunks: chunks,
              pages: Math.max(0, ...(chunks.map(c => c.metadata.pageNumbers).flat().filter((num): num is number => typeof num === 'number'))),
              totalChunks: data.chunkNum,
              updateTime: formatDate(data.createTime)
            };
            
            setDocument(documentDetail);
            
            // 仍使用模拟的知识库数据
            setKnowledgeBase(mockKnowledgeBase);
          } else {
            throw new Error(chunksData.message || "获取分块数据失败");
          }
        } catch (chunksError) {
          console.error("获取分块数据失败:", chunksError);
          // 失败时使用模拟数据作为备用
          setDocument({...mockDocument, chunks: mockChunks});
          setKnowledgeBase(mockKnowledgeBase);
          toast({ variant: "destructive", title: "警告", description: "无法获取真实分块数据，已使用模拟数据" });
        }
        
        // 获取文档预览URL和内容类型
        try {
          const previewData = await api.get(`/document/${documentId}`);
          setDocumentPreview(previewData);
          console.log("文档预览数据:", previewData);
        } catch (previewError) {
          console.error("获取文档预览失败:", previewError);
          // 继续使用模拟数据，不阻止页面加载
        }
      } catch (err) {
        console.error("获取数据失败:", err);
        setError("获取数据失败，请稍后重试");
        // 最后使用模拟数据作为兜底
        setDocument({...mockDocument, chunks: mockChunks});
        setKnowledgeBase(mockKnowledgeBase);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, [knowledgeBaseId, documentId, toast]);

  const handleDownload = async () => {
    try {
      // 直接使用window.open打开下载链接
      window.open(`/api/knowledge-base/${knowledgeBaseId}/doc/${documentId}/download`, '_blank');
      
      toast({
        title: "开始下载",
        description: "文档已开始下载"
      });
    } catch (err) {
      console.error("下载文档失败:", err);
      toast({
        title: "下载失败",
        description: "无法下载文档，请稍后重试",
        variant: "destructive"
      });
    }
  };

  // 过滤文档块
  const filteredChunks = document?.chunks
    ?.filter(chunk => {
      if (!searchQuery) return true;
      return chunk.content.toLowerCase().includes(searchQuery.toLowerCase());
    })
    .sort((a, b) => a.index - b.index) || [];

  // 按章节组织文档块
  const sections = document?.chunks?.reduce((acc, chunk) => {
    const section = chunk.metadata?.section || "未分类";
    if (!acc[section]) {
      acc[section] = [];
    }
    acc[section].push(chunk);
    return acc;
  }, {} as Record<string, DocumentChunk[]>) || {};

  const toggleSection = (section: string) => {
    const newExpanded = new Set(expandedSections);
    if (newExpanded.has(section)) {
      newExpanded.delete(section);
    } else {
      newExpanded.add(section);
    }
    setExpandedSections(newExpanded);
  };

  const handleSectionClick = (section: string) => {
    setSelectedSection(section);
    if (!expandedSections.has(section)) {
      toggleSection(section);
    }
  };

  // 根据选择的章节过滤文档块
  const displayedChunks = selectedSection 
    ? sections[selectedSection] || []
    : filteredChunks;

  if (loading) {
    return (
      <DashboardLayout>
        <div className="flex flex-col min-h-[70vh] justify-center items-center p-8">
          <div className="w-12 h-12 border-4 border-primary/30 border-t-primary rounded-full animate-spin mb-4"></div>
          <p className="text-muted-foreground">加载文档详情中...</p>
        </div>
      </DashboardLayout>
    );
  }

  if (error || !document || !knowledgeBase) {
    return (
      <DashboardLayout>
        <div className="flex flex-col min-h-[70vh] justify-center items-center p-8 text-center">
          <AlertCircle className="w-16 h-16 text-destructive/50 mb-4" />
          <h3 className="text-xl font-semibold mb-2">无法加载文档</h3>
          <p className="text-muted-foreground max-w-md mb-6">{error || "文档不存在或已被删除"}</p>
          <Link href={`/dashboard/knowledge/${knowledgeBaseId}/doc`}>
            <Button>返回知识库</Button>
          </Link>
        </div>
      </DashboardLayout>
    );
  }

  // 获取文件图标
  const getFileIcon = () => {
    const suffix = document.suffix.toLowerCase();
    if (suffix === "pdf") return <FileIcon extension="pdf" {...defaultStyles.pdf} />;
    if (suffix === "doc" || suffix === "docx") return <FileIcon extension="doc" {...defaultStyles.docx} />;
    if (suffix === "txt") return <FileIcon extension="txt" {...defaultStyles.txt} />;
    if (suffix === "md") return <FileIcon extension="md" {...defaultStyles.md} />;
    return <FileIcon extension={suffix} color="#E2E8F0" labelColor="#94A3B8" />;
  };

  return (
    <DashboardLayout>
    <div className="space-y-4">
        {/* 面包屑导航 */}
        <div className="flex items-center gap-2 text-sm">
          <Link href="/dashboard" className="text-muted-foreground hover:text-foreground">
            仪表盘
          </Link>
          <ChevronLeft className="h-4 w-4 text-muted-foreground" />
          <Link href={`/dashboard/knowledge`} className="text-muted-foreground hover:text-foreground">
            知识库
          </Link>
          <ChevronLeft className="h-4 w-4 text-muted-foreground" />
          <Link href={`/dashboard/knowledge/${knowledgeBaseId}`} className="text-muted-foreground hover:text-foreground">
            {knowledgeBase.name}
          </Link>
          <ChevronLeft className="h-4 w-4 text-muted-foreground" />
          <span className="font-medium truncate max-w-[200px]">{document.name}</span>
        </div>

        {/* 文档信息头部 */}
        <div className="flex justify-between items-center">
          <div className="flex items-center gap-4">
            <div className="w-10 h-10 flex-shrink-0">
              {getFileIcon()}
            </div>
            <div>
              <h1 className="text-xl font-bold tracking-tight mb-1">{document.name}</h1>
              <div className="flex flex-wrap items-center gap-x-4 gap-y-2 text-sm text-muted-foreground">
                <span>{(document.size / 1024 / 1024).toFixed(2)} MB</span>
                <span>•</span>
                <span>{new Date(document.createDate).toLocaleDateString()}</span>
                <Badge variant="outline">{document.pages} 页</Badge>
              </div>
            </div>
          </div>
          
          <div className="flex gap-3">
            <Button variant="ghost" size="sm" onClick={handleDownload}>
              <Download className="h-4 w-4 mr-2" />
              下载
            </Button>
            <Button variant="ghost" size="sm">
              <Share2 className="h-4 w-4 mr-2" />
              分享
            </Button>
            <Button variant="ghost" size="sm">
              <Info className="h-4 w-4 mr-2" />
              详情
            </Button>
          </div>
        </div>

        {/* 主内容区域 - 双栏布局 */}
          <div className="flex gap-4">
            {/* 左侧面板 - 原文档内容 */}
            <div className="w-1/2">
              <Card>
                <CardContent className="p-0">
                  <div className="p-4 border-b">
                    <div className="flex justify-between items-center">
                      <h3 className="font-medium">{document.name}</h3>
                      <Badge variant="outline">{document.suffix.toUpperCase()}</Badge>
                    </div>
                  </div>
                  <div className="h-[calc(100vh-280px)] overflow-auto">
                    {/* 原文档内容区域 - 根据文档预览数据显示内容 */}
                    <div className="min-h-full p-4 bg-muted/30">
                      {documentPreview ? (
                        // 使用API返回的预览数据
                        <div className="w-full h-full">
                          {/* PDF内容预览 */}
                          {documentPreview.contentType === 'application/pdf' && (
                            <div className="w-full h-full">
                              <div className="bg-white h-full w-full rounded-lg shadow-sm overflow-hidden flex flex-col">
                                {/* 标题栏 */}
                                {/* <div className="px-4 py-2 border-b bg-muted/10 flex justify-between items-center">
                                  <div className="text-lg font-medium">PDF 文档预览</div>
                                </div> */}
                                {/* 使用iframe加载PDF预览URL */}
                                <iframe 
                                  src={documentPreview.url}
                                  className="w-full h-[calc(100vh-320px)]"
                                  title="PDF 文档预览"
                                  frameBorder="0"
                                />
                              </div>
                            </div>
                          )}
                          
                          {/* Markdown内容预览 */}
                          {documentPreview.contentType === 'text/markdown' && (
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
                          
                          {/* 其他文件类型的预览 */}
                          {!['application/pdf', 'text/markdown'].includes(documentPreview.contentType) && (
                            <div className="w-full h-full">
                              <div className="bg-white h-full w-full rounded-lg shadow-sm overflow-hidden flex flex-col">
                                {/* 标题栏 */}
                                <div className="px-4 py-2 border-b bg-muted/10 flex justify-between items-center">
                                  <div className="text-lg font-medium">文档预览</div>
                                </div>
                                {/* 内容区域 */}
                                <div className="flex-1 flex flex-col justify-center items-center p-6">
                                  <div className="text-center mb-4">
                                    <Badge variant="outline">{documentPreview.contentType}</Badge>
                                  </div>
                                  <div className="text-sm text-muted-foreground mb-6 max-w-lg text-center">
                                    文件URL: {documentPreview.url}
                                  </div>
                                  <Button 
                                    variant="default" 
                                    className="w-64"
                                    onClick={() => window.open(documentPreview.url, '_blank')}
                                  >
                                    <FileText className="h-4 w-4 mr-2" />
                                    在新窗口打开文档
                                  </Button>
                                </div>
                              </div>
                            </div>
                          )}
                        </div>
                      ) : (
                        // 回退到模拟数据预览
                        <div className="w-full h-full">
                          <div className="bg-white h-full w-full rounded-lg shadow-sm overflow-hidden flex flex-col">
                            {/* 标题栏 */}
                            <div className="px-4 py-2 border-b bg-muted/10 flex justify-between items-center">
                              <div className="text-lg font-medium">
                                {document.suffix.toLowerCase() === 'pdf' && "PDF 预览"}
                                {document.suffix.toLowerCase() === 'md' && "Markdown 预览"}
                                {!['pdf', 'md'].includes(document.suffix.toLowerCase()) && "文档预览"}
                              </div>
                            </div>
                            {/* 内容区域 */}
                            <div className="flex-1 overflow-auto p-4">
                              {filteredChunks.map((chunk) => (
                                <div key={chunk.id} className="mb-6">
                                  {chunk.metadata.pageNumber && (
                                    <div className="flex justify-between items-center text-sm text-muted-foreground mb-2">
                                      <span>第 {chunk.metadata.pageNumber} 页</span>
                                    </div>
                                  )}
                                  <div className="whitespace-pre-wrap leading-relaxed">
                                    {chunk.content}
                                  </div>
                                </div>
                              ))}
                            </div>
                          </div>
                        </div>
                      )}
                    </div>
                  </div>
                </CardContent>
              </Card>
            </div>

            {/* 右侧面板 - 分块结果 */}
            <div className="w-1/2">
              <Card>
                <CardContent className="p-0">
                  <div className="p-4 border-b">
                    <div className="flex justify-between items-center">
                      <h3 className="font-medium">切片结果</h3>
                      <Badge variant="outline">{document.chunks.length} 块</Badge>
                    </div>
                    {/* 搜索框 */}
                    <div className="relative mt-3">
                      <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                      <Input 
                        placeholder="搜索切片..." 
                        value={searchQuery}
                        onChange={(e) => setSearchQuery(e.target.value)}
                        className="pl-9"
                      />
                    </div>
                  </div>
                  <div className="h-[calc(100vh-280px)] overflow-y-auto">
                    {filteredChunks.length === 0 ? (
                      <div className="text-center py-12 text-muted-foreground">
                        未找到匹配的切片
                      </div>
                    ) : (
                      <div className="p-2">
                        {filteredChunks.map((chunk) => (
                          <div key={chunk.id} className="mb-4 border rounded-lg overflow-hidden">
                            {/* 切片图片和内容 - 左右布局 */}
                            <div className="flex">
                              {/* 图片预览区域 */}
                              <div className="w-1/3 bg-muted/30 flex items-center justify-center p-2">
                                {chunk.imgUrls && chunk.imgUrls.length > 0 ? (
                                  <div className="w-full h-full">
                                    {chunk.imgUrls.map((imgUrl, idx) => (
                                      <img
                                        key={idx}
                                        src={imgUrl.startsWith('http') ? imgUrl : `/api/image/${imgUrl}`}
                                        alt={`Page ${chunk.metadata.pageNumber}`}
                                        className="w-full h-auto object-contain"
                                      />
                                    ))}
                                  </div>
                                ) : (
                                  <div className="text-center">
                                    <FileText className="h-8 w-8 mx-auto text-muted-foreground mb-2" />
                                    <span className="text-xs text-muted-foreground">第 {chunk.metadata.pageNumber} 页</span>
                                  </div>
                                )}
                              </div>
                              {/* 内容信息区域 */}
                              <div className="w-2/3 p-3">
                                <div className="flex justify-between items-center text-xs text-muted-foreground mb-2">
                                  <span>切片 {chunk.index + 1}</span>
                                </div>
                                <div className="text-sm mb-3 line-clamp-3">
                                  {chunk.content.substring(0, 100)}...
                                </div>
                                <div className="flex justify-between items-center">
                                  <div className={`w-4 h-4 border rounded flex-shrink-0 ${selectedSection === chunk.id ? 'bg-primary border-primary flex items-center justify-center' : 'border-border'}`}>
                                    {selectedSection === chunk.id && <span className="text-xs text-primary-foreground">✓</span>}
                                  </div>
                                  <Button variant="ghost" size="sm" className="h-8 text-xs">
                                    查看原始内容
                                  </Button>
                                </div>
                              </div>
                            </div>
                            </div>
                          ))}
                      </div>
                    )}
                  </div>
                </CardContent>
              </Card>
            </div>
          </div>
    </div>
    </DashboardLayout>
  );
}