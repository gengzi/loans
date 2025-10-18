'use client';

import DashboardLayout from '@/components/layout/dashboard-layout';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Table, TableBody, TableCaption, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Badge } from '@/components/ui/badge';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, RadarChart, PolarGrid, PolarAngleAxis, PolarRadiusAxis, Radar } from 'recharts';
import { Search, Download, RefreshCw, PlusCircle } from 'lucide-react';

// 模拟数据
const evaluationData = [
  { id: 1, 问题: '贷款申请流程', 参考答案: '贷款申请流程包括提交申请、资料审核、信用评估、审批通过和放款', 实际回答: '贷款申请流程包括提交申请、资料审核、信用评估、审批通过和放款', 准确率: '高', 完整度: '高', 相关性: '高' },
  { id: 2, 问题: '贷款利率如何计算', 参考答案: '贷款利率根据贷款期限、贷款金额、信用等级综合计算', 实际回答: '贷款利率主要看贷款期限和金额', 准确率: '中', 完整度: '低', 相关性: '高' },
  { id: 3, 问题: '还款方式有哪些', 参考答案: '还款方式包括等额本息、等额本金、先息后本等', 实际回答: '还款方式有等额本息和等额本金两种', 准确率: '高', 完整度: '中', 相关性: '高' },
  { id: 4, 问题: '贷款逾期有什么影响', 参考答案: '贷款逾期会影响信用记录、产生罚息、可能被起诉', 实际回答: '贷款逾期会影响信用记录', 准确率: '高', 完整度: '低', 相关性: '高' },
  { id: 5, 问题: '提前还款需要注意什么', 参考答案: '提前还款需要注意是否有违约金、提前多久申请', 实际回答: '提前还款需要提前申请', 准确率: '高', 完整度: '中', 相关性: '高' },
];

// 图表数据
const chartData = [
  { name: 'Mon', 准确率: 90, 完整度: 85, 相关性: 92, 一致性: 88, 及时性: 94, 可理解性: 91 },
  { name: 'Tue', 准确率: 92, 完整度: 88, 相关性: 94, 一致性: 90, 及时性: 95, 可理解性: 93 },
  { name: 'Wed', 准确率: 88, 完整度: 86, 相关性: 90, 一致性: 87, 及时性: 92, 可理解性: 90 },
  { name: 'Thu', 准确率: 95, 完整度: 90, 相关性: 96, 一致性: 93, 及时性: 97, 可理解性: 95 },
  { name: 'Fri', 准确率: 93, 完整度: 89, 相关性: 95, 一致性: 91, 及时性: 96, 可理解性: 94 },
  { name: 'Sat', 准确率: 94, 完整度: 91, 相关性: 97, 一致性: 92, 及时性: 98, 可理解性: 96 },
  { name: 'Sun', 准确率: 96, 完整度: 92, 相关性: 98, 一致性: 94, 及时性: 99, 可理解性: 97 },
];

// 雷达图数据
const radarData = [
  { subject: '准确率', A: 94, fullMark: 100 },
  { subject: '完整度', A: 89, fullMark: 100 },
  { subject: '相关性', A: 95, fullMark: 100 },
  { subject: '一致性', A: 91, fullMark: 100 },
  { subject: '及时性', A: 96, fullMark: 100 },
  { subject: '可理解性', A: 94, fullMark: 100 },
];

