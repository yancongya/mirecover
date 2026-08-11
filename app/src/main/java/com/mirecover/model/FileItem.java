package com.mirecover.model;

import java.io.File;

/**
 * 表示一个待恢复的缓存文件。
 * 通过 java.io.File 访问（需「所有文件访问」权限才能进入 Android/data 等受保护目录）。
 */
public class FileItem {
    public final File file;       // 源文件句柄，用于读取/删除
    public final String name;     // 原始文件名（含 .0 后缀）
    public final long size;       // 文件大小（字节）

    public FileItem(File file) {
        this.file = file;
        this.name = file.getName() == null ? "" : file.getName();
        this.size = file.length();
    }

    /** 去掉 .0 后缀后的目标文件名（.jpg）。 */
    public String targetName() {
        String base = name;
        if (base.toLowerCase().endsWith(".0")) {
            base = base.substring(0, base.length() - 2);
        }
        return base + ".jpg";
    }

    /** 人类可读大小。 */
    public String sizeText() {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return (size / 1024) + " KB";
        return (size / (1024 * 1024)) + " MB";
    }

    /** 修改时间（yyyy-MM-dd HH:mm）。 */
    public String modTimeText() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date(file.lastModified()));
    }
}
