import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.IntArrayList;
import com.knuddels.jtokkit.api.ModelType;
import org.commonmark.ext.gfm.tables.TableBlock;
import org.commonmark.ext.gfm.tables.TableCell;
import org.commonmark.ext.gfm.tables.TableRow;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.*;
import org.commonmark.parser.Parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 基于 commonmark-java 的 RAG 分块工具
 * 特性：语法边界优先（标题/代码块/表格/图片独立拆分）+ Token 长度控制
 */
public class CommonmarkRagSplitter {
    // 配置参数（适配 GPT-3.5 4k Token）
    private final int MAX_TOKEN_PER_CHUNK; // 单个 chunk 最大 Token 数
    private final int CHUNK_OVERLAP;       // 相邻 chunk 重叠 Token 数
    private final Encoding tokenEncoder;   // Token 编码器
    private final Parser markdownParser;   // Markdown 解析器（带扩展）
    private final List<Chunk> chunkResult = new ArrayList<>();
    private final StringBuilder tempContent = new StringBuilder(); // 临时拼接普通内容
    // 临时状态
    private String currentChapter = "根文档"; // 当前章节标题


    // 构造器：初始化配置
    public CommonmarkRagSplitter(int maxTokenPerChunk, int chunkOverlap) {
        this.MAX_TOKEN_PER_CHUNK = maxTokenPerChunk;
        this.CHUNK_OVERLAP = chunkOverlap;
        // 初始化 Token 编码器（GPT-3.5/4 兼容）
        this.tokenEncoder = Encodings.newDefaultEncodingRegistry().getEncodingForModel(ModelType.GPT_3_5_TURBO);
        // 初始化解析器（启用表格、GFM图片扩展）
        this.markdownParser = Parser.builder()
                .extensions(Arrays.asList(
                        TablesExtension.create()
                ))
                .build();
    }

