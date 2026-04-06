# agents.md

## Project
项目名称：记工  
项目类型：Android 离线 App  
目标用户：包工头、工地现场记工人员、对手机不熟悉的中老年用户  
开发目标：帮助用户用最少步骤完成工天记录、工地照片归档、工资统计导出与图片检索。

---

## Global Product Goals
1. 页面简单，操作简单，尽量减少输入步骤。
2. 大字体、大按钮、高对比度，适合中老年用户。
3. 完全离线可用，不依赖登录、服务器、云同步。
4. 所有核心数据安全保存到本地，不使用不透明缓存作为正式存储。
5. 任何导出、下载、保存结果都要让用户清楚知道文件保存到了哪里，并支持点击打开。
6. 所有功能优先保证“可用、稳定、容易理解”，其次再追求复杂交互。

---

## Tech Constraints
- Language: Kotlin
- UI: Jetpack Compose
- IDE: Android Studio
- Min SDK: Android 10
- Image loading: Glide
- Architecture: MVVM
- Local DB: Room
- Local file storage: MediaStore + App-specific persistent storage where appropriate
- Export: .xlsx
- Image compression: local compression before persistent save
- Permissions: only request when needed, and explain clearly in UI

---

## UX Style Rules
遵循以下设计原则：
1. 首页入口清晰，核心功能不超过 4 个主入口。
2. 字体默认偏大，并提供“字体大小调节”设置。
3. 重要操作按钮固定、醒目、文案直白，例如：
   - 新增记工
   - 上传照片
   - 统计工资
   - 搜索图片
4. 列表项信息密度高但不拥挤，适合快速浏览。
5. 删除、导出、下载等高风险操作必须二次确认。
6. 所有空状态页面必须告诉用户“下一步该做什么”。
7. 所有成功操作要给出明确反馈，例如：
   - 已保存
   - 已导出到下载目录
   - 已保存到相册
8. 所有失败操作要提示原因和解决建议。
9. 尽量减少用户键盘输入，优先使用选择器、自动补全、历史记录。
10. 适配单手操作，大按钮、低学习成本。

---

## Core Domain Modules
1. 引导页 Onboarding
2. 首页 Home
3. 做工记录 Work Record
4. 图片上传与管理 Photo Archive
5. 工资统计 Wage Statistics
6. 图片搜索 Photo Search
7. 员工档案 Worker Management
8. 工地档案 Site Management
9. 设置 Settings

---

## Domain Definitions

### Worker
员工档案，至少包含：
- id
- name
- defaultDailyWage 或 defaultUnitPrice（可为空）
- phone（可为空）
- note（可为空）
- createdAt
- updatedAt

### Site
工地档案，至少包含：
- id
- siteName
- addressOrAlias（可为空）
- note（可为空）
- createdAt
- updatedAt

### WorkRecord
做工记录，至少包含：
- id
- workDate
- workerId
- workerNameSnapshot
- siteId（可为空）
- siteNameSnapshot（可为空）
- durationText 或 workHours
- unitPriceText 或 unitPrice
- amount
- remark（可为空）
- createdAt
- updatedAt

说明：
- 金额以手动输入为准
- 工时、工价仅作参考，不自动覆盖金额
- 使用 snapshot 字段防止档案名称后续修改导致历史记录失真

### PhotoBatch
一次上传行为是一条记录，至少包含：
- id
- uploadDate
- workDate
- siteId（可为空）
- siteNameSnapshot
- remark（可为空）
- photoCount
- createdAt
- updatedAt

### PhotoItem
单张照片，至少包含：
- id
- batchId
- localPath or uri
- thumbPath or derived preview strategy
- originalFileName
- width
- height
- sizeBytes
- createdAt

---

## Agent Roles

### 1. Product Planner Agent
职责：
- 把需求拆成阶段目标
- 保证 MVP 范围清晰
- 为每个阶段提供验收标准
- 避免过度设计

输出要求：
- 每个阶段说明目标、范围、非目标、验收标准
- 优先考虑老年用户可用性

### 2. Android Architect Agent
职责：
- 设计模块边界、路由、数据流
- 选定 Room、Repository、ViewModel、Compose UI 分层
- 统一文件存储、导出、权限策略

输出要求：
- 所有 spec 必须包含建议目录结构
- 所有模块必须可独立测试
- 严禁把业务逻辑直接堆进 Compose 页面

## UX Style Rules
整体视觉目标：简洁、现代、清爽、轻盈，避免老旧安卓工具风。不要做成灰扑扑、蓝白老式管理系统风格。

### 1. 视觉关键词
- 清爽
- 现代
- 温和
- 明亮
- 有呼吸感
- 卡片化
- 轻拟物弱化版
- 年轻但不花哨
- 适合中老年用户，但不能显得土

### 2. 颜色风格
要求使用“新鲜、流行、耐看”的配色，而不是传统深蓝+灰色管理系统风格。

