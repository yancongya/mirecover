package com.mirecover;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import com.mirecover.model.FileItem;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * 第二步「导出」：把已修复的 .jpg 文件通过 MediaStore 写入相册目录，
 * 使相册立即可见。
 */
public final class ExportJob {

    private ExportJob() { }

    public static class ExportResult {
        public final int exported;
        public final int failed;
        public final String subdir;

        ExportResult(int exported, int failed, String subdir) {
            this.exported = exported;
            this.failed = failed;
            this.subdir = subdir;
        }
    }

    /**
     * 导出选中文件到 DCIM/subdir。
     *
     * @param ctx      上下文
     * @param items    选中的已修复 .jpg 文件
     * @param subdir   相册子目录（如 Camera / Recovered）
     */
    public static ExportResult export(Context ctx, List<FileItem> items, String subdir) {
        int exported = 0, failed = 0;
        if (items == null || items.isEmpty()) {
            return new ExportResult(0, 0, subdir);
        }
        ContentResolver cr = ctx.getContentResolver();
        String safeSub = ImageRecover.sanitizeSubdir(subdir);

        for (FileItem it : items) {
            if (it.file == null || !it.file.exists()) {
                failed++;
                continue;
            }
            String displayName = findFreeName(cr, safeSub, it.targetName());
            Uri dest = insertViaMediaStore(cr, safeSub, displayName);
            if (dest == null) { failed++; continue; }

            if (copyBytes(cr, it, dest)) {
                finalizePending(cr, dest);
                exported++;
            } else {
                try { cr.delete(dest, null, null); } catch (Throwable ignored) { }
                failed++;
            }
        }
        return new ExportResult(exported, failed, safeSub);
    }

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

    private static boolean copyBytes(ContentResolver cr, FileItem it, Uri dest) {
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

    private static void finalizePending(ContentResolver cr, Uri uri) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return;
        ContentValues v = new ContentValues();
        v.put(MediaStore.Images.Media.IS_PENDING, 0);
        try { cr.update(uri, v, null, null); } catch (Throwable ignored) { }
    }

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
}
