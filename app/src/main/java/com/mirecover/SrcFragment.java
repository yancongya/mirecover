package com.mirecover;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mirecover.model.FileItem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Tab1：源文件（.0）列表。
 * 显示源缓存目录下的 .0 文件；点击某一行可打开对应目录（用文件管理器定位）。
 */
public class SrcFragment extends Fragment {

    private TextView tvStatus;
    private RecyclerView recyclerView;
    private SrcAdapter adapter;
    private List<FileItem> files = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_src, container, false);
        tvStatus = v.findViewById(R.id.tv_src_status);
        recyclerView = v.findViewById(R.id.list_src);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new SrcAdapter(files);
        adapter.setOnItemClickListener(this::openContainingDir);
        recyclerView.setAdapter(adapter);
        return v;
    }

    /** 由 Activity 在扫描/修复后调用，刷新 .0 列表。 */
    public void showSource(String srcPath) {
        if (getView() == null) return;
        tvStatus.setText(R.string.loading);
        List<FileItem> result = FileScanner.scan(srcPath, ".0").files;
        files.clear();
        files.addAll(result);
        adapter.notifyDataSetChanged();
        if (result.isEmpty()) {
            tvStatus.setText(getString(R.string.empty_src) + "\n" + srcPath);
        } else {
            tvStatus.setText(getString(R.string.src_done, result.size(), srcPath));
        }
    }

    /** 点击打开对应目录：弹窗显示路径，并用 SAF 打开系统文件管理器定位。 */
    private void openContainingDir(int position) {
        if (position < 0 || position >= files.size()) return;
        FileItem it = files.get(position);
        File dir = it.file.getParentFile();
        if (dir == null || !dir.exists()) {
            new AlertDialog.Builder(requireContext())
                    .setMessage("目录不存在")
                    .setPositiveButton("确定", null).show();
            return;
        }
        String path = dir.getAbsolutePath();
        new AlertDialog.Builder(requireContext())
                .setTitle("文件所在目录")
                .setMessage(path)
                .setPositiveButton("打开目录", (d, w) -> openViaSAF())
                .setNegativeButton("复制路径", (d, w) -> copyPath(path))
                .show();
    }

    /** 用系统文件选择器(SAF)打开目录，用户可导航到目标位置。 */
    private void openViaSAF() {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception e) {
            new AlertDialog.Builder(requireContext())
                    .setMessage("无法打开文件管理器")
                    .setPositiveButton("确定", null).show();
        }
    }

    private void copyPath(String path) {
        ClipboardManager cm =
                (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("path", path));
        Toast.makeText(requireContext(), "路径已复制", Toast.LENGTH_SHORT).show();
    }
}
