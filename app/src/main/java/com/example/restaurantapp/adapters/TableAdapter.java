package com.example.restaurantapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.restaurantapp.R;
import com.example.restaurantapp.models.TableModel;

import java.util.List;

public class TableAdapter extends RecyclerView.Adapter<TableAdapter.TableViewHolder> {

    public interface OnTableClickListener {
        void onTableClick(TableModel table);
    }

    private final List<TableModel> tableList;
    private final OnTableClickListener listener;

    public TableAdapter(List<TableModel> tableList, OnTableClickListener listener) {
        this.tableList = tableList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TableViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_table, parent, false);
        return new TableViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TableViewHolder holder, int position) {
        TableModel table = tableList.get(position);
        holder.bind(table);
    }

    @Override
    public int getItemCount() {
        return tableList.size();
    }

    class TableViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView txtTableName, txtCapacity, txtStatus;
        ImageView imgTable;

        TableViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardTable);
            txtTableName = itemView.findViewById(R.id.txtTableName);
            txtCapacity = itemView.findViewById(R.id.txtCapacity);
            txtStatus = itemView.findViewById(R.id.txtStatus);
            imgTable = itemView.findViewById(R.id.imgTable);
        }

        void bind(TableModel table) {
            txtTableName.setText(table.getName());
            txtCapacity.setText(table.getCapacity() + " chỗ");

            if ("free".equals(table.getStatus())) {
                txtStatus.setText("Trống");
                txtStatus.setTextColor(itemView.getContext().getColor(android.R.color.holo_green_dark));
                cardView.setCardBackgroundColor(itemView.getContext().getColor(android.R.color.white));
            } else {
                txtStatus.setText("Có khách");
                txtStatus.setTextColor(itemView.getContext().getColor(android.R.color.holo_red_dark));
                cardView.setCardBackgroundColor(0xFFFFEBEE);
            }

            // Hiển thị ảnh bàn
            if (table.getImageUrl() != null && !table.getImageUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(table.getImageUrl())
                        .placeholder(R.drawable.ic_table)
                        .error(R.drawable.ic_table)
                        .into(imgTable);
            } else {
                imgTable.setImageResource(R.drawable.ic_table);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onTableClick(table);
            });
        }
    }
}