    // ------------------------------ 测试示例 ------------------------------
    public static void main(String[] args) {
        String testMd = """
                # 测试
                
                💡 字多 ≠ 有价值
                
                周报不是为了表现工作量，而是给团队提供最基本的“信息透明”。尽量挑选重要信息来写。
                
                汇报人：...
                
                日期：2022-01-20
                
                ## 本周重点
                
                ### 1.任务进展
                
                import org.springframework.beans.factory.annotation.Autowired;
                import org.springframework.stereotype.Service;
                import org.springframework.web.reactive.function.client.WebClient;
                import reactor.core.publisher.Mono;
                
                @Service
                public class MarkItDownMcpReactiveService {
                 private static final String MCP\\_CONVERT\\_PATH = "/mcp";
                
                 @Autowired
                 private WebClient webClient;
                
                 /\\*\\*
                 \\* 异步调用MCP服务转换文档
                 \\* @param resourceUri 资源URI
                 \\* @return 异步结果（Mono）
                 \\*/
                 public Mono<String> convertToMarkdownAsync(String resourceUri) {
                 // 构造请求体
                 McpConvertRequest request = new McpConvertRequest();
                 request.setTool("convert\\_to\\_markdown");
                 McpConvertRequest.McpConvertParams params = new McpConvertRequest.McpConvertParams();
                 params.setUri(resourceUri);
                 request.setParameters(params);
                
                 // 发送异步POST请求
                 return webClient.post()
                 .uri(MCP\\_CONVERT\\_PATH)
                 .bodyValue(request)
                 .retrieve()
                 .bodyToMono(McpConvertResponse.class)
                 .flatMap(response -> {
                 if ("failed".equals(response.getStatus())) {
                 return Mono.error(new RuntimeException("转换失败：" + response.getError()));
                 }
                 return Mono.just(response.getMarkdown());
                 });
                 }
                }
                
                本周完成了哪些任务、整体进度如何。
                
                本周完成了XXX需求开发，已经提测。项目整体进度比预期延迟1d，预计下周三可以开始正式测试。
                
                ![11](data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAA+gAAAQ8CAYAAA)
                
                ### 2.相关数据
                
                呈现相关数据以及背后的原因（如有）。
                
                本周日均 UV 3000，同比上周上涨20%。原因是周二投放的运营活动生效，吸引了部分新用户。
                
                |             |              |
                | ----------- | ------------ |
                | 本周日均 UV | 3000（↑20%） |
                | 上周日均 UV | 2500         |
                
                具体详见「数据接口」
                
                ### 3.风险同步
                
                存在哪些风险、对应的对策是什么。
                
                由于需要调用外网数据，需要在预发环境搭建代理，接下来需要考虑代理的通用性，在其他需要外网数据配合的需求中可以直接使用。
                
                以下是截至2023年全球十大富豪的财富情况：
                
                |                                                              |                                 |      |                               |                    |
                | ------------------------------------------------------------ | ------------------------------- | ---- | ----------------------------- | ------------------ |
                | 排名                                                         | 姓名                            | 国籍 | 财富来源                      | 财富净值（亿美元） |
                | 1                                                            | 埃隆·马斯克 (Elon Musk)         | 美国 | 特斯拉、SpaceX、推特等        | 2,190              |
                | 2                                                            | 杰夫·贝佐斯 (Jeff Bezos)        | 美国 | 亚马逊、蓝色起源等            | 1,670              |
                | 3                                                            | 伯纳德·阿诺特 (Bernard Arnault) | 法国 | 路威酩轩集团 (LVMH)           | 1,500              |
                | 4                                                            | 拉里·埃利森 (Larry Ellison)     | 美国 | 甲骨文公司 (Oracle)           | 1,130              |
                | 5                                                            | 比尔·盖茨 (Bill Gates)          | 美国 | 微软、比尔及梅琳达·盖茨基金会 | 1,080              |
                | 6                                                            | 史蒂夫·鲍尔默 (Steve Ballmer)   | 美国 | 微软、洛杉矶快船队等          | 1,030              |
                | 7                                                            | 沃伦·巴菲特 (Warren Buffett)    | 美国 | 伯克希尔·哈撒韦公司           | 1,020              |
                | 8                                                            | 劳伦斯·埃利森 (Larry Ellison)   | 美国 | 甲骨文公司 (Oracle)           | 1,000              |
                | 9                                                            | 马克·扎克伯格 (Mark Zuckerberg) | 美国 | Facebook、Meta Platforms      | 920                |
                | 10                                                           | 瑞·达利欧 (Ray Dalio)           | 美国 | 桥水投资公司                  | 840                |
                | 注：以上数据仅供参考，实际财富净值可能会因市场波动、投资变化等因素而有所变动。如需获取最新数据，请参考权威财经媒体或相关报告。 |                                 |      |                               |                    |
                
                ## 下周计划
                
                接下来要做什么、是否需要其他协助。
                
                下周开始主要投入XXX、XXX等功能点开发，依赖于中台团队提供接口，下周一和中台团队的xxx沟通确认。
                
                ## 思考
                
                有什么想法或心得体会，都可以拿出来分享下。
                """;

        CommonmarkRagSplitter commonmarkRagSplitter = new CommonmarkRagSplitter(300, 10);
        List<Chunk> split = commonmarkRagSplitter.split(testMd);
        for (Chunk chunk : split) {
            System.out.println("分块："+  chunk.getType() + ": " + chunk.getContent() +"\n\n");
            System.out.println("---------------------------------------------------");
        }

    }


    // ------------------------------ 节点处理逻辑 ------------------------------

    /**
     * 核心方法：拆分 Markdown 文本为 RAG 可用的 chunk
     */
    public List<Chunk> split(String markdownContent) {
        // 预处理：统一换行符，重置临时状态
        String content = markdownContent.replace("\r\n", "\n").replace("\r", "\n");
        resetTempState();

        // 解析为 AST
        Node document = markdownParser.parse(content);

        // 遍历 AST 节点，按语法边界分块
        document.accept(new AbstractVisitor() {
            @Override
            public void visit(Heading heading) {
                handleHeading(heading);
                super.visit(heading);
            }

            @Override
            public void visit(FencedCodeBlock codeBlock) {
                handleCodeBlock(codeBlock);
                super.visit(codeBlock);
            }

            @Override
            public void visit(CustomBlock table) {
                if(table instanceof TableBlock){
                    handleTable((TableBlock) table);
                }
                super.visit(table);
            }

            @Override
            public void visit(Image image) {
                handleImage(image);
                super.visit(image);
            }

            @Override
            public void visit(Paragraph paragraph) {
                handleParagraph(paragraph);
                super.visit(paragraph);
            }

            @Override
            public void visit(ListItem listItem) {
                handleListItem(listItem);
                super.visit(listItem);
            }
        });

        // 保存最后一段临时内容
        saveTempContent();

        return chunkResult;
    }

