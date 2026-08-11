package com.mirecover;

import com.mirecover.model.FileItem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 通过 java.io.File 递归扫描给定目录，按扩展名过滤文件。
 * 需要「所有文件访问」权限才能访问 Android/data 等受保护目录。
 */
public final class FileScanner {

    private FileScanner() { }

    /** 扫描结果：文件列表 + 诊断信息。 */
    public static class ScanResult {
        public final List<FileItem> files;
        public final String diagnostic;

        ScanResult(List<FileItem> files, String diagnostic) {
            this.files = files;
            this.diagnostic = diagnostic;
        }
    }

    /**
     * 递归扫描 dirPath，收集指定后缀的文件。
     *
     * @param dirPath   目录绝对路径
     * @param ext       目标扩展名（小写，含点，如 ".0" 或 ".jpg"），传 null 收集全部文件
     */
    public static ScanResult scan(String dirPath, String ext) {
        List<FileItem> result = new ArrayList<>();
        StringBuilder diag = new StringBuilder();

        if (dirPath == null || dirPath.trim().isEmpty()) {
            diag.append("诊断: 路径为空。");
            return new ScanResult(result, diag.toString());
        }

        File root = new File(dirPath);
        if (!root.exists()) {
            diag.append("诊断: 路径不存在 -> ").append(dirPath);
            return new ScanResult(result, diag.toString());
        }
        if (!root.isDirectory()) {
            diag.append("诊断: 该路径不是目录 -> ").append(dirPath);
            return new ScanResult(result, diag.toString());
        }

        Counter c = new Counter();
        collect(root, result, diag, c, ext, 0);

        diag.insert(0, "诊断: 目录数=" + c.dirs + ", 文件数=" + c.files
                + ", 匹配(" + (ext == null ? "全部" : ext) + ")数=" + result.size() + "\n");

        if (result.isEmpty()) {
            diag.append("提示: 未找到匹配文件。可能原因: 路径下无文件 / 扩展名不对 / 文件访问受限。");
        }
        return new ScanResult(result, diag.toString());
    }

    private static final class Counter {
        int dirs;
        int files;
    }

    private static void collect(File dir, List<FileItem> out, StringBuilder diag,
                                Counter c, String ext, int depth) {
        File[] children = dir.listFiles();
        if (children == null) {
            diag.append("诊断: 无权限列出 ").append(dir.getAbsolutePath()).append("\n");
            return;
        }
        for (File f : children) {
            if (f.isDirectory()) {
                c.dirs++;
                collect(f, out, diag, c, ext, depth + 1);
            } else if (f.isFile()) {
                c.files++;
                String name = f.getName();
                if (name == null) continue;
                if (ext == null || name.toLowerCase().endsWith(ext)) {
                    out.add(new FileItem(f));
                }
            }
        }
    }
}
