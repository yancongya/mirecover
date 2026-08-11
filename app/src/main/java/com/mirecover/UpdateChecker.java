package com.mirecover;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import androidx.core.content.FileProvider;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 应用内热更新：
 * 1) 请求 GitHub latest release，解析最新版本号与 APK 下载地址；
 * 2) 若比当前版本新，回调 onUpdateFound(version, url)；
 * 3) 下载 APK 到本地，回调进度，最后触发安装。
 *
 * 采用「在线更新 APK」方案（适用于原生 App），避免引入 Tinker 等重量级框架。
 */
public class UpdateChecker {

    private static final String TAG = "UpdateChecker";
    // 仓库 latest release API
    private static final String LATEST_RELEASE_URL =
            "https://api.github.com/repos/yancongya/mirecover/releases/latest";

    public interface Callback {
        void onLatest(int versionCode, String versionName);
        void onNoUpdate();
        void onError(String msg);
    }

    public interface DownloadCallback {
        void onProgress(int percent);
        void onDone(File apk);
        void onError(String msg);
    }

    private static final ExecutorService pool = Executors.newSingleThreadExecutor();

    /** 检查更新。注意：是在后台线程执行的异步操作，回调会切回主线程需自行 post。 */
    public static void check(final Context context, final Callback cb) {
        pool.execute(() -> {
            try {
                // 读当前版本
                String current = context.getPackageManager()
                        .getPackageInfo(context.getPackageName(), 0).versionName;
                // 请求 GitHub API
                String json = httpGet(LATEST_RELEASE_URL);
                JSONObject release = new JSONObject(json);
                String tag = release.optString("tag_name", "v0.0.0");   // 如 v1.0.3
                String apkUrl = null;
                org.json.JSONArray assets = release.optJSONArray("assets");
                if (assets != null) {
                    for (int i = 0; i < assets.length(); i++) {
                        JSONObject a = assets.getJSONObject(i);
                        String name = a.optString("name", "");
                        if (name.endsWith("-release.apk")) {
                            apkUrl = a.optString("browser_download_url");
                            break;
                        }
                    }
                }
                String latestName = tag.startsWith("v") ? tag.substring(1) : tag;
                int cmp = compareVersion(latestName, current);
                if (cmp > 0 && apkUrl != null) {
                    cb.onLatest(parseCode(latestName), latestName);
                } else {
                    cb.onNoUpdate();
                }
            } catch (Exception e) {
                Log.w(TAG, "check failed", e);
                cb.onError(e.getMessage() == null ? "error" : e.getMessage());
            }
        });
    }

    /** 下载 APK 到应用私有缓存目录。 */
    public static void download(final Context context, final String url,
                                final DownloadCallback cb) {
        pool.execute(() -> {
            try {
                File dir = new File(context.getExternalCacheDir(), "update");
                if (!dir.exists()) dir.mkdirs();
                File apk = new File(dir, "mirecover-update.apk");

                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(30000);
                conn.connect();
                int len = conn.getContentLength();
                InputStream in = new BufferedInputStream(conn.getInputStream());
                FileOutputStream out = new FileOutputStream(apk);
                byte[] buf = new byte[8192];
                int total = 0, n;
                while ((n = in.read(buf)) != -1) {
                    out.write(buf, 0, n);
                    total += n;
                    if (len > 0) cb.onProgress((int) (total * 100f / len));
                }
                out.flush(); out.close(); in.close(); conn.disconnect();
                cb.onDone(apk);
            } catch (Exception e) {
                Log.w(TAG, "download failed", e);
                cb.onError(e.getMessage() == null ? "error" : e.getMessage());
            }
        });
    }

    /** 触发安装：FileProvider 提供 Uri 给系统安装器。 */
    public static void install(Activity activity, File apk) {
        try {
            Uri uri = FileProvider.getUriForFile(activity,
                    activity.getPackageName() + ".fileprovider", apk);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);
        } catch (Exception e) {
            Log.w(TAG, "install failed", e);
        }
    }

    private static String httpGet(String urlStr) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/vnd.github+json");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(15000);
        conn.connect();
        InputStream in = conn.getInputStream();
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
        in.close(); conn.disconnect();
        return out.toString(StandardCharsets.UTF_8.name());
    }

    /** 比较 a 是否比 b 新。返回 >0 表示 a 更新。 */
    private static int compareVersion(String a, String b) {
        String[] as = a.split("\\.");
        String[] bs = b.split("\\.");
        int n = Math.max(as.length, bs.length);
        for (int i = 0; i < n; i++) {
            int av = i < as.length ? toInt(as[i]) : 0;
            int bv = i < bs.length ? toInt(bs[i]) : 0;
            if (av != bv) return av - bv;
        }
        return 0;
    }

    private static int toInt(String s) {
        try { return Integer.parseInt(s.replaceAll("[^0-9]", "")); }
        catch (Exception e) { return 0; }
    }

    /** 从版本名 "1.0.3" 提取 versionCode 用于判断（近似用末段）。 */
    private static int parseCode(String name) {
        String[] p = name.split("\\.");
        if (p.length >= 3) {
            return toInt(p[2]) + toInt(p[1]) * 100 + toInt(p[0]) * 10000;
        }
        return toInt(name);
    }
}
