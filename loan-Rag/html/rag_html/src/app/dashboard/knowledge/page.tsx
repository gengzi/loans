"use client";

import { useState, useRef, useEffect } from "react";
import Link from "next/link";
import { FileIcon, defaultStyles } from "react-file-icon";
import { ArrowRight, Plus, Settings, Trash2, Search } from "lucide-react";
import DashboardLayout from "@/components/layout/dashboard-layout";
import { api, ApiError } from "@/lib/api";
import { useToast } from "@/components/ui/use-toast";

interface KnowledgeBase {
  id: string;
  createTime: number;
  createDate: string;
  updateTime: number;
  updateDate: string;
  avatar: string | null;
  name: string;
  language: string | null;
  description: string;
  createdBy: string;
  docNum: number;
  tokenNum: number;
  chunkNum: number;
  status: string | null;
}
interface Document {
  id: number;
  file_name: string;
  file_path: string;
  file_size: number;
  content_type: string;
  knowledge_base_id: number;
  created_at: string;
  updated_at: string;
  processing_tasks: any[];
}

export default function KnowledgeBasePage() {
  const [knowledgeBases, setKnowledgeBases] = useState<KnowledgeBase[]>([]);
  const [loading, setLoading] = useState(true);
  const { toast } = useToast();
  const hasFetchedKnowledgeBases = useRef(false);

  useEffect(() => {
    if (!hasFetchedKnowledgeBases.current) {
      hasFetchedKnowledgeBases.current = true;
      fetchKnowledgeBases();
    }
  }, []);

  const fetchKnowledgeBases = async () => {
    try {
      const data = await api.get("/api/knowledge-base");
      setKnowledgeBases(data);
    } catch (error) {
      console.error("Failed to fetch knowledge bases:", error);
      if (error instanceof ApiError) {
        toast({
          title: "Error",
          description: error.message,
          variant: "destructive",
        });
      }
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id: string) => {
    if (!confirm("Are you sure you want to delete this knowledge base?"))
      return;
    try {
      await api.delete(`/api/knowledge-base/${id}`);
      setKnowledgeBases((prev) => prev.filter((kb) => kb.id !== id));
      toast({
        title: "Success",
        description: "Knowledge base deleted successfully",
      });
    } catch (error) {
      console.error("Failed to delete knowledge base:", error);
      if (error instanceof ApiError) {
        toast({
          title: "Error",
          description: error.message,
          variant: "destructive",
        });
      }
    }
  };

  return (
    <DashboardLayout>
      <div className="space-y-8">
        <div className="flex justify-between items-center">
          <div>
            <h2 className="text-3xl font-bold tracking-tight">
              Knowledge Bases
            </h2>
            <p className="text-muted-foreground">
              Manage your knowledge bases and documents
            </p>
          </div>
          <Link
            href="/dashboard/knowledge/new"
            className="inline-flex items-center justify-center rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
          >
            <Plus className="mr-2 h-4 w-4" />
            New Knowledge Base
          </Link>
        </div>

        <div className="grid gap-6">
          {knowledgeBases.map((kb) => (
            <div
              key={kb.id}
              className="rounded-lg border bg-card p-6 space-y-4"
            >
              <div className="flex justify-between items-start">
                <div>
                  <h3 className="text-lg font-semibold">{kb.name}</h3>
                  <p className="text-sm text-muted-foreground">
                    {kb.description || "No description"}
                  </p>
                  <p className="text-sm text-muted-foreground mt-1">
                    {kb.docNum} documents •
                    {new Date(kb.createDate).toLocaleDateString()}
                  </p>
                </div>

                <div className="flex space-x-2">
                  <Link
                    href={`/dashboard/knowledge/${kb.id}`}
                    className="inline-flex items-center justify-center rounded-md bg-secondary w-8 h-8"
                  >
                    <Settings className="h-4 w-4" />
                  </Link>
                  <Link
                    href={`/dashboard/test-retrieval/${kb.id}`}
                    className="inline-flex items-center justify-center rounded-md bg-secondary w-8 h-8"
                  >
                    <Search className="h-4 w-4" />
                  </Link>
                  <button
                    onClick={() => handleDelete(kb.id)}
                    className="inline-flex items-center justify-center rounded-md bg-destructive/10 hover:bg-destructive/20 w-8 h-8"
                  >
                    <Trash2 className="h-4 w-4 text-destructive" />
                  </button>
                </div>
              </div>

              {kb.docNum > 0 && (
                <div className="border-t pt-4">
                  <h4 className="text-sm font-medium mb-2">Documents</h4>
                  <div className="flex items-center gap-2">
                    <Link
                      href={`/dashboard/knowledge/${kb.id}`}
                      className="inline-flex items-center text-sm text-primary hover:underline"
                    >
                      View all {kb.docNum} documents
                      <ArrowRight className="ml-1 h-3 w-3" />
                    </Link>
                  </div>
                </div>
              )}
            </div>
          ))}

          {!loading && knowledgeBases.length === 0 && (
            <div className="text-center py-12">
              <p className="text-muted-foreground">
                No knowledge bases found. Create one to get started.
              </p>
            </div>
          )}

          {loading && (
            <div className="flex items-center justify-center py-12">
              <div className="space-y-4">
                <div className="w-8 h-8 border-4 border-primary/30 border-t-primary rounded-full animate-spin mx-auto"></div>
                <p className="text-muted-foreground animate-pulse">
                  Loading knowledge bases...
                </p>
              </div>
            </div>
          )}
        </div>
      </div>
    </DashboardLayout>
  );
}
