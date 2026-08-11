package com.mirecover;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    static final String DEFAULT_SRC =
            "/storage/emulated/0/Download/full_size";
    static final String DEFAULT_REPAIR =
            "/storage/emulated/0/Download/MIRecovered";

    /** 各 Tab 对应的 ViewPager position。 */
    private static final int POS_HOME = 0;
    private static final int POS_SRC = 1;
    private static final int POS_PREVIEW = 2;
    private static final int POS_ABOUT = 3;

    private HomeFragment homeFragment;
    private SrcFragment srcFragment;
    private PreviewFragment previewFragment;
    private AboutFragment aboutFragment;
    private ViewPager2 viewPager;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 三个页面：主面板 / 源文件 / 修复预览
        viewPager = findViewById(R.id.view_pager);
        bottomNav = findViewById(R.id.bottom_nav);
        viewPager.setAdapter(new PagerAdapter(this));
        // 预创建全部 Fragment，保证修复后能立即刷新
        viewPager.setOffscreenPageLimit(4);

        // 底部导航 ↔ ViewPager 双向联动
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                bottomNav.setSelectedItemId(getNavIdForPosition(position));
            }
        });
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            int pos;
            if (id == R.id.nav_home) pos = POS_HOME;
            else if (id == R.id.nav_src) pos = POS_SRC;
            else if (id == R.id.nav_preview) pos = POS_PREVIEW;
            else pos = POS_ABOUT;
            viewPager.setCurrentItem(pos, false);
            return true;
        });

        // 检测并申请存储权限
        checkAndRequestPermissions();

        // 启动后静默检查更新（有新版才提示，避免打扰）
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
                this::silentCheckUpdate, 2500);
    }

    /** 启动时静默检查更新：仅当发现新版本时弹窗，否则保持安静。 */
    private void silentCheckUpdate() {
        UpdateChecker.check(this, new UpdateChecker.Callback() {
            @Override
            public void onLatest(int versionCode, String versionName) {
                runOnUiThread(() -> promptDownload(versionName));
            }

            @Override
            public void onNoUpdate() { /* 静默 */ }

            @Override
            public void onError(String msg) { /* 静默 */ }
        });
    }

    /** 弹出下载对话框并走下载安装流程。 */
    private void promptDownload(String newVersion) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.update_dialog_title)
                .setMessage(getString(R.string.update_dialog_msg,
                        currentVersionName(), newVersion))
                .setPositiveButton(R.string.btn_download, (d, w) ->
                        startDownloadAndInstall(newVersion))
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void startDownloadAndInstall(String version) {
        Toast.makeText(this, getString(R.string.update_downloading, 0), Toast.LENGTH_SHORT).show();
        UpdateChecker.download(this, apkUrl(), new UpdateChecker.DownloadCallback() {
            @Override
            public void onProgress(int percent) { }

            @Override
            public void onDone(File apk) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, R.string.update_download_ok, Toast.LENGTH_LONG).show();
                    UpdateChecker.install(MainActivity.this, apk);
                });
            }

            @Override
            public void onError(String msg) {
                runOnUiThread(() ->
                        Toast.makeText(MainActivity.this, R.string.update_download_fail, Toast.LENGTH_LONG).show());
            }
        });
    }

    private String currentVersionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "1.0.0";
        }
    }

    private String apkUrl() {
        return "https://github.com/yancongya/mirecover/releases/latest/download/app-release.apk";
    }

    /** position → 底部导航菜单 id。 */
    private int getNavIdForPosition(int position) {
        if (position == POS_HOME) return R.id.nav_home;
        if (position == POS_SRC) return R.id.nav_src;
        if (position == POS_PREVIEW) return R.id.nav_preview;
        return R.id.nav_about;
    }

    private static final int REQ_STORAGE = 2001;

    /** 检测存储权限，不足则弹窗引导申请。 */
    private void checkAndRequestPermissions() {
        if (PermissionHelper.hasStoragePermission(this)
                && !PermissionHelper.needsRuntimePermission(this)) {
            if (homeFragment != null) homeFragment.setStatus(getString(R.string.permission_ok));
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.permission_need_title)
                .setMessage(R.string.permission_need_msg)
                .setPositiveButton(R.string.permission_goto, (d, w) -> grantPermissions())
                .setNegativeButton("取消", null)
                .show();
    }

    /** 按需发起申请：运行时权限 + 所有文件访问。 */
    private void grantPermissions() {
        if (PermissionHelper.needsRuntimePermission(this)) {
            PermissionHelper.requestRuntimePermission(this, REQ_STORAGE);
        } else {
            PermissionHelper.requestAllFilesAccess(this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_STORAGE) {
            // 运行时权限已处理，仍需所有文件访问（Android 11+）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                    && !PermissionHelper.hasAllFilesAccess()) {
                PermissionHelper.requestAllFilesAccess(this);
            }
            checkAndRequestPermissions();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 用户从设置页返回后重新检测
        if (PermissionHelper.hasStoragePermission(this)
                && !PermissionHelper.needsRuntimePermission(this)) {
            if (homeFragment != null) homeFragment.setStatus(getString(R.string.permission_ok));
        }
    }

    /** 由 PreviewFragment 调用，读取相册子目录。 */
    public String getSubdir() {
        return homeFragment != null ? homeFragment.getSubdir() : "Camera";
    }

    /** 用 FileProvider 打开指定目录。 */
    public void openDir(String path) {
        if (path == null || path.isEmpty()) {
            Toast.makeText(this, R.string.source_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        File dir = new File(path);
        if (!dir.exists()) {
            Toast.makeText(this, "目录不存在: " + path, Toast.LENGTH_LONG).show();
            return;
        }
        try {
            Uri uri = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", dir);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "vnd.android.document/directory");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception e) {
            // 无文件管理器支持时，回退到 SAF 选择器
            try {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                startActivity(intent);
            } catch (Exception ex) {
                Toast.makeText(this, "无法打开目录", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void doRepair() {
        // 权限不足先提醒，避免静默产出 0
        if (!PermissionHelper.hasStoragePermission(this)
                || PermissionHelper.needsRuntimePermission(this)) {
            checkAndRequestPermissions();
            return;
        }
        String src = homeFragment.getSourcePath();
        String tgt = homeFragment.getRepairPath();
        if (src.isEmpty() || tgt.isEmpty()) {
            Toast.makeText(this, R.string.source_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        homeFragment.setStatus(getString(R.string.repairing));
        homeFragment.setRepairBusy(true);
        new Thread(() -> {
            // 先扫描源目录，用于诊断
            FileScanner.ScanResult srcScan = FileScanner.scan(src, ".0");
            RepairJob.RepairResult r = RepairJob.repair(src, tgt, true);
            runOnUiThread(() -> {
                String diag = getString(R.string.repair_done, r.repaired, r.skipped, r.failed)
                        + "\n" + srcScan.diagnostic;
                homeFragment.setStatus(diag);
                homeFragment.setRepairBusy(false);
                // 刷新源文件 / 修复预览两个页面
                refreshSrc();
                if (previewFragment != null) {
                    previewFragment.setRepairDir(tgt);
                    previewFragment.loadRepaired();
                }
                // 修复完成后自动跳到「源文件」页面
                if (viewPager != null) {
                    viewPager.setCurrentItem(POS_SRC, false);
                }
                // 若一个都没修复，弹窗明示原因，便于排查
                if (r.repaired == 0 && r.failed == 0) {
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("未找到可修复的 .0 文件")
                            .setMessage("源目录: " + src + "\n\n" + srcScan.diagnostic)
                            .setPositiveButton("知道了", null)
                            .show();
                }
            });
        }).start();
    }

    /** 刷新 Tab1（源 .0 列表）。 */
    private void refreshSrc() {
        if (srcFragment != null) {
            srcFragment.showSource(homeFragment.getSourcePath());
        }
    }

    public void showHelp() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.btn_help_copy)
                .setMessage(R.string.help_copy_tips)
                .setPositiveButton("知道了", null)
                .show();
    }

    /** Pager 适配器：提供三个 Fragment（主面板 / 源文件 / 修复预览）。 */
    private class PagerAdapter extends FragmentStateAdapter {

        PagerAdapter(@NonNull MainActivity activity) {
            super(activity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (position == POS_HOME) {
                if (homeFragment == null) homeFragment = new HomeFragment();
                return homeFragment;
            } else if (position == POS_SRC) {
                if (srcFragment == null) srcFragment = new SrcFragment();
                return srcFragment;
            } else if (position == POS_PREVIEW) {
                if (previewFragment == null) previewFragment = new PreviewFragment();
                return previewFragment;
            } else {
                if (aboutFragment == null) aboutFragment = new AboutFragment();
                return aboutFragment;
            }
        }

        @Override
        public int getItemCount() {
            return 4;
        }
    }
}
