package com.mirecover;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mirecover.model.FileItem;

import java.util.List;

/**
 * Tab1 源 .0 文件列表适配器：每条显示图标 + 文件名 + 大小/时间。
 */
public class SrcAdapter extends RecyclerView.Adapter<SrcAdapter.VH> {

    public interface OnItemClick {
        void onClick(int position);
    }

    private final List<FileItem> items;
    private OnItemClick listener;

    public SrcAdapter(List<FileItem> items) {
        this.items = items;
    }

    public void setOnItemClickListener(OnItemClick l) {
        this.listener = l;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_file, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        FileItem it = items.get(position);
        holder.tvName.setText(it.name);
        holder.tvMeta.setText(it.sizeText() + " · " + it.modTimeText());
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(holder.getBindingAdapterPosition());
        });
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvName;
        final TextView tvMeta;

        VH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_name);
            tvMeta = itemView.findViewById(R.id.tv_meta);
        }
    }
}
