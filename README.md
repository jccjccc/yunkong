# 三角洲云控 · DashaControl

一个面向 **DASHABI 三角洲账号信息平台开放 API** 的 Android 客户端，用原生 Kotlin + Jetpack Compose 编写。
底部只有两个主页面 —— **设备**、**账号** —— 覆盖开放 API 的全部 24 个接口：总览、详情、实时状态、资产、设施、截图、暂停/恢复、制造、出售、定时、备注、同步。

---

## 1. 功能一览

### 设备页

- **运行总览**：`GET /status/summary`，设备数 / 账号数 / 运行中 / 等待下一轮 / 已停止 / 无障碍关闭 / 未知
- **设备卡片**：状态灯（运行中会呼吸闪烁）、机型、账号数、当前任务、错误信息、定时状态、最近更新时间
- 每 30 秒自动刷新，支持手动刷新、下拉不打断操作
- 点击进入 **设备详情**

### 设备详情

- 基础信息 + 站内状态快照（`GET /devices/{device_code}`）
- **读取实时状态**（`GET /devices/{device_code}/status`）
- 设备控制：开启定时 / 关闭定时 / 停止脚本（可勾选同时关定时）/ 同步账号
- 出售任务状态（`GET .../sell/status`），出售后自动轮询直到结束
- 该设备账号列表，可勾选、全选、**批量出售**（`POST /devices/{device_code}/sell`）
- 点击账号进入账号详情

### 账号页

- 全部账号平铺（`GET /accounts`，自动翻页），支持**本地即时搜索**（昵称 / 游戏 ID / 账号 / 备注 / 设备名），输入即出结果，不再逐次请求接口
- 状态筛选（全部 / 正常 / 暂停 / 异常 / 设备停止 / 未知）与设备筛选，均带数量
- 排序：默认按**哈夫币从多到少**，可切换行动等级 / 体力 / 总资产，点同一项切换升序「↑」与降序「↓」（体力按 AI 识别的训练中心等级排序，未登记的排最后）
- 标题栏右侧实时显示当前筛选 / 搜索结果的哈夫币合计（单位 W）
- 切换设备、状态、排序或搜索词时，列表自动回到顶部
- 账号卡片：**备注优先作为标题**（上游备注，无备注时回退为游戏名），游戏名移到底部；登录方式后面跟账号（QQ 号），游戏 ID 单独用标签展示（不提供复制）；只展示设备、状态、总资产、**哈夫币（按万换算，单位 W，如 4122.3W）**、行动等级，设施等级不在列表页展示；**不加载账号截图**
- 排序行最右侧的图标可切换**缩略模式**：双列卡片，仅展示备注 / 总资产 / 哈夫币 / 行动等级 / 体力负重，点击卡片进入详情（无暂停、出售按钮）；模式选择保存在本机，重启后保留
- 快捷操作：暂停（二次确认）/ 恢复、出售（二次确认）、进入详情；制造按钮不在列表页展示（详情页仍可用）
- 每 60 秒自动刷新

### 账号详情

- 完整信息（`GET .../accounts/{account_code}`）
- 设施进度：技术中心 / 工作台 / 制药台 / 防具台，等级条与完成时间
- 三张账号截图（个人信息 / 特勤处 / 仓库），带拍摄时间，点击全屏查看，图片请求自动携带鉴权头
- 备注编辑（`PUT .../note`，200 字上限，仅写网站本地）
- 账号操作：暂停 / 恢复 / 立即制造 / 出售（二次确认 + 状态轮询）
- 定时暂停：查看本设备计划（`GET /pause-schedules`）、新建（`POST .../pause-schedules`）、取消（`DELETE /pause-schedules/{id}`）
- 接口字段区展示 `account_code`、`device_code`、`status`、`raw_status` 等原始值

### 体力 / 负重（AI 识别）

