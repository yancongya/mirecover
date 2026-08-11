package com.mirecover;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

/**
 * 主面板（Tab3）：源目录 / 修复目录 / 修复按钮 / 相册子目录 / 帮助。
 * 配置状态保存在此 Fragment，由 MainActivity 读取以执行修复与导出。
 */
public class HomeFragment extends Fragment {

    private EditText etSource;
    private EditText etRepair;
    private EditText etSubdir;
    private MaterialButton btnRepair;
    private TextView tvStatus;
    private TextView btnHelp;
    private View settingsMore;
    private ImageView ivChevron;
    private boolean moreExpanded = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_home, container, false);

        etSource = v.findViewById(R.id.et_source);
        etRepair = v.findViewById(R.id.et_repair);
        etSubdir = v.findViewById(R.id.et_subdir);
        btnRepair = v.findViewById(R.id.btn_repair);
        tvStatus = v.findViewById(R.id.tv_status);
        btnHelp = v.findViewById(R.id.btn_help_copy);
        settingsMore = v.findViewById(R.id.settings_more);
        ivChevron = v.findViewById(R.id.iv_chevron);
        View moreSettingsBtn = v.findViewById(R.id.more_settings_btn);
        View btnOpenSource = v.findViewById(R.id.btn_open_source);
        View btnOpenRepair = v.findViewById(R.id.btn_open_repair);

        MainActivity activity = (MainActivity) requireActivity();
        View btnHowTo = v.findViewById(R.id.btn_howto);

        etSource.setText(MainActivity.DEFAULT_SRC);
        etRepair.setText(MainActivity.DEFAULT_REPAIR);

        btnRepair.setOnClickListener(x -> activity.doRepair());
        btnHelp.setOnClickListener(x -> activity.showHelp());
        btnHowTo.setOnClickListener(x -> showHowTo());
        btnOpenSource.setOnClickListener(x -> activity.openDir(getSourcePath()));
        btnOpenRepair.setOnClickListener(x -> activity.openDir(getRepairPath()));
        moreSettingsBtn.setOnClickListener(x -> toggleMoreSettings());
        return v;
    }

    /** 弹出面向家人的「使用说明」对话框。 */
    private void showHowTo() {
        String msg = getString(R.string.howto_what_title) + "\n" + getString(R.string.howto_what)
                + "\n\n" + getString(R.string.howto_steps_title) + "\n"
                + getString(R.string.howto_step1) + "\n\n"
                + getString(R.string.howto_step2) + "\n\n"
                + getString(R.string.howto_step3) + "\n\n"
                + getString(R.string.howto_note_title) + "\n" + getString(R.string.howto_note);
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.howto_title)
                .setMessage(msg)
                .setPositiveButton(R.string.howto_ok, null)
                .show();
    }

    /** 展开/收起「更多设置」面板并旋转箭头。 */
    private void toggleMoreSettings() {
        moreExpanded = !moreExpanded;
        settingsMore.setVisibility(moreExpanded ? View.VISIBLE : View.GONE);
        ivChevron.animate().rotation(moreExpanded ? 180f : 0f).setDuration(200).start();
    }

    // ---------- 供 MainActivity 读取的配置 ----------

    public String getSourcePath() {
        return etSource == null ? "" : etSource.getText().toString().trim();
    }

    public String getRepairPath() {
        return etRepair == null ? "" : etRepair.getText().toString().trim();
    }

    public String getSubdir() {
        return etSubdir == null ? "Camera" : etSubdir.getText().toString().trim();
    }

    /** 设置状态文案（权限、修复进度等）。 */
    public void setStatus(CharSequence text) {
        if (tvStatus != null) tvStatus.setText(text);
    }

    /** 设置修复按钮可用性（修复中禁用）。 */
    public void setRepairBusy(boolean busy) {
        if (btnRepair == null) return;
        btnRepair.setEnabled(!busy);
        btnRepair.setText(busy ? R.string.repairing : R.string.btn_repair);
    }
}
