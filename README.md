# MIUI 缓存图片恢复 (MIRecover)

> 为家人开发的一款图片找回工具：把 MIUI 相册缓存里的 `.0` 文件修复为 `.jpg`，再导出回相册。

这是一个很简单的小工具，主要为了避免“糊涂老妈误删图片后找不回来”的烦恼而开发。
很多“被删掉”的照片其实并没有真正消失，只是变成了相册缓存里没有名字的 `.0` 文件——本 App 就是帮它们“现出原形”。

## ✨ 功能特性

- **找回隐藏图片**：扫描缓存目录，把 `.0` 后缀的图片批量修复为 `.jpg`
- **可视化预览**：修复结果以双列缩略图网格展示，点击勾选、长按全屏预览
- **一键导出相册**：把勾选的图片导出到系统相册（`DCIM/<子目录>`）
- **Material 3 设计**：跟随系统浅/深色模式，MIUI 橙色主题
- **三个清晰 Tab**：源文件(.0) / 修复预览 / 主面板，各司其职
- **家人友好**：内置图文「使用说明」，讲清楚每一步该怎么做

## 📱 界面结构

| Tab | 说明 |
|-----|------|
| **源文件(.0)** | 源缓存目录下的 `.0` 文件列表，点击可打开所在目录 |
| **修复预览** | 修复后的图片缩略图网格，勾选 / 长按预览 / 导出 |
| **主面板** | 配置源路径 / 修复按钮 / 相册子目录 / 使用说明与帮助 |

## 🛠 技术栈

- **语言**：Java
- **UI**：Material 3（`Theme.Material3.DayNight`）+ 传统 View / XML
- **列表**：RecyclerView（源文件单列 + 预览双列网格）
- **图片加载**：Glide 4.16
- **组件**：ViewPager2 + TabLayoutMediator、ConstraintLayout、CoordinatorLayout
- **最低版本**：`minSdk 21`，目标 `targetSdk 35`

## 🚀 构建与运行

### 环境要求

- JDK 17+（推荐用 Android Studio 自带的 JBR）
- Android SDK（`compileSdk 35`）
- Gradle 8.11.1（仓库已带 wrapper）

### 命令行构建

```bash
# Windows
build_debug.bat
# 或直接用 gradle wrapper
gradlew.bat assembleDebug
```

APK 输出位置：`app/build/outputs/apk/debug/app-debug.apk`

### Android Studio

1. 用 Android Studio 打开 `f:/mirecover`
2. 连接 MIUI 手机，开启 USB 调试
3. 直接点 Run，或 `Build > Build APK(s)`

## 📖 使用流程

> 完整的分步说明已内置在 App 的「使用说明」里，以下是概要。

1. **准备**：用手机「文件管理」，把相册缓存 `Android/data/com.miui.gallery/files/gallery_disk_cache/full_size` 整个文件夹复制到 `Download` 目录
2. **修复**：在「主面板」确认源路径后，点橙色「修复文件」按钮，App 递归扫描并把 `.0` 修复为 `.jpg`
3. **找回**：在「修复预览」勾选想要找回的图片，点「导出选中到相册」

## 🔐 权限说明

| 权限 | 用途 |
|------|------|
| `READ_EXTERNAL_STORAGE`（≤Android 10） | 读取外部存储 |
| `READ_MEDIA_IMAGES`（Android 11+） | 读取媒体图片 |
| `MANAGE_EXTERNAL_STORAGE` | 写入 DCIM / 读取 Android/data 缓存 |

> Android 11+ 系统限制，App 无法直接读取 `Android/data`，因此必须先把缓存目录复制到普通位置（如 `Download`）。

## 📁 目录结构

```
f:/mirecover/
├── app/
│   └── src/main/
│       ├── java/com/mirecover/
│       │   ├── MainActivity.java      # 入口，三 Tab 容器
│       │   ├── HomeFragment.java      # 主面板（配置/说明）
│       │   ├── SrcFragment.java       # 源文件(.0)列表
│       │   ├── PreviewFragment.java   # 修复预览网格
│       │   ├── SrcAdapter.java        # 源文件列表适配器
│       │   ├── PreviewAdapter.java    # 预览网格适配器
│       │   ├── FileScanner.java       # 扫描 .0 / .jpg
│       │   ├── RepairJob.java         # .0 → .jpg 修复
│       │   ├── ExportJob.java         # 导出到相册
│       │   ├── MediaRefresh.java      # 媒体库刷新
│       │   └── PermissionHelper.java  # 存储权限
│       └── res/
│           ├── layout/                # 各页面与条目布局
│           ├── values/ + values-night/# 浅/深色主题与色板
│           └── drawable/              # 矢量图标
├── README_MIUI.md                     # MIUI 真机排坑说明
└── build.gradle                       # 根构建脚本
```

## 📜 说明

- 找回的图片可能是原图的缓存版本，清晰度通常低于原图
- 详细的 MIUI 真机已知问题与应对方案见 [README_MIUI.md](./README_MIUI.md)
- 本项目为个人家庭用途开发，欢迎按需修改

## 📄 License

仅供学习交流，可自由修改使用。
