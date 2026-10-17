# REA 安装配置与 base.apk 逆向分析实施方案

## 目标简述
在 Windows 环境下为 Antigravity CLI 安装与配置 `morluto/rea` 逆向工程工具链（包含 MCP 伺服程序与技能指令），准备 Android 静态逆向引擎（JADX Headless JAR），并对当前工作区目录下的 `base.apk`（约 38.3MB）进行完整的静态逆向分析，提取包信息、权限配置、核心组件、第三方集成服务及关键业务逻辑，最终在工作区生成结构化分析报告。

## 用户确认事项

> [!IMPORTANT]
> 1. **下载依赖引擎**：REA 进行 Android APK 静态分析需要由官方指定的无头反编译组件 `jadx-headless-mcp-0.7.1-all.jar`（约 30MB）。实施时将从官方 GitHub Release 下载至 `$HOME\.local\share\rea\android\` 并进行 SHA-256 完整性核验。
> 2. **修改 Antigravity 配置**：将在 `C:\Users\29602\.gemini\config\mcp_config.json` 中添加 `rea` 伺服程序配置，并注入 `REA_JADX_MCP_JAR` 环境变量。

## 待商榷问题
- 无阻碍性问题。分析将涵盖组件、权限、网络请求端点及核心业务逻辑。如需对特定业务功能（如录音存储、语音识别接口）进行特别关注，可在审批时直接说明。

## 方案设计与实施步骤

### 阶段一：环境与依赖准备
1. 检查已确认项：
   - Java 环境：已安装 JDK 24.0.1，满足 JDK >= 17 要求。
   - Node.js 环境：已安装 v24.16.0，满足 Node.js >= 22.19 要求。
2. 下载并配置 JADX 无头引擎：
   - 目标路径：`C:\Users\29602\.local\share\rea\android\jadx-headless-mcp-0.7.1-all.jar`
   - 下载地址：`https://github.com/1013503897/jadx-headless-mcp/releases/download/v0.7.1/jadx-headless-mcp-0.7.1-all.jar`
   - 校验值：`6e5eacf500b64292bfb73c49797c1958f6ee44646e43e868039ae7feb573ff75`

### 阶段二：配置 Antigravity CLI 与 REA 工具链
1. 安装技能工作流：
   - 通过 `npx skills add morluto/rea@reverse-engineer-anything -g -y` 安装逆向分析技能。
2. 注册 Antigravity MCP 伺服程序：
   - 执行 `npx -y rea-agents@latest setup --client antigravity --yes`。
   - 在 `C:\Users\29602\.gemini\config\mcp_config.json` 的 `rea` 伺服程序 `env` 节点中配置 `REA_JADX_MCP_JAR` 与 `JAVA_HOME`。
3. 执行就绪验证：
   - 执行 `npx -y rea-agents@latest doctor --client antigravity --skill --json`，确认伺服程序与技能均处于就绪状态。

### 阶段三：执行 base.apk 静态逆向分析
1. **包体结构与清单解析**：
   - 运行 `rea inspect-android-package "D:\github-repository\smart-record\base.apk"`。
   - 提取应用包名、VersionCode、VersionName、目标 SDK 版本。
   - 提取申请的系统权限（如录音权限、存储权限、网络权限）。
   - 提取四大组件清单（Activity、Service、BroadcastReceiver、ContentProvider）以及入口 Activity。
2. **类库与框架检索**：
   - 运行 `rea search-android-classes "D:\github-repository\smart-record\base.apk"`。
   - 识别应用架构主类（如 Application 类、主界面控制器、数据管理层）。
   - 分析本地动态库组成（已探明包含 `libBaiduSpeechSDK.so`、`libvad.dnn.so` 等，评估百度语音识别与 VAD 语音活动检测功能）。
3. **关键业务逻辑反编译分析**：
   - 针对核心业务类运行 `rea inspect-android-class` 与 `rea inspect-android-method`。
   - 分析录音处理、语音识别、Markdown 笔记渲染（基于 assets 中的 `markdown-it`）与数据库存储（基于 `litepal.xml`）的实现链路。
4. **生成成果报告**：
   - 在工作区生成详尽的分析成果文档：`docs/reports/apk_analysis_report.md`。

## 计划变更文件清单

```
D:\github-repository\smart-record\
├── docs\
│   ├── plans\
│   │   └── rea_apk_analysis_plan.md        # [NEW] 实施计划文档
│   └── reports\
│       └── apk_analysis_report.md          # [NEW] base.apk 逆向分析交付报告
C:\Users\29602\.gemini\config\
└── mcp_config.json                         # [MODIFY] 添加 rea MCP 伺服程序与环境变量
```

## 验证方案

### 自动化验证
1. 校验 JADX 引擎哈希：
   ```powershell
   (Get-FileHash -Algorithm SHA256 "$HOME\.local\share\rea\android\jadx-headless-mcp-0.7.1-all.jar").Hash
   ```
2. 验证 REA 配置健康状态：
   ```powershell
   npx -y rea-agents@latest doctor --client antigravity --skill --json
   ```
3. 验证 APK 基础信息提取：
   ```powershell
   npx -y rea-agents@latest inspect-android-package "D:\github-repository\smart-record\base.apk"
   ```

### 人工验证
- 检查 `docs/reports/apk_analysis_report.md` 的内容完整性，核对应用基础信息、权限列表、核心组件列表及业务架构逻辑分析是否准确完备。
