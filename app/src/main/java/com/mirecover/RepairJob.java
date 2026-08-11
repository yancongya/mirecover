package com.mirecover;

import com.mirecover.model.FileItem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * 第一步「修复」：把源目录中的 .0 文件复制到修复目录，并重命名为 .jpg。
 * 源文件保留（复制语义）。
 */
public final class RepairJob {

    private RepairJob() { }

    public static class RepairResult {
        public final int repaired;      // 成功修复数
        public final int skipped;       // 已存在跳过数
        public final int failed;        // 失败数
        public final String targetDir;  // 修复目录绝对路径

        RepairResult(int repaired, int skipped, int failed, String targetDir) {
            this.repaired = repaired;
            this.skipped = skipped;
            this.failed = failed;
            this.targetDir = targetDir;
        }
    }

    /**
     * 递归修复源目录下的 .0 文件到 targetDir。
     *
     * @param srcDir    源目录（含 .0 文件）
     * @param targetDir 修复输出目录
     * @param overwrite 目标已存在同名 .jpg 时是否覆盖
     */
    public static RepairResult repair(String srcDir, String targetDir, boolean overwrite) {
        File tgt = new File(targetDir);
        if (!tgt.exists() && !tgt.mkdirs()) {
            return new RepairResult(0, 0, 0, targetDir);
        }

        FileScanner.ScanResult sr = FileScanner.scan(srcDir, ".0");
        int repaired = 0, skipped = 0, failed = 0;

        for (FileItem it : sr.files) {
            File dest = new File(tgt, it.targetName());
            if (dest.exists() && !overwrite) {
                skipped++;
                continue;
            }
            if (copyFile(it.file, dest)) {
                repaired++;
            } else {
                failed++;
            }
        }
        return new RepairResult(repaired, skipped, failed, targetDir);
    }

    private static boolean copyFile(File src, File dest) {
        try (InputStream in = new FileInputStream(src);
             OutputStream out = new FileOutputStream(dest)) {
            byte[] buf = new byte[64 * 1024];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
