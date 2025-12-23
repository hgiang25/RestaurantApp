package com.example.restaurantapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.restaurantapp.R;
import com.example.restaurantapp.models.TableModel;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class TableBookingAdapter extends RecyclerView.Adapter<TableBookingAdapter.ViewHolder> {

    public interface OnTableBookListener {
        void onBookClick(TableModel table);
    }

    private List<TableModel> tables;
    private OnTableBookListener listener;

    public TableBookingAdapter(List<TableModel> tables, OnTableBookListener listener) {
        this.tables = tables;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_table_booking, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TableModel table = tables.get(position);
        holder.bind(table);
    }

    @Override
    public int getItemCount() {
        return tables.size();
    }

    public void updateData(List<TableModel> newTables) {
        this.tables = newTables;
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivTableIcon;
        TextView tvTableName, tvCapacity, tvStatus;
        MaterialButton btnBook;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivTableIcon = itemView.findViewById(R.id.ivTableIcon);
            tvTableName = itemView.findViewById(R.id.tvTableName);
            tvCapacity = itemView.findViewById(R.id.tvCapacity);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            btnBook = itemView.findViewById(R.id.btnBook);
        }

        public void bind(TableModel table) {
            tvTableName.setText(table.getName());
            tvCapacity.setText(table.getCapacity() + " người");

            boolean isFree = "free".equals(table.getStatus());
            if (isFree) {
                tvStatus.setText("Trống");
                tvStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.status_free));
                ivTableIcon.setBackgroundResource(R.drawable.bg_status_free);
                btnBook.setEnabled(true);
                btnBook.setAlpha(1f);
            } else {
                tvStatus.setText("Đang sử dụng");
                tvStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.status_occupied));
                ivTableIcon.setBackgroundResource(R.drawable.bg_status_occupied);
                btnBook.setEnabled(false);
                btnBook.setAlpha(0.5f);
            }

            btnBook.setOnClickListener(v -> {
                if (listener != null && isFree) {
                    listener.onBookClick(table);
                }
            });
        }
    }
}
