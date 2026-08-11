package com.mirecover;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.button.MaterialButton;
import com.mirecover.model.FileItem;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Tab2：修复预览。
 * 双列缩略图网格；支持勾选/全选、点击或长按预览、导出到相册。
 */
public class PreviewFragment extends Fragment {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private List<FileItem> repairedList = new ArrayList<>();
    private boolean[] selected = new boolean[0];

    private TextView tvStatus;
    private RecyclerView recyclerView;
    private MaterialCheckBox cbSelectAll;
    private MaterialButton btnExport;
    private PreviewAdapter adapter;

    private String repairDir = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_preview, container, false);
        tvStatus = v.findViewById(R.id.tv_preview_status);
        recyclerView = v.findViewById(R.id.list_preview);
        cbSelectAll = v.findViewById(R.id.cb_select_all_preview);
        btnExport = v.findViewById(R.id.btn_export_preview);
        MaterialButton btnRefresh = v.findViewById(R.id.btn_refresh_preview);

        // 自适应列数：手机 2 列，宽屏 3 列
        int span = 2;
        float dpWidth = requireContext().getResources().getDisplayMetrics().widthPixels
                / requireContext().getResources().getDisplayMetrics().density;
        if (dpWidth >= 600f) span = 3;      // 平板/横屏
        else if (dpWidth >= 480f) span = 2;

        GridLayoutManager glm = new GridLayoutManager(requireContext(), span);
        recyclerView.setLayoutManager(glm);
        adapter = new PreviewAdapter(repairedList, selected);
        setupAdapterListener();
        recyclerView.setAdapter(adapter);

        cbSelectAll.setOnCheckedChangeListener((b, checked) -> {
            for (int i = 0; i < selected.length; i++) selected[i] = checked;
            adapter.notifyDataSetChanged();
        });
        btnRefresh.setOnClickListener(view -> loadRepaired());
        btnExport.setOnClickListener(view -> doExport());

        // 若修复目录已设置（修复后创建的 fragment），view 创建后自动加载
        if (repairDir != null && !repairDir.isEmpty()) {
            loadRepaired();
        }
        return v;
    }

    /** 由 Activity 传入修复目录路径。 */
    public void setRepairDir(String dir) {
        this.repairDir = dir;
    }

    /** 重新扫描修复目录并刷新列表。 */
    public void loadRepaired() {
        if (getView() == null || repairDir == null || repairDir.isEmpty()) return;
        tvStatus.setText(R.string.loading);
        executor.execute(() -> {
            List<FileItem> result = FileScanner.scan(repairDir, ".jpg").files;
            requireActivity().runOnUiThread(() -> {
                repairedList.clear();
                repairedList.addAll(result);
                selected = new boolean[repairedList.size()];
                adapter = new PreviewAdapter(repairedList, selected);
                setupAdapterListener();
                recyclerView.setAdapter(adapter);
                if (repairedList.isEmpty()) {
                    tvStatus.setText(getString(R.string.empty_preview) + "\n" + repairDir);
                } else {
                    tvStatus.setText(getString(R.string.preview_done, repairedList.size(), repairDir));
                }
                updateSelectAllState();
            });
        });
    }

    /** 为适配器设置点击(勾选)/长按(预览)回调。 */
    private void setupAdapterListener() {
        adapter.setOnItemClickListener(new PreviewAdapter.OnItemClick() {
            @Override
            public void onTap(int position) {
                if (position >= 0 && position < selected.length) {
                    selected[position] = !selected[position];
                    // 播放选中动效（徽标缩放 + 遮罩淡入）
                    RecyclerView.ViewHolder h =
                            recyclerView.findViewHolderForAdapterPosition(position);
                    if (h instanceof PreviewAdapter.VH) {
                        ((PreviewAdapter.VH) h).animateSelection(selected[position]);
                    }
                    adapter.notifyItemChanged(position);
                    updateSelectAllState();
                }
            }

            @Override
            public void onLongPress(int position) {
                if (position >= 0 && position < repairedList.size()) {
                    FileItem it = repairedList.get(position);
                    ImagePreviewDialog.show(requireContext(),
                            it.file.getAbsolutePath(), it.name);
                }
            }
        });
    }

    private void updateSelectAllState() {
        boolean all = selected.length > 0;
        for (boolean b : selected) { if (!b) { all = false; break; } }
        cbSelectAll.setOnCheckedChangeListener(null);
        cbSelectAll.setChecked(all);
        cbSelectAll.setOnCheckedChangeListener((b, checked) -> {
            for (int i = 0; i < selected.length; i++) selected[i] = checked;
            adapter.notifyDataSetChanged();
        });
    }

    private void doExport() {
        List<FileItem> picked = new ArrayList<>();
        for (int i = 0; i < repairedList.size(); i++) {
            if (i < selected.length && selected[i]) picked.add(repairedList.get(i));
        }
        if (picked.isEmpty()) {
            Toast.makeText(requireContext(), R.string.select_none, Toast.LENGTH_SHORT).show();
            return;
        }
        String subdir = getSubdir();
        tvStatus.setText(R.string.exporting);
        btnExport.setEnabled(false);
        executor.execute(() -> {
            ExportJob.ExportResult r = ExportJob.export(requireContext(), picked, subdir);
            MediaRefresh.refresh(requireContext(), ImageRecover.sanitizeSubdir(subdir));
            requireActivity().runOnUiThread(() -> {
                tvStatus.setText(getString(R.string.export_done, r.exported, r.failed, r.subdir));
                btnExport.setEnabled(true);
                Toast.makeText(requireContext(),
                        getString(R.string.exported_toast, r.exported), Toast.LENGTH_LONG).show();
            });
        });
    }

    private String getSubdir() {
        if (getActivity() instanceof MainActivity) {
            return ((MainActivity) getActivity()).getSubdir();
        }
        return "Camera";
    }

    @Override
    public void onDestroy() {
        executor.shutdown();
        super.onDestroy();
    }
}
