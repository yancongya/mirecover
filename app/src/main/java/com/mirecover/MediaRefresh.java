package com.mirecover;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.os.Environment;

import java.io.File;

/**
 * 恢复完成后刷新系统媒体库，让相册立即识别新图片。
 */
public final class MediaRefresh {

    private MediaRefresh() { }

    /**
     * 扫描 DCIM 下的恢复子目录，触发媒体库刷新。
     *
     * @param ctx    上下文
     * @param subdir 恢复的相册子目录名（位于 DCIM 下）
     */
    public static void refresh(Context ctx, String subdir) {
        String dir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM), subdir).getAbsolutePath();
        MediaScannerConnection.scanFile(ctx,
                new String[]{dir},
                new String[]{"image/jpeg"},
                null);
    }
}
