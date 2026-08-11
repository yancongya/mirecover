# MIUI 缓存图片恢复 — 真机测试与排坑说明

本应用用于扫描 MIUI 手机上的缓存目录，将 `.0` 后缀的图片文件批量重命名为 `.jpg`，
并恢复到 `DCIM/<子目录>` 相册目录。

## 一、构建与安装

1. 用 Android Studio 打开 `f:/mirecover` 工程（已自带 Gradle 8.4 包装器，无需手动配置）。
2. 连接 MIUI 手机，开启「USB 调试」。
3. 直接 Run / 或通过 `gradlew assembleDebug` 生成 APK 后安装。

## 二、使用流程

1. 打开 App，点击「选择缓存目录」，在系统文件选择器里选中 MIUI 的缓存目录
   （常见位置：`Android/data/<包名>/cache` 或 `MIUI/.cache` 等）。
2. 首次使用建议先点「授予所有文件访问权限」，按提示在 MIUI 权限页打开开关。
3. 可选：在输入框填写目标子目录（如 `Camera` 或 `Recovered`，位于 `DCIM/` 下）。
4. 勾选「移动模式」可在恢复后删除源文件（需所有文件访问权限）。
5. 点「扫描 .0 文件」，预览列表无误后点「恢复选中文件」。
6. 完成后推荐重启相册或等待媒体库刷新，即可在相册看到图片。

## 三、MIUI 已知坑与应对

| 现象 | 原因 | 应对 |
|------|------|------|
| 选完目录后重启 App 扫描失效 | SAF 持久化权限有时被 MIUI 回收 | 重新用「选择缓存目录」授权一次 |
| 点权限按钮崩溃 | MIUI 专用 Intent 不存在 | 已 try-catch 兜底到标准设置页 |
| 移动模式删不掉源文件 | 未授予「所有文件访问」 | 必须先完成权限授予；否则仅复制模式可用 |
| 相册不立即显示 | 媒体库未刷新 | 已调用 `MediaScannerConnection.scanFile`；如仍不显示重启相册 |
| 重名覆盖 | 文件名重复 | `ImageRecover` 自动追加 `_1`/`_2` 序号 |

## 四、权限说明

- `READ_EXTERNAL_STORAGE`：Android 10 及以下读取所需。
- `READ_MEDIA_IMAGES`：Android 11+ 读取图片所需。
- `MANAGE_EXTERNAL_STORAGE`：MIUI 上写入 DCIM 与删除源文件的稳定保障。
- 源目录读取走 SAF，无需危险权限弹窗。

## 五、兼容性

- `minSdk 21`，`targetSdk 33`。
- 写入统一走 `MediaStore` + `RELATIVE_PATH`，兼容 Scoped Storage。