    /**
     * 处理标题：更新章节，保存临时内容
     */
    private void handleHeading(Heading heading) {
        //saveTempContent(); // 先保存之前的普通内容

        // 提取标题文本和层级
        String headingText = getNodeText(heading);
        int level = heading.getLevel();
        // 更新章节（如 "根文档 > 1. 功能 > 1.1 介绍"）
        updateChapter(headingText, level);

        // 标题单独分块
        String content = "#".repeat(level) + " " + headingText;
        tempContent.append(content).append("\n");
//        addChunk(ChunkType.HEADING, content);
    }

    /**
     * 处理代码块：强制单独分块
     */
    private void handleCodeBlock(FencedCodeBlock codeBlock) {
        saveTempContent();

        String lang = codeBlock.getInfo() != null ? codeBlock.getInfo() : "unknown";
        String code = codeBlock.getLiteral();
        String content = "```" + lang + "\n" + code + "\n```";

        addChunk(ChunkType.CODE_BLOCK, content, "语言：" + lang);
    }

    /**
     * 处理表格：强制单独分块
     */
    private void handleTable(TableBlock table) {
        saveTempContent();

        String tableContent = getNodeText(table); // 保留表格 Markdown 语法
        addChunk(ChunkType.TABLE, tableContent);
    }

    /**
     * 处理图片：单独分块
     */
    private void handleImage(Image image) {
        saveTempContent();

//        String alt = image.getAlt() != null ? image.getAlt() : "";
        String url = image.getDestination();
        String title = image.getTitle();
        String content = "![" + title + "](" + url + ")";

        addChunk(ChunkType.IMAGE, content, "链接：" + url);
    }

    /**
     * 处理段落：临时拼接，后续长度控制
     */
    private void handleParagraph(Paragraph paragraph) {
        String paraText = getNodeText(paragraph) + "\n\n";
        tempContent.append(paraText);
        splitIfOverLength(); // 检查是否超长
    }


    // ------------------------------ 辅助方法 ------------------------------

    /**
     * 处理列表项：临时拼接
     */
    private void handleListItem(ListItem listItem) {
        // 判断有序/无序列表
        Node parent = listItem.getParent();
        String prefix = (parent instanceof OrderedList) ?
                ((OrderedList) parent).getStartNumber() + ". " : "- ";

        String itemText = prefix + getNodeText(listItem) + "\n";
        tempContent.append(itemText);
        splitIfOverLength(); // 检查是否超长
    }

    /**
     * 计算文本的 Token 数
     */
    private int countTokens(String text) {
        return tokenEncoder.countTokens(text);
    }

    /**
     * 提取节点的 Markdown 文本（保留语法）
     */
    private String getNodeText(Node node) {
        StringBuilder sb = new StringBuilder();
        node.accept(new AbstractVisitor() {
            @Override
            public void visit(Text text) {
                sb.append(text.getLiteral());
                super.visit(text);
            }

            @Override
            public void visit(Link link) {
                sb.append("[").append(getNodeText(link.getFirstChild())).append("](").append(link.getDestination()).append(")");
            }

            @Override
            public void visit(Image image) {
                sb.append("![").append(image.getTitle()).append("](").append(image.getDestination()).append(")");
            }

            @Override
            public void visit(CustomBlock table) {
                if(table instanceof TableBlock){
                    TableBlock tableBlock = (TableBlock) table;
                    // 特殊处理表格：遍历行和单元格
                    TableBlockTextExtractor tableBlockTextExtractor = new TableBlockTextExtractor();
                    String tableMarkdown = tableBlockTextExtractor.extractTableMarkdown(tableBlock);
                    sb.append(tableMarkdown);
                }

            }
        });
        return sb.toString().trim();
    }

