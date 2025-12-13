package com.example.restaurantapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.restaurantapp.R;
import com.example.restaurantapp.models.InventoryModel;

import java.util.List;

public class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.ViewHolder> {

    private List<InventoryModel> inventoryList;
    private OnInventoryClickListener listener;

    public interface OnInventoryClickListener {
        void onInventoryClick(InventoryModel inventory);
    }

    public InventoryAdapter(List<InventoryModel> inventoryList, OnInventoryClickListener listener) {
        this.inventoryList = inventoryList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_inventory, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        InventoryModel inventory = inventoryList.get(position);
        holder.bind(inventory);
    }

    @Override
    public int getItemCount() {
        return inventoryList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvQuantity, tvUnit, tvStatus;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvItemName);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);
            tvUnit = itemView.findViewById(R.id.tvUnit);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }

        void bind(InventoryModel inventory) {
            tvName.setText(inventory.getName());
            tvQuantity.setText(String.format("%.1f", inventory.getQuantity()));
            tvUnit.setText(inventory.getUnit());

            if (inventory.isLowStock()) {
                tvStatus.setText("Sắp hết");
                tvStatus.setTextColor(0xFFF44336);
                itemView.setBackgroundColor(0x1AF44336);
            } else {
                tvStatus.setText("Còn hàng");
                tvStatus.setTextColor(0xFF4CAF50);
                itemView.setBackgroundColor(0xFFFFFFFF);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onInventoryClick(inventory);
                }
            });
        }
    }
}