- 设置里新增「体力 / 负重（AI 识别）」：填写 DeepSeek API Key（默认模型 `deepseek-flash`，地址 `https://api.deepseek.com`）
- 设置里分两个按钮：**「识别体力 / 负重」**只读特勤处截图，**「识别 AW 子弹」**只读仓库截图，互不影响，可分别重跑
- **训练中心等级 = 体力，靶场等级 = 负重，.338AP 子弹 = AW**，结果保存在本机：账号卡片和缩略模式显示「体力 / 负重 / X发AW子弹」，账号详情页有独立的「体力 / 负重 / AW」卡片（含识别时间）
- 默认只识别未登记的账号（可取消勾选强制全部重识别），识别过程显示进度、支持取消，3 并发
- 图片接口限流 120 次/分钟：识别时下载限速到约 85 次/分钟，遇到 429/5xx 会按 Retry-After 自动重试
- 排序行新增「AW」选项，按子弹数量从多到少排序（未登记排最后）
- 每个账号 2 张截图，每张约 1000 tokens，100 个账号约 20 万 tokens，费用很低

### 异常账号

- 底部导航中间新增「异常」页签，有异常账号时图标/文字变红并显示数量角标
- 判定规则：云端状态异常 · 接口字段 raw_status 为「蓝脸 / 需要验证 / 人脸验证」· 哈夫币低于 100 万 · 设施进度缺少完成时间或任一设施超时 10 分钟以上（仅账号状态正常时判断）
- 只在该页归类展示，**不影响账号总览的「异常」筛选**（总览仍按接口返回的 status 判断）
- 卡片显示命中的具体原因（如「哈夫币不足」「防具台 无完成时间」），点击进入账号详情
- **自动出售**：哈夫币低于 100 万的账号，每 2 小时（偶数整点）自动提交一次出售请求；异常页卡片显示上次请求时间与结果（已提交 / 失败原因）
- 新增异常账号、账号异常解除时都会发送系统通知（高重要性通道，手机顶部悬浮横幅），点击通知直接打开异常页
- Android 13+ 首次启动会请求通知权限；未授权则不会提醒（需在系统设置里允许通知）

### 主题

- 设置 →「主题」可切换：**跟随系统 / 暗色 / 亮色**，默认暗色
- 选择保存在本机，切换后立即生效；状态栏、导航栏图标会跟随亮暗自动调整

### 连接设置

- 当前 Key 身份与 Scope（`GET /me`）
- 修改 API Key / Base URL，保存并立即测试连通性，可恢复默认
- DeepSeek API Key / 地址 / 模型（用于体力负重识别，与平台 Key 分开保存）
- Key 仅保存在本机 SharedPreferences

---

## 2. API 覆盖

| 接口 | 方法 | 使用位置 |
| --- | --- | --- |
| `/me` | GET | 连接设置 · 身份与权限 |
| `/devices` | GET | 设备页列表 |
| `/devices/{device_code}` | GET | 设备详情 |
| `/devices/{device_code}/status` | GET | 设备详情 · 实时状态 |
| `/devices/{device_code}/accounts` | GET | 设备详情 · 账号列表 |
| `/devices/{device_code}/accounts/{account_code}` | GET | 账号详情 |
| `/accounts` | GET | 账号页总览 / 搜索 |
| `/devices/{device_code}/accounts/{account_code}/images` | GET | 账号详情 · 截图元数据 |
| `/devices/{device_code}/accounts/{account_code}/images/{kind}` | GET | 截图缩略图 / 全屏预览 |
| `/devices/{device_code}/accounts/{account_code}/pause` | POST | 账号卡片 / 账号详情 |
| `/devices/{device_code}/accounts/{account_code}/resume` | POST | 账号卡片 / 账号详情 |
| `/devices/{device_code}/accounts/{account_code}/run-production` | POST | 账号卡片 / 账号详情 |
| `/devices/{device_code}/accounts/{account_code}/sell` | POST | 账号卡片 / 账号详情 |
| `/devices/{device_code}/sell` | POST | 设备详情 · 批量出售 |
| `/devices/{device_code}/sell/status` | GET | 设备详情 / 账号详情 · 轮询 |
| `/devices/{device_code}/timer/enable` | POST | 设备详情 |
| `/devices/{device_code}/timer/disable` | POST | 设备详情 |
| `/devices/{device_code}/stop` | POST | 设备详情 · 可带 `disable_timer` |
| `/devices/{device_code}/sync` | POST | 设备详情 · 顶栏 / 控制卡 |
| `/devices/{device_code}/accounts/{account_code}/note` | PUT | 账号详情 · 备注 |
| `/pause-schedules` | GET | 账号详情 · 定时暂停列表 |
| `/devices/{device_code}/accounts/{account_code}/pause-schedules` | POST | 账号详情 · 新建定时暂停 |
| `/pause-schedules/{id}` | DELETE | 账号详情 · 取消定时暂停 |
| `/status/summary` | GET | 设备页 · 运行总览 |

