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

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    static final String DEFAULT_SRC =
            "/storage/emulated/0/Download/full_size";
    static final String DEFAULT_REPAIR =
            "/storage/emulated/0/Download/MIRecovered";

    private HomeFragment homeFragment;
    private SrcFragment srcFragment;
    private PreviewFragment previewFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 三个 Tab：源文件 / 修复预览 / 主面板
        TabLayout tabLayout = findViewById(R.id.tab_layout);
        ViewPager2 viewPager = findViewById(R.id.view_pager);
        PagerAdapter pagerAdapter = new PagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);
        // 预创建全部 Fragment，保证修复后能立即刷新
        viewPager.setOffscreenPageLimit(3);
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(getTabTitle(position));
        }).attach();

        // 检测并申请存储权限
        checkAndRequestPermissions();
    }

    /** 三个 Tab 标题。 */
    private String getTabTitle(int position) {
        if (position == 0) return getString(R.string.tab_src);
        if (position == 1) return getString(R.string.tab_preview);
        return getString(R.string.tab_home);
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
                // 刷新源文件 / 修复预览两个 tab
                refreshSrc();
                if (previewFragment != null) {
                    previewFragment.setRepairDir(tgt);
                    previewFragment.loadRepaired();
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

    /** Pager 适配器：提供三个 Fragment（源文件 / 修复预览 / 主面板）。 */
    private class PagerAdapter extends FragmentStateAdapter {

        PagerAdapter(@NonNull MainActivity activity) {
            super(activity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (position == 0) {
                if (srcFragment == null) srcFragment = new SrcFragment();
                return srcFragment;
            } else if (position == 1) {
                if (previewFragment == null) previewFragment = new PreviewFragment();
                return previewFragment;
            } else {
                if (homeFragment == null) homeFragment = new HomeFragment();
                return homeFragment;
            }
        }

        @Override
        public int getItemCount() {
            return 3;
        }
    }
}
