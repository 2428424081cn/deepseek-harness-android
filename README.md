# DeepSeek Harness Android

<p align="center">
  <img src="./icon.png" width="128" height="128" alt="DeepSeek Harness Logo" />
</p>

<p align="center">
  <strong>DeepSeek Harness 移动端配套 Android 应用 / Mobile Companion App</strong>
</p>

---

## 📖 简介 / Introduction

**DeepSeek Harness Android** 是专为 [DeepSeek Harness](https://github.com/2428424081cn/deepseek-harness) 打造的轻量级原生移动端伴侣应用。通过深度优化的 WebView 与原生 Android Bridge，提供沉浸式的移动端交互体验与远程运维能力。

This is a lightweight native Android companion application designed specifically for DeepSeek Harness. It brings seamless mobile interaction, native notification channels, multi-server profile switching, and mobile viewport optimizations.

---

## ✨ 核心特性 / Features

- 📱 **移动端视图深度适配 (Mobile Viewport Optimization)**
  - 自动注入移动端适配 CSS 与 JS 脚本，优化对话流、输入框布局与滚动体验。
  - 支持软键盘弹起防遮挡与全屏沉浸式状态栏适配。

- ⚡ **原生双向桥接 (Native Android JS Bridge)**
  - `window.AndroidBridge` 双向交互接口。
  - **系统级通知通道 (System Notifications)**：后台或运行中即时接收 Agent 任务完成、审批请求通知。
  - **触觉反馈 (Haptics & Vibration)**：任务完成与重要操作触感提示。
  - **模态框与状态捕获**：自动追踪网页弹窗状态，优先响应返回操作。

- 🔄 **多服务器实例管理 (Multi-Server Profile Manager)**
  - 支持快捷切换多个服务器配置（本地局域网电脑、开发机、远程 VPS 等）。
  - 支持自定义 IP/域名、端口及 HTTP/HTTPS (SSL) 协议配置。

- 🛠️ **便捷悬浮操作栏 (Floating Action Pill)**
  - 悬浮胶囊按钮，可快捷查看当前连接节点、快速刷新网页、一键切换服务器或进入设置。

- 🔙 **智能返回导航 (Smart Back Navigation)**
  - 优先关闭网页中的活跃弹窗/模态层。
  - 其次执行 WebView 内部历史后退。
  - 根页面支持双击返回退出应用，防止误触。

- 📁 **文件与媒体选择支持 (File Chooser)**
  - 原生支持网页端的文件选择与附件上传功能。

---

## 🏗️ 技术架构 / Tech Stack

- **语言 (Language)**: Kotlin (100%)
- **最低支持版本 (Min SDK)**: Android 8.0 (API 26)
- **目标版本 (Target / Compile SDK)**: Android 15 (API 35)
- **核心组件 (Core Architecture)**:
  - AndroidX Core KTX / AppCompat / Activity KTX
  - Android Material Components 3
  - ViewBinding
  - Android System NotificationManager & WebKit WebView
  - Gradle Version Catalog (`libs.versions.toml`)

---

## 🚀 快速上手 / Getting Started

### 前置要求 / Prerequisites
- Android Studio Ladybug (2024.2+) 或更高版本
- JDK 17
- Android SDK 35

### 编译与运行 / Build & Run

1. **克隆仓库 (Clone repository)**
   ```bash
   git clone https://github.com/2428424081cn/deepseek-harness-android.git
   cd deepseek-harness-android
   ```

2. **编译 Debug APK (Build Debug APK)**
   - Windows:
     ```cmd
     gradlew.bat assembleDebug
     ```
   - macOS / Linux:
     ```bash
     ./gradlew assembleDebug
     ```
   生成文件位于 `app/build/outputs/apk/debug/app-debug.apk`。

3. **连接与使用 (Connect to Harness Server)**
   - 打开应用后进入连接配置界面。
   - 输入正在运行 DeepSeek Harness Web 服务的 IP/域名（例如 `192.168.1.100`）和端口（默认 `3080`）。
   - 点击保存并连接即可开始使用。

---

## 📄 开源协议 / License

本项目采用 [Apache-2.0 License](LICENSE) 开源协议。
