package com.mirecover;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 权限与 MIUI 适配辅助类。
 * 检测并引导申请存储相关权限：所有文件访问 + 读取媒体图片 + 读取外部存储。
 */
public final class PermissionHelper {

    private PermissionHelper() { }

    /** 是否小米/红米设备。 */
    public static boolean isXiaomi() {
        String manufacturer = Build.MANUFACTURER == null ? "" : Build.MANUFACTURER.toLowerCase(Locale.ROOT);
        String brand = Build.BRAND == null ? "" : Build.BRAND.toLowerCase(Locale.ROOT);
        return manufacturer.contains("xiaomi") || brand.contains("redmi") || brand.contains("xiaomi");
    }

    /** 是否已拥有「所有文件访问」权限（Android 11+）。 */
    public static boolean hasAllFilesAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        }
        return true;
    }

    /**
     * 综合判断是否已具备读取目标目录所需的全部存储权限。
     */
    public static boolean hasStoragePermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+：需要所有文件访问权限才能读 Android/data 及写 DCIM
            return hasAllFilesAccess();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    /**
     * 需要申请的运行时权限列表（Android 13+ 用 READ_MEDIA_IMAGES）。
     */
    public static String[] runtimePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return new String[]{Manifest.permission.READ_MEDIA_IMAGES};
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
        }
        return new String[0];
    }

    /** 是否需要申请运行时权限（Android 12 及以下 / 13+ 的媒体权限）。 */
    public static boolean needsRuntimePermission(Activity activity) {
        String[] perms = runtimePermissions();
        for (String p : perms) {
            if (ContextCompat.checkSelfPermission(activity, p)
                    != PackageManager.PERMISSION_GRANTED) {
                return true;
            }
        }
        return false;
    }

    /** 发起运行时权限申请。 */
    public static void requestRuntimePermission(Activity activity, int requestCode) {
        String[] perms = runtimePermissions();
        if (perms.length > 0) {
            ActivityCompat.requestPermissions(activity, perms, requestCode);
        }
    }

    /** 跳转到「所有文件访问」设置页。 */
    public static void requestAllFilesAccess(Activity activity) {
        if (hasAllFilesAccess()) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent standard = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                standard.setData(Uri.parse("package:" + activity.getPackageName()));
                activity.startActivity(standard);
            } catch (Throwable ex) {
                Intent fallback = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                fallback.setData(Uri.parse("package:" + activity.getPackageName()));
                activity.startActivity(fallback);
            }
        }
    }
}
