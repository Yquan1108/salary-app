# phase-1-spec.md

## Phase 1 Name
项目骨架 + 引导页 + 首页 + 员工/工地档案基础

## Goal
搭建一个可运行的离线 Android App 基础版本，完成首页导航、引导页和基础档案管理，为后续记工、图片、工资统计功能打底。

## In Scope
1. App 工程初始化
2. 主题、字号体系、基础组件封装
3. 首次启动引导页
4. 首页
5. 员工档案管理
6. 工地档案管理
7. 本地数据库初始化
8. 基础设置：字体大小调节

## Out of Scope
1. 做工记录
2. 图片上传
3. 工资统计
4. 图片搜索
5. Excel 导出

## User Stories
1. 作为首次使用者，我希望看到简单的引导页，知道这个 App 是干什么的。
2. 作为用户，我进入首页后能一眼看懂四个主要入口。
3. 作为用户，我可以先录入常用员工，后续不用重复输入。
4. 作为用户，我可以先录入常用工地，后续直接选择。
5. 作为视力不太好的用户，我可以调大字体。

## Functional Spec

### 1. App 启动逻辑
- 首次进入 App：显示引导页
- 非首次进入：直接进入首页
- 使用本地持久化保存是否已完成引导

### 2. 引导页
共 3 页，可左右滑动，最后一页有“进入记工”按钮。

页面内容建议：
- 第 1 页：记工记录
  - 记录每天谁做工、做了多久、应发多少
- 第 2 页：工地照片
  - 按日期和工地保存照片，后续方便查找
- 第 3 页：工资统计
  - 按员工和时间筛选，一键导出工资表

要求：
- 大标题
- 大图标/插图占位
- 文案简短
- 有页码指示器
- 最后一页点击进入首页

### 3. 首页
首页显示 4 个主入口卡片：
- 做工记录
- 上传照片
- 工资统计
- 图片搜索

同时提供两个次级入口：
- 员工管理
- 工地管理

以及一个设置入口：
- 字体大小

首页要求：
- 字体大
- 卡片清晰
- 每个入口有图标、标题、简短说明
- 支持纵向滚动，适配小屏

### 4. 员工管理
页面能力：
- 查看员工列表
- 新增员工
- 编辑员工
- 删除员工

字段：
- 姓名：必填
- 默认工价：选填
- 电话：选填
- 备注：选填

规则：
- 姓名不能为空
- 姓名支持去重提醒，但允许用户确认后继续保存
- 删除员工前弹确认框
- 若该员工后续被做工记录引用，历史记录仍保留 snapshot，不受影响

### 5. 工地管理
页面能力：
- 查看工地列表
- 新增工地
- 编辑工地
- 删除工地

字段：
- 工地名称：必填
- 地址或别名：选填
- 备注：选填

规则：
- 工地名称不能为空
- 支持重复提醒
- 删除前二次确认

### 6. 设置页面
至少包含：
- 字体大小调节
  - 标准
  - 偏大
  - 超大

要求：
- 修改后全局生效
- 首页和列表明显可见变化

## Data Spec

### WorkerEntity
- id: Long
- name: String
- defaultWage: String?
- phone: String?
- note: String?
- createdAt: Long
- updatedAt: Long

### SiteEntity
- id: Long
- siteName: String
- addressOrAlias: String?
- note: String?
- createdAt: Long
- updatedAt: Long

### AppPreference
- hasCompletedOnboarding: Boolean
- fontScaleLevel: Enum(standard, large, extra_large)

## UI States

### 员工列表
- 空态：暂无员工，点击下方按钮新增员工
- 正常态：列表展示姓名、默认工价、电话
- 删除确认态
- 编辑弹窗/页面态

### 工地列表
- 空态：暂无工地，点击下方按钮新增工地
- 正常态：列表展示工地名、地址或别名
- 删除确认态
- 编辑弹窗/页面态

## Suggested Package Structure
- ui/
  - onboarding/
  - home/
  - worker/
  - site/
  - settings/
  - common/
- data/
  - local/db/
  - local/dao/
  - local/entity/
  - repository/
- domain/
  - model/
  - usecase/
- navigation/
- core/
  - ui/
  - util/
  - preference/

## Acceptance Criteria
1. 首次启动进入引导页，点击“进入记工”后进入首页。
2. 再次启动 App 直接进入首页。
3. 首页可进入员工管理、工地管理、设置页面。
4. 员工增删改查可用，数据重启后仍存在。
5. 工地增删改查可用，数据重启后仍存在。
6. 字体大小设置生效，并能持久化。
7. 所有删除操作都有确认弹窗。

## QA Checklist
1. 首次安装是否进入引导页。
2. 关闭 App 重开后是否跳过引导页。
3. 员工姓名为空时是否阻止保存。
4. 工地名称为空时是否阻止保存。
5. 删除员工/工地时取消按钮是否有效。
6. 字体切换后首页文字是否明显变大。
7. 旋转屏幕或进后台再回来，页面状态是否正常。

## Codex Task Prompt
请先完成 Phase 1。
要求：
1. 使用 Kotlin + Jetpack Compose + Room + MVVM。
2. 先搭建工程目录和导航结构。
3. 再实现 onboarding、home、worker、site、settings。
4. 所有页面文案使用中文。
5. 输出变更文件列表、核心实现说明、已知限制、下一步建议。