    /**
     * 更新章节标题（按层级嵌套）
     */
    private void updateChapter(String headingText, int level) {
        if (level == 1) {
            currentChapter = headingText;
        } else {
            String[] parts = currentChapter.split(" > ");
            List<String> newParts = new ArrayList<>(Arrays.asList(parts).subList(0, level - 1));
            newParts.add(headingText);
            currentChapter = String.join(" > ", newParts);
        }
    }

    /**
     * 保存临时内容（段落/列表），并进行长度控制
     */
    private void saveTempContent() {
        if (tempContent.length() == 0) return;

        String content = tempContent.toString().trim();
        if (content.isEmpty()) {
            tempContent.setLength(0);
            return;
        }

        if (countTokens(content) <= MAX_TOKEN_PER_CHUNK) {
            addChunk(ChunkType.CONTENT, content);
            tempContent.setLength(0);
        } else {
            String s = splitBySentence(content);// 超长则按句子拆分
            tempContent.setLength(0);
            tempContent.append(s);
        }

    }

    /**
     * 检查临时内容是否超长，需要拆分
     */
    private void splitIfOverLength() {
        if (countTokens(tempContent.toString()) > MAX_TOKEN_PER_CHUNK) {
            saveTempContent();
        }
    }

    /**
     * 按句子拆分长文本（支持中英文句号）
     */
    private String splitBySentence(String content) {
        List<String> sentences = new ArrayList<>();
        Pattern pattern = Pattern.compile("([^。.]+[。.])");
        pattern.matcher(content).results().forEach(match -> sentences.add(match.group(1)));

        StringBuilder current = new StringBuilder();
        for (String sentence : sentences) {
            int newCount = countTokens(current + sentence);
            if (newCount > MAX_TOKEN_PER_CHUNK - CHUNK_OVERLAP) {
                if (current.length() > 0) {
                    addChunk(ChunkType.CONTENT, current.toString().trim());
                    // 保留重叠部分
                    current = new StringBuilder();
                }
            }
            current.append(sentence);
        }
        return current.toString().trim();

//        if (current.length() > 0) {
//            addChunk(ChunkType.CONTENT, current.toString().trim());
//        }
    }

    /**
     * 获取内容末尾的重叠部分
     */
    private String getOverlap(String content) {
        if (countTokens(content) <= CHUNK_OVERLAP) {
            return content;
        }
//        IntArrayList tokens = tokenEncoder.encode(content);
//
//        List<String> overlapTokens = tokens.subList(tokens.size() - CHUNK_OVERLAP, tokens.size());
//        return tokenEncoder.decode(overlapTokens);
        return content;
    }

    /**
     * 添加分块到结果（含元信息）
     */
    private void addChunk(ChunkType type, String content, String... extraMeta) {
        int tokenCount = countTokens(content);
        StringBuilder meta = new StringBuilder();
        meta.append("【章节：").append(currentChapter).append("】")
                .append("【类型：").append(type).append("】")
                .append("【Token数：").append(tokenCount).append("】");
        for (String extra : extraMeta) {
            meta.append("【").append(extra).append("】");
        }

        chunkResult.add(new Chunk(type, meta.toString(), content, tokenCount));
    }

    /**
     * 重置临时状态
     */
    private void resetTempState() {
        currentChapter = "根文档";
        chunkResult.clear();
        tempContent.setLength(0);
    }

    // ------------------------------ 分块模型 ------------------------------
    public enum ChunkType {
        HEADING, CODE_BLOCK, TABLE, IMAGE, CONTENT
    }

    public static class Chunk {
        private final ChunkType type;
        private final String meta;
        private final String content;
        private final int tokenCount;

        public Chunk(ChunkType type, String meta, String content, int tokenCount) {
            this.type = type;
            this.meta = meta;
            this.content = content;
            this.tokenCount = tokenCount;
        }

        // Getter 方法
        public ChunkType getType() {
            return type;
        }

        public String getMeta() {
            return meta;
        }

        public String getContent() {
            return content;
        }

        public int getTokenCount() {
            return tokenCount;
        }

        @Override
        public String toString() {
            return meta + "\n" + content + "\n" + "-".repeat(60);
        }
    }
}