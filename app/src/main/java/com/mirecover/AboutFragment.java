package com.mirecover;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

import java.io.File;

/**
 * 关于页（Tab4）：
 * 展示 Logo、版本、功能简介；支持检查更新（热更新）、跳转官网与 GitHub。
 */
public class AboutFragment extends Fragment {

    private static final String GITHUB_URL = "https://github.com/yancongya/mirecover";
    private static final String WEBSITE_URL = "https://yancongya.github.io/mirecover/";
    private static final String ONLINE_REPAIR_URL = "https://yancongya.github.io/mirecover/repair.html";

    private TextView tvVersion;
    private MaterialButton btnCheckUpdate;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_about, container, false);

        tvVersion = v.findViewById(R.id.tv_about_version);
        btnCheckUpdate = v.findViewById(R.id.btn_check_update);
        MaterialButton btnWebsite = v.findViewById(R.id.btn_about_website);
        MaterialButton btnOnline = v.findViewById(R.id.btn_about_online);
        MaterialButton btnGithub = v.findViewById(R.id.btn_about_github);

        tvVersion.setText(getString(R.string.about_version, getVersionName()));

        btnCheckUpdate.setOnClickListener(x -> checkUpdate());
        btnWebsite.setOnClickListener(x -> openUrl(WEBSITE_URL));
        btnOnline.setOnClickListener(x -> openUrl(ONLINE_REPAIR_URL));
        btnGithub.setOnClickListener(x -> openUrl(GITHUB_URL));
        return v;
    }

    private String getVersionName() {
        try {
            PackageInfo p = requireContext().getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0);
            return p.versionName;
        } catch (Exception e) {
            return "1.0.0";
        }
    }

    /** 检查更新并提示。 */
    public void checkUpdate() {
        btnCheckUpdate.setEnabled(false);
        btnCheckUpdate.setText(R.string.checking_update);
        UpdateChecker.check(requireContext(), new UpdateChecker.Callback() {
            @Override
            public void onLatest(int versionCode, String versionName) {
                requireActivity().runOnUiThread(() -> {
                    btnCheckUpdate.setEnabled(true);
                    btnCheckUpdate.setText(R.string.btn_check_update);
                    showUpdateDialog(versionName);
                });
            }

            @Override
            public void onNoUpdate() {
                requireActivity().runOnUiThread(() -> {
                    btnCheckUpdate.setEnabled(true);
                    btnCheckUpdate.setText(R.string.btn_check_update);
                    Toast.makeText(requireContext(),
                            getString(R.string.no_update, getVersionName()), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String msg) {
                requireActivity().runOnUiThread(() -> {
                    btnCheckUpdate.setEnabled(true);
                    btnCheckUpdate.setText(R.string.btn_check_update);
                    Toast.makeText(requireContext(), R.string.check_update_fail, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void showUpdateDialog(String newVersion) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.update_dialog_title)
                .setMessage(getString(R.string.update_dialog_msg, getVersionName(), newVersion))
                .setPositiveButton(R.string.btn_download, (d, w) -> downloadAndInstall())
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void downloadAndInstall() {
        btnCheckUpdate.setEnabled(false);
        btnCheckUpdate.setText(R.string.update_downloading);
        UpdateChecker.download(requireContext(), apkDownloadUrl(), new UpdateChecker.DownloadCallback() {
            @Override
            public void onProgress(int percent) {
                requireActivity().runOnUiThread(() ->
                        btnCheckUpdate.setText(getString(R.string.update_downloading, percent)));
            }

            @Override
            public void onDone(File apk) {
                requireActivity().runOnUiThread(() -> {
                    btnCheckUpdate.setEnabled(true);
                    btnCheckUpdate.setText(R.string.btn_check_update);
                    Toast.makeText(requireContext(), R.string.update_download_ok, Toast.LENGTH_LONG).show();
                    UpdateChecker.install(requireActivity(), apk);
                });
            }

            @Override
            public void onError(String msg) {
                requireActivity().runOnUiThread(() -> {
                    btnCheckUpdate.setEnabled(true);
                    btnCheckUpdate.setText(R.string.btn_check_update);
                    Toast.makeText(requireContext(), R.string.update_download_fail, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /** 始终指向最新 release 的 APK 下载直链。 */
    private String apkDownloadUrl() {
        return "https://github.com/yancongya/mirecover/releases/latest/download/app-release.apk";
    }

    private void openUrl(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(requireContext(),
                    getString(R.string.howto_more_fail, url), Toast.LENGTH_LONG).show();
        }
    }
}