export default function RAGEvaluationPage() {
  return (
    <DashboardLayout>
      <div className="mb-6 flex items-center justify-between">
        <h1 className="text-3xl font-bold">RAG评估</h1>
        <div className="flex items-center gap-3">
          <Button variant="ghost" size="icon">
            <Download className="h-5 w-5" />
          </Button>
          <Button variant="ghost" size="icon">
            <RefreshCw className="h-5 w-5" />
          </Button>
          <Button>
            <PlusCircle className="mr-2 h-4 w-4" />
            新建评估
          </Button>
        </div>
      </div>

      <Tabs defaultValue="evaluation" className="mb-8">
        <TabsList className="mb-6">
          <TabsTrigger value="evaluation">评估结果</TabsTrigger>
          <TabsTrigger value="statistics">统计分析</TabsTrigger>
        </TabsList>

        <TabsContent value="evaluation" className="space-y-6">
          {/* 搜索和筛选区域 */}
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
            <div className="relative w-full md:w-96">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-muted-foreground" />
              <Input 
                placeholder="搜索问题或回答..." 
                className="pl-10 h-10 w-full"
              />
            </div>
            <div className="flex items-center gap-3">
              <Select defaultValue="all">
                <SelectTrigger className="h-10 w-[140px]">
                  <SelectValue placeholder="准确率" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">全部</SelectItem>
                  <SelectItem value="high">高</SelectItem>
                  <SelectItem value="medium">中</SelectItem>
                  <SelectItem value="low">低</SelectItem>
                </SelectContent>
              </Select>
              <Select defaultValue="all">
                <SelectTrigger className="h-10 w-[140px]">
                  <SelectValue placeholder="完整度" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">全部</SelectItem>
                  <SelectItem value="high">高</SelectItem>
                  <SelectItem value="medium">中</SelectItem>
                  <SelectItem value="low">低</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>

          {/* 评估结果表格 */}
          <Card>
            <CardHeader>
              <CardTitle>评估结果列表</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="overflow-x-auto">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>ID</TableHead>
                      <TableHead>问题</TableHead>
                      <TableHead>参考答案</TableHead>
                      <TableHead>实际回答</TableHead>
                      <TableHead>准确率</TableHead>
                      <TableHead>完整度</TableHead>
                      <TableHead>相关性</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {evaluationData.map((item) => (
                      <TableRow key={item.id}>
                        <TableCell className="font-medium">{item.id}</TableCell>
                        <TableCell className="max-w-[200px] truncate">{item.问题}</TableCell>
                        <TableCell className="max-w-[200px] truncate">{item.参考答案}</TableCell>
                        <TableCell className="max-w-[200px] truncate">{item.实际回答}</TableCell>
                        <TableCell>
                          <Badge variant={item.准确率 === '高' ? 'default' : item.准确率 === '中' ? 'secondary' : 'destructive'}>
                            {item.准确率}
                          </Badge>
                        </TableCell>
                        <TableCell>
                          <Badge variant={item.完整度 === '高' ? 'default' : item.完整度 === '中' ? 'secondary' : 'destructive'}>
                            {item.完整度}
                          </Badge>
                        </TableCell>
                        <TableCell>
                          <Badge variant={item.相关性 === '高' ? 'default' : item.相关性 === '中' ? 'secondary' : 'destructive'}>
                            {item.相关性}
                          </Badge>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="statistics" className="space-y-6">
          {/* 统计图表 */}
          <Card>
            <CardHeader>
              <CardTitle>评估指标趋势</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                {/* 折线图 */}
                <div className="h-[400px]">
                  <ResponsiveContainer width="100%" height="100%">
                    <LineChart
                      data={chartData}
                      margin={{ top: 10, right: 30, left: 0, bottom: 0 }}
                    >
                      <CartesianGrid strokeDasharray="3 3" />
                      <XAxis dataKey="name" />
                      <YAxis domain={[0, 100]} />
                      <Tooltip />
                      <Line type="monotone" dataKey="准确率" stroke="#8884d8" strokeWidth={2} />
                      <Line type="monotone" dataKey="完整度" stroke="#82ca9d" strokeWidth={2} />
                      <Line type="monotone" dataKey="相关性" stroke="#ffc658" strokeWidth={2} />
                      <Line type="monotone" dataKey="一致性" stroke="#ff8042" strokeWidth={2} />
                      <Line type="monotone" dataKey="及时性" stroke="#0088fe" strokeWidth={2} />
                      <Line type="monotone" dataKey="可理解性" stroke="#00C49F" strokeWidth={2} />
                    </LineChart>
                  </ResponsiveContainer>
                </div>
                
                {/* 雷达图 */}
                <div className="h-[400px]">
                  <ResponsiveContainer width="100%" height="100%">
                    <RadarChart cx="50%" cy="50%" outerRadius="80%" data={radarData}>
                      <PolarGrid />
                      <PolarAngleAxis dataKey="subject" />
                      <PolarRadiusAxis angle={30} domain={[0, 100]} />
                      <Radar name="RAG性能" dataKey="A" stroke="#8884d8" fill="#8884d8" fillOpacity={0.6} />
                      <Tooltip />
                    </RadarChart>
                  </ResponsiveContainer>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* 优化建议 */}
          <Card>
            <CardHeader>
              <CardTitle>优化建议</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="prose max-w-none">
                <p>优先解决缺失问题：若准确率（置信度低）或相关性较差（答非所问），优先优化提示模板（Prompt约束、模型调优等），这是用户最易感知的问题。</p>
                <p>再解决完整度问题：若回答详尽但存在"部分遗忘"或"知识点少"，优化检索策略（调整相似度阈值、增加返回文档数量）或增加知识库覆盖面。</p>
                <p>最后处理细节问题：如专业术语使用不当、回答逻辑混乱等，可通过优化提示词模板或微调模型参数解决。</p>
                <p>若准确率和完整度都很高，但用户仍不满意，可能是提示词或答案格式问题，需要进一步分析用户反馈优化。</p>
              </div>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </DashboardLayout>
  );
}