接口返回的 `error.code / message / request_id`、`Retry-After`、分页 `meta.pagination` 均被解析并在界面提示。

---

## 3. 技术栈

| 层 | 选型 |
| --- | --- |
| 语言 / UI | Kotlin 1.9.24 + Jetpack Compose（Material 3，Compose BOM 2024.06） |
| 架构 | 单 Activity + Navigation Compose + ViewModel + Repository |
| 网络 | OkHttp 4.12（拦截器统一注入 `Authorization` 与 `User-Agent`） |
| 序列化 | kotlinx.serialization 1.6.3，泛型 `Envelope<T>` + 容错解析（忽略未知字段） |
| 图片 | Coil 2.6，复用同一个 OkHttpClient（图片同样带鉴权头） |
| AI 识别 | DeepSeek `deepseek-flash` 视觉模型（OpenAI 兼容 Chat Completions，base64 图片输入） |
| 最低版本 | Android 8.0（API 26），targetSdk 34 |
| 构建 | AGP 8.5.2 / Gradle 8.7 / JDK 17 |

---

## 4. 构建与安装

### 直接安装

```
app/build/outputs/apk/release/app-release.apk
app/build/outputs/apk/debug/app-debug.apk
```

两个 APK 都可以直接装到手机（允许「安装未知来源应用」）。`release` 版使用 `keystore/release.jks` 签名（口令 `dashacontrol`），`debug` 版包名带 `.debug` 后缀，可与正式版共存。

### 从源码构建

前置：JDK 17、Android SDK（Platform 34、Build-Tools 34.0.0）。`local.properties` 中的 `sdk.dir` 请改成自己的 SDK 路径。

```bash
# Windows
gradlew.bat assembleRelease

# macOS / Linux
./gradlew assembleRelease
```

不想把 Key 打进安装包（适合分享给别人）：

```bash
gradlew.bat assembleRelease -PembedDefaultKey=false
```

产物 `versionName` 为 `1.0.0-nokey`、`DEFAULT_API_KEY` 为空字符串；首次启动会直接弹出「连接设置」，设备页和账号页会显示「还没有配置 API Key」的引导卡片，粘贴 Key 保存后即可正常加载。

首次构建会从 Google Maven / Maven Central 下载依赖。若网络较慢，可在 `settings.gradle.kts` 把 `google()`、`mavenCentral()` 换成国内镜像；`gradle/wrapper/gradle-wrapper.properties` 的 `distributionUrl` 也可以换成 `https://mirrors.cloud.tencent.com/gradle/gradle-8.7-bin.zip`。

### 运行接口测试

```bash
gradlew.bat testDebugUnitTest
```

`ApiLiveTest` 会对真实服务发起 **只读** 请求，验证鉴权头、User-Agent、分页与全部 DTO 解析，并验证错误信封。不会执行任何写操作。可通过环境变量覆盖密钥：

```bash
set DASHA_API_KEY=zh_live_xxx
set DASHA_BASE_URL=https://a.12123.pro/index.php/open/v1
```

---

## 5. 目录结构

