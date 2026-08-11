package com.mirecover;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.widget.ImageView;

import java.io.File;

/**
 * 图片预览弹窗：在对话框内显示修复后的 .jpg 大图。
 */
public final class ImagePreviewDialog {

    private ImagePreviewDialog() { }

    public static void show(Context ctx, String path, String title) {
        File f = new File(path);
        if (!f.exists()) {
            new AlertDialog.Builder(ctx).setMessage("文件不存在").setPositiveButton("确定", null).show();
            return;
        }
        // 采样解码，避免大图 OOM
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, opt);
        int maxDim = 1600;
        int sample = 1;
        int w = opt.outWidth, h = opt.outHeight;
        while ((w / sample) > maxDim || (h / sample) > maxDim) sample *= 2;
        opt.inJustDecodeBounds = false;
        opt.inSampleSize = sample;

        Bitmap bmp = BitmapFactory.decodeFile(path, opt);
        if (bmp == null) {
            new AlertDialog.Builder(ctx).setMessage("无法解码图片").setPositiveButton("确定", null).show();
            return;
        }
        ImageView iv = new ImageView(ctx);
        iv.setImageBitmap(bmp);
        iv.setPadding(24, 24, 24, 24);
        new AlertDialog.Builder(ctx)
                .setTitle(title)
                .setView(iv)
                .setPositiveButton("关闭", (d, w2) -> {
                    BitmapDrawable bd = (BitmapDrawable) iv.getDrawable();
                    if (bd != null && bd.getBitmap() != null) bd.getBitmap().recycle();
                })
                .show();
    }
}
