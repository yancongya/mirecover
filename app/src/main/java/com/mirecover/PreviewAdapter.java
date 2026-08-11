package com.mirecover;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.mirecover.model.FileItem;

import java.util.List;

/**
 * 修复预览网格适配器（优化版）：
 * - 缩略图 Glide 加载，淡入过渡 + 占位底色
 * - 选中时徽标缩放弹出、遮罩淡入；取消时反向动画
 * - 点击切换勾选，长按预览大图
 */
public class PreviewAdapter extends RecyclerView.Adapter<PreviewAdapter.VH> {

    public interface OnItemClick {
        void onTap(int position);
        void onLongPress(int position);
    }

    private final List<FileItem> items;
    private final boolean[] selected;
    private OnItemClick listener;

    public PreviewAdapter(List<FileItem> items, boolean[] selected) {
        this.items = items;
        this.selected = selected;
    }

    public void setOnItemClickListener(OnItemClick l) {
        this.listener = l;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_preview, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        FileItem it = items.get(position);
        boolean checked = isSelected(position);

        Glide.with(holder.ivThumb.getContext())
                .load(it.file)
                .apply(new RequestOptions()
                        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                        .format(DecodeFormat.PREFER_RGB_565)   // 缩略图省内存
                        .override(600, 600)
                        .centerCrop()
                        .placeholder(0))
                .thumbnail(0.5f)
                .into(holder.ivThumb);

        // 选中态：初始化最终外观（列表首次加载不播放动画）
        holder.mask.setAlpha(checked ? 1f : 0f);
        holder.mask.setVisibility(View.VISIBLE);
        holder.badge.setScaleX(checked ? 1f : 0f);
        holder.badge.setScaleY(checked ? 1f : 0f);
        holder.badge.setAlpha(checked ? 1f : 0f);
        holder.badge.setVisibility(checked ? View.VISIBLE : View.GONE);

        final int pos = position;
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onTap(pos);
        });
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) listener.onLongPress(pos);
            return true;
        });
    }

    /** 刷新单个 item 的选中外观（配合 DiffUtil/局部刷新调用）。 */
    public void notifySelectionChanged(int position, boolean checked) {
        if (position < 0 || position >= items.size()) return;
        notifyItemChanged(position);
    }

    private boolean isSelected(int position) {
        return selected != null && position >= 0 && position < selected.length && selected[position];
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ImageView ivThumb;
        final View mask;
        final View badge;

        VH(@NonNull View itemView) {
            super(itemView);
            ivThumb = itemView.findViewById(R.id.iv_thumb);
            mask = itemView.findViewById(R.id.iv_mask);
            badge = itemView.findViewById(R.id.badge);
        }

        /** 播放选中/取消动画。 */
        void animateSelection(boolean checked) {
            badge.animate().cancel();
            badge.setScaleX(0.6f);
            badge.setScaleY(0.6f);
            badge.setAlpha(0f);
            badge.setVisibility(View.VISIBLE);
            badge.animate()
                    .scaleX(1f).scaleY(1f).alpha(1f)
                    .setDuration(180)
                    .start();
            mask.animate()
                    .alpha(checked ? 1f : 0f)
                    .setDuration(180)
                    .start();
        }
    }
}