```
DashaControl/
├─ app/src/main/java/com/dashabi/control/
│  ├─ DashaApp.kt                 Application + 依赖容器 + Coil ImageLoader
│  ├─ MainActivity.kt             单 Activity，edge-to-edge
│  ├─ data/
│  │  ├─ Models.kt                DTO（Device / Account / Summary / Envelope …）
│  │  ├─ ApiResult.kt             统一结果与错误
│  │  ├─ SettingsStore.kt         Key / BaseUrl 持久化
│  │  ├─ Repository.kt            领域接口，自动翻页
│  │  └─ remote/DashaApi.kt       OkHttp 客户端、鉴权拦截器、错误解析
│  └─ ui/
│     ├─ nav/DashaRoot.kt         底部两个主页面 + 详情路由
│     ├─ screens/                 设备页 / 账号页 / 两个详情页 / 设置面板
│     ├─ components/Common.kt     卡片、状态胶囊、统计块、JSON 表格等
│     ├─ vm/                      ViewModel 与工厂
│     ├─ theme/Theme.kt           深色战术配色
│     └─ Format.kt                状态映射、时间格式化、JSON 展平
├─ app/src/test/…/ApiLiveTest.kt  真实接口只读测试
└─ keystore/release.jks           演示用签名
```

---

## 6. 安全说明

- 内置 Key 的安装包中，Key 以 `BuildConfig.DEFAULT_API_KEY` 形式打进 APK，**任何拿到 APK 的人都可以反编译提取**。要分发给别人请使用 `dist/DashaControl-1.0.0-nokey-release.apk`（或用 `-PembedDefaultKey=false` 自行构建），它不含任何 Key，首次启动要求手动填写。
- Key 只写入本机 `SharedPreferences`，不会上传到任何第三方；App 只与配置的 Base URL 通信（默认 `https://a.12123.pro`）。
- 所有写操作（暂停、恢复、制造、出售、停止、定时、备注）都有二次确认或明确提示；出售不可撤销。
- 接口有每分钟限流，频繁刷新可能返回 429，界面会提示稍后再试。
- **隐私提示**：使用「体力 / 负重 AI 识别」时，账号的「特勤处」截图会被上传到所配置的 DeepSeek（或兼容）接口用于识别；如不希望上传截图，请不要使用该功能。

---

## 7. 已验证

- `assembleDebug` / `assembleRelease` 编译通过，Lint 无 Error，APK 已用 `apksigner` 校验签名。
- **9 项真实接口测试**全部通过：身份、汇总、设备列表/详情/实时状态、设备账号、账号总览、账号详情、图片元数据、定时计划、出售状态、无效 Key 的 401 错误信封。
- **4 项 Robolectric UI 测试**全部通过：两个主页面完成 Compose 组合、底部导航双向切换、设备详情与账号详情可组合、中文文案 UTF-8 校验。
- 测试只读，不会改变线上账号状态。

> 说明：构建机上没有连接 Android 真机或模拟器（CPU 支持 VT-x，但未启用 WHPX/AEHD，且不适合在你的机器上安装驱动/重启），因此 APK 未实际安装到手机运行；界面通过 Robolectric 在 JVM 上真实组合验证。建议安装后先打开「连接设置 → 保存并测试」确认连通性。

---

## 8. 交付物

| 文件 | 说明 |
| --- | --- |
| `dist/DashaControl-1.0.0-release.apk` | 正式签名包，10.7 MB，可直接安装（内置 Key） |
| `dist/DashaControl-1.0.0-nokey-release.apk` | 不含内置 Key 的签名包，首次启动引导填写 Key，适合分享 |
| `dist/DashaControl-1.0.0-debug.apk` | 调试包（包名带 `.debug`，可与正式版共存） |
| `dist/DashaControl-1.0.0-src.zip` | 源码压缩包（不含 build 产物） |

---

## 9. 已知取舍

- 开放 API 的 `sell/status`、`pause-schedules` 响应字段未在 OpenAPI 中声明，客户端对未知字段做通用渲染（键值表），不会因字段变化崩溃。
- 出售提交后按 2.5 秒间隔轮询，最长约 3 分钟；若上游一直返回 `active=true`，界面会保持「跟踪中」。
- 账号截图较大，列表只加载个人信息一张，其余在详情页按需加载。
- 开放 API 不返回体力 / 负重字段：这两项由 DeepSeek 识别每个账号的「特勤处」截图得到（训练中心 = 体力，靶场 = 负重）。识别精度依赖模型，个别账号可能需要重新识别；未登记的账号显示「—」；体力可参与排序（未登记排最后），负重不参与排序。
