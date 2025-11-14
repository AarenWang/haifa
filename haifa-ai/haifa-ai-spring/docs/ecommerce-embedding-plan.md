# Spring AI 电商搜索推荐 Embedding 技术方案

## 1. 背景与目标
- 结合 Spring AI 能力，构建一个支持语义搜索与个性化推荐的电商示例应用。
- 使用大模型生成的文本 embedding 表示商品、用户行为与查询，实现语义级匹配。
- 向量数据库采用 PostgreSQL + pgvector 扩展，统一存储商品、用户、会话向量。

## 2. 文章要点提炼（Embedding 在电商的实践）
- **语义搜索与召回**：Embedding 将商品标题、描述、标签转换成向量，使得相似语义可以被召回，解决关键词不匹配问题。
- **推荐与相似商品发现**：通过商品向量之间的近邻搜索，快速找到替代品、互补品，用于关联推荐、猜你喜欢等场景。
- **用户画像与个性化**：将用户历史行为（浏览、加购、购买、搜索词）编码成用户向量，实现用户与商品间的语义匹配，提高个性化推荐精准度。
- **多模态融合**：文章强调需要结合图像、文本等多模态信息，构建更丰富的商品向量，提高长尾商品的召回与冷启动表现。
- **实时更新与反馈**：Embedding 系统需要支持增量更新与在线学习，利用用户反馈不断优化向量表示，保证推荐与搜索结果的新鲜度。

## 3. 系统范围
- 提供一个面向商品搜索的 REST API，支持输入自然语言查询返回 Top-K 商品。
- 提供一个个性化推荐 API，根据用户 ID 或最近行为返回候选商品列表。
- 支持基于大模型（OpenAI、DashScope、文心等兼容 Spring AI）的文本向量生成。
- 向量存储使用 pgvector，结合 PostgreSQL 元数据，实现过滤、排序与向量检索的混合查询。

## 4. 技术架构
1. **数据准备流水线**：
   - 解析商品主数据（SPU/SKU、类目、价格、标签、媒体）生成统一 JSON。
   - 收集用户行为日志（曝光、点击、加购、购买、搜索查询）。
   - 构建提示模板，将商品或用户行为拼接为适合大模型的 embedding 文本。
2. **Embedding 服务**：
   - 使用 Spring AI `EmbeddingClient` 封装大模型调用。
   - 支持批量生成商品向量、用户向量，并定义重试、限流策略。
3. **向量存储**：
   - PostgreSQL 14+，安装 `pgvector` 扩展。
   - 表设计示例：`item_embedding`、`user_embedding`、`search_log_embedding`，包含 `id`、`vector`、`metadata`、`updated_at`。
4. **查询服务**：
   - Spring Boot WebFlux / MVC，提供 `/search/semantic`、`/recommendations/user/{id}` 等端点。
   - 查询流程：生成查询向量 → pgvector 相似度检索（`<->` 运算符）→ 结合结构化过滤（库存、价格、类目）→ 排序 → 返回结果。
5. **评估与监控**：
   - 离线指标：NDCG、Recall@K、CTR 提升。
   - 在线监控：平均响应时延、向量写入耗时、模型调用错误率。

## 5. 数据来源与准备
| 数据类型 | 来源 | 处理方式 | 向量化内容 |
| --- | --- | --- | --- |
| 商品主数据 | 自建 CSV/JSON 或示例数据库 | 清洗脏数据、合并多语种描述 | 商品名称、卖点、长描述、属性标签 |
| 商品多模态信息 | 商品图片（URL）、短视频脚本 | 采用 CLIP 等模型提取图片向量，或将视觉描述转文本 | 图文混合向量（可先实现文本向量） |
| 用户行为日志 | Web/App 埋点 | 提取最近 N 次搜索词、点击、加购；构造提示模板 | 用户兴趣向量、搜索 Query 向量 |
| 运营配置 | 热门榜单、主题活动 | 作为过滤条件或权重增强 | 无需向量化 |

> Demo 阶段可使用公开电商数据集（如 Amazon Reviews、阿里天池商品数据）或自定义样例数据。

## 6. Embedding 策略
- **模型选择**：优先使用支持中文语义的向量模型（OpenAI text-embedding-3-large、阿里 DashScope text-embedding-v2、百度文心向量等）。
- **向量维度**：结合模型输出（1k~3k 维），pgvector 支持最大 16k 维；依据精度与存储权衡决定。
- **标准化**：将向量归一化以使用余弦相似度；pgvector `<#>` 可做余弦距离。
- **批处理与缓存**：商品数据采用离线批量生成；用户向量结合流式任务（Kafka → Spring Batch → pgvector）。
- **更新策略**：
  - 商品：每日离线批处理，增量更新新增/变更 SKU。
  - 用户：用户行为触发增量更新（近实时），支持 Redis 缓存最新向量。
  - 查询：运行时实时生成，可缓存热点查询向量。

## 7. 核心流程
1. **离线商品向量生成**：
   - Spring Batch 读取商品数据 → 调用 EmbeddingClient → 写入 pgvector。
2. **在线语义搜索**：
   - 接收查询 → 生成查询向量 → `SELECT ... ORDER BY embedding <-> :query_vector LIMIT K` → 返回商品详情。
3. **个性化推荐**：
   - 从 pgvector 读取用户向量 → 计算与商品向量近邻 → 结合业务规则（库存、价格区间、品牌排除）输出。
4. **推荐解释**：
   - 调用 LLM 根据匹配向量的元数据生成推荐理由文案。

## 8. 接口与组件设计（草案）
- `EmbeddingService`
  - `float[] embedItem(ItemDocument doc)`
  - `float[] embedUser(UserProfile profile)`
  - `float[] embedQuery(String query)`
- `VectorRepository`
  - `void upsertItemVector(ItemVector vector)`
  - `List<ItemVector> searchByVector(float[] vector, Filter filter, int k)`
  - `List<ItemVector> findSimilarForUser(String userId, int k)`
- `SearchController`
  - `GET /api/search/semantic?q=xxx&k=10`
- `RecommendationController`
  - `GET /api/recommendations/user/{userId}?k=10`
- `NarrationService`
  - 利用 LLM 基于匹配商品元数据生成自然语言解释。

## 9. 运行与部署建议
- PostgreSQL 安装 `pgvector`：`CREATE EXTENSION IF NOT EXISTS vector;`
- 向量列使用 `vector(1536)` 等定长类型，结合 `ivfflat` 索引优化查询。
- 配置 Spring Boot：
  - 数据源：`spring.datasource.*`
  - Spring AI 客户端：选择模型提供方的 API Key，从配置中心加载。
- 可选：引入 Kafka + Debezium 构建商品数据变更流，保持向量更新实时。

## 10. 交付节奏
1. 方案评审（当前文档）。
2. 数据样例与 pgvector 初始化脚本。
3. Spring AI 模块骨架搭建，封装 EmbeddingClient。
4. 商品向量批量生成脚本与 REST 接口实现。
5. 推荐解释与监控指标完善。

---
待方案确认后，进入编码阶段。