推荐主色方向：
- 主色：偏清新的绿色 / 青绿色 / 蓝绿色
- 辅色：柔和米白 / 浅灰白 / 淡青色背景
- 强调色：温和橙色或琥珀色，仅用于金额、重要提示、导出成功等重点信息
- 错误色：柔和红，不要过于刺眼

禁止：
- 大面积纯深蓝
- 大面积死灰色
- 高饱和廉价渐变
- 纯黑背景配高亮色的廉价风格
- 按钮和卡片全都一个颜色，导致页面发闷

### 3. 页面层级
- 页面背景不要纯白一整片，建议使用极浅暖白或浅青灰背景
- 核心内容使用白色或近白色卡片承载
- 卡片要有明显留白和圆角
- 通过留白、字号、字重、阴影建立层级，而不是靠重边框
- 少用硬边线，多用轻阴影和块状分区

### 4. 组件风格
- 卡片圆角偏大，建议 16dp ~ 24dp
- 主按钮圆角明显，按钮高度适中，容易点击
- 输入框不要采用老式描边矩形，应更简洁柔和
- 图标风格统一，线性图标优先
- 页面中避免出现密集表格边框，列表项做成“表格信息感 + 卡片视觉”

### 5. 字体与排版
- 标题要有明显层级，不能所有字都一样大
- 主要标题要简洁有力量
- 副标题只负责解释，不要写太长
- 数字信息（金额、条数）要突出
- 每个页面要有足够留白，不能拥挤

### 6. 引导页设计要求
引导页必须体现“现代 App 感”，不能像老式 PPT 翻页说明。
要求：
- 每页有一个醒目的主视觉区域
- 图标/插画用简洁几何风格，不要复杂插图
- 每页一句主标题 + 一句副标题
- 使用轻渐变或柔和色块做背景装饰
- 底部按钮和分页指示器样式统一且现代
- 最后一页“进入记工”按钮要有品牌感

### 7. 首页设计要求
首页要像现代工具类 App，不要像传统后台菜单。
要求：
- 4 个主功能入口做成大卡片
- 每个卡片有独立色彩识别，但整体统一
- 图标、标题、说明文案层次清楚
- 页面顶部可加入欢迎语或简短说明，增加亲和感
- 卡片之间要有呼吸感，不要排得太挤

### 8. 适老化不等于老旧
必须牢记：
- 大字体、大按钮、清晰对比度，是为了适老化
- 但视觉上仍然要现代、清新、精致
- 不能因为照顾中老年用户，就把页面做成十年前安卓风格

### 4. Android Feature Agent
职责：
- 根据 spec 实现具体功能
- 输出 Compose 页面、ViewModel、Repository、Room 实体
- 严格按阶段开发，不跨阶段偷加复杂功能

输出要求：
- 代码可运行
- 可维护
- 组件职责单一
- 优先稳定，再优化

### 5. Storage & Export Agent
职责：
- 负责本地数据库
- 负责图片压缩和持久化
- 负责 Excel 导出
- 负责文件打开、下载、保存路径提示

输出要求：
- 不使用临时缓存作为正式存储
- 文件保存后必须能被用户感知
- 导出和下载完成后必须提供“打开文件”动作

### 6. QA Agent
职责：
- 编写阶段验收 checklist
- 关注边界场景、空数据、权限拒绝、异常恢复
- 优先覆盖真实用户高频场景

输出要求：
- 每个阶段给出手工测试点
- 特别覆盖：
  - 大字体
  - 批量删除
  - 导出成功/失败
  - 相机/相册权限
  - 图片下载到系统相册

---

## Collaboration Rules for Codex
1. 每次只完成当前阶段 spec 范围内的内容。
2. 先生成目录结构，再生成核心数据模型，再生成页面，再补充交互。
3. 每完成一个功能，给出：
   - 变更文件列表
   - 核心实现说明
   - 已知限制
   - 下一步建议
4. 复杂功能先写最小可用版本，再迭代。
5. 不得擅自引入联网能力。
6. 不得擅自引入登录、账号体系、云同步。
7. 不得擅自引入难理解的动画和复杂手势，除非 spec 明确要求。
8. 所有弹窗文案、按钮文案、错误提示要使用中文。
9. 所有本地路径展示要对普通用户友好，例如：
   - 已保存到“下载”
   - 已保存到“相册”
10. 任何涉及删除的数据操作都必须支持确认取消。

---

## Definition of Done
一个阶段完成，必须同时满足：
1. 功能可运行
2. 页面可进入
3. 空态完整
4. 错误提示完整
5. 本地数据可正确保存和读取
6. 手工测试 checklist 通过
7. 不破坏上一阶段已完成能力

---

## Suggested Execution Order
1. Phase 1: 项目骨架 + 引导页 + 首页 + 档案基础
2. Phase 2: 做工记录
3. Phase 3: 图片上传与图片管理
4. Phase 4: 工资统计与 Excel 导出
5. Phase 5: 图片搜索 + 设置 + 收尾优化