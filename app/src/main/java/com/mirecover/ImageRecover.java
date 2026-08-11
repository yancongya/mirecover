package com.mirecover;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import com.mirecover.model.FileItem;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * 核心恢复逻辑：将 .0 文件重命名为 .jpg 并写入 DCIM/&lt;subdir&gt; 相册目录。
 * 通过 MediaStore 写入以兼容 Android 11+ Scoped Storage。
 */
public final class ImageRecover {

    private static final String TAG = "ImageRecover";

    private ImageRecover() { }

    /**
     * 恢复一组文件。
     *
     * @param ctx     上下文
     * @param items   待恢复文件列表
     * @param subdir  目标子目录名（如 Camera / Recovered），将位于 DCIM 下
     * @param move    为 true 时恢复成功后删除源文件，否则保留
     * @return 成功恢复的文件数量
     */
    public static int recover(Context ctx, List<FileItem> items,
                              String subdir, boolean move) {
        if (items == null || items.isEmpty()) return 0;
        ContentResolver cr = ctx.getContentResolver();
        String safeSub = sanitizeSubdir(subdir);
        int ok = 0;

        for (FileItem it : items) {
            if (it.file == null || !it.file.exists()) continue;

            String displayName = findFreeName(cr, safeSub, it.targetName());
            Uri inserted = insertViaMediaStore(cr, safeSub, displayName);
            if (inserted == null) continue;

            boolean written = copyBytes(ctx, cr, it, inserted);
            if (written) {
                finalizePending(cr, inserted);
                if (move) {
                    try { it.file.delete(); } catch (Throwable ignored) { }
                }
                ok++;
            } else {
                // 写入失败，回滚插入的占位记录
                try { cr.delete(inserted, null, null); } catch (Throwable ignored) { }
            }
        }
        return ok;
    }

    /**
     * 在 MediaStore 中插入一条 PENDING 状态的图片记录，返回可写 URI。
     */
    private static Uri insertViaMediaStore(ContentResolver cr, String subdir, String name) {
        ContentValues v = new ContentValues();
        v.put(MediaStore.Images.Media.DISPLAY_NAME, name);
        v.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        v.put(MediaStore.Images.Media.RELATIVE_PATH,
                Environment.DIRECTORY_DCIM + "/" + subdir);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            v.put(MediaStore.Images.Media.IS_PENDING, 1);
        }
        return cr.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v);
    }

    /**
     * 复制字节流：从源文件读取，写入 MediaStore 提供的 OutputStream。
     */
    private static boolean copyBytes(Context ctx, ContentResolver cr,
                                     FileItem it, Uri dest) {
        try (InputStream in = new FileInputStream(it.file);
             OutputStream out = cr.openOutputStream(dest)) {
            if (out == null) return false;
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

    /**
     * 清除 PENDING 标记，使图片对相册可见。
     */
    private static void finalizePending(ContentResolver cr, Uri uri) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return;
        ContentValues v = new ContentValues();
        v.put(MediaStore.Images.Media.IS_PENDING, 0);
        try { cr.update(uri, v, null, null); } catch (Throwable ignored) { }
    }

    /**
     * 通过查询 MediaStore 解决重名冲突，自动追加 _1 / _2 序号。
     */
    private static String findFreeName(ContentResolver cr, String subdir, String baseName) {
        String candidate = baseName;
        int idx = 1;
        while (existsInMediaStore(cr, subdir, candidate)) {
            int dot = baseName.lastIndexOf('.');
            if (dot > 0) {
                candidate = baseName.substring(0, dot) + "_" + idx + baseName.substring(dot);
            } else {
                candidate = baseName + "_" + idx;
            }
            idx++;
        }
        return candidate;
    }

    private static boolean existsInMediaStore(ContentResolver cr, String subdir, String name) {
        String sel = MediaStore.Images.Media.DISPLAY_NAME + "=? AND "
                + MediaStore.Images.Media.RELATIVE_PATH + " LIKE ?";
        String rel = Environment.DIRECTORY_DCIM + "/" + subdir + "%";
        try (android.database.Cursor c = cr.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Images.Media._ID},
                sel, new String[]{name, rel}, null)) {
            return c != null && c.getCount() > 0;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * 清洗子目录名：仅保留字母数字下划线连字符，空值回落到 Camera。
     */
    public static String sanitizeSubdir(String s) {
        if (s == null) return "Camera";
        s = s.trim().replaceAll("[^a-zA-Z0-9_\\-]", "");
        return s.isEmpty() ? "Camera" : s;
    }
}
