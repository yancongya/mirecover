package com.mirecover;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.mirecover.model.FileItem;

import java.util.List;

/**
 * Tab2 修复预览网格适配器：双列缩略图 + 右上角勾选徽标。
 * 点击切换勾选，长按预览大图。
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
        boolean checked = selected != null && position < selected.length && selected[position];

        Glide.with(holder.ivThumb.getContext())
                .load(it.file)
                .apply(new RequestOptions()
                        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                        .override(400, 400)
                        .centerCrop())
                .into(holder.ivThumb);

        holder.mask.setVisibility(checked ? View.VISIBLE : View.GONE);
        holder.badge.setVisibility(checked ? View.VISIBLE : View.INVISIBLE);

        final int pos = position;
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onTap(pos);
        });
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) listener.onLongPress(pos);
            return true;
        });
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
    }
}
