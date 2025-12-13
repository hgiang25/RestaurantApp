package com.example.restaurantapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.restaurantapp.R;
import com.example.restaurantapp.models.OrderModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {

    public interface OnOrderClickListener {
        void onOrderClick(OrderModel order);
    }

    private List<OrderModel> orderList;
    private OnOrderClickListener listener;

    public OrderAdapter(List<OrderModel> orderList, OnOrderClickListener listener) {
        this.orderList = orderList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        OrderModel order = orderList.get(position);
        holder.bind(order);
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView txtOrderId, txtOrderItems, txtOrderTime;
        Chip chipStatus;
        MaterialButton btnUpdateStatus;

        OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            txtOrderId = itemView.findViewById(R.id.txtOrderId);
            txtOrderItems = itemView.findViewById(R.id.txtOrderItems);
            txtOrderTime = itemView.findViewById(R.id.txtOrderTime);
            chipStatus = itemView.findViewById(R.id.chipStatus);
            btnUpdateStatus = itemView.findViewById(R.id.btnUpdateStatus);
        }

        void bind(OrderModel order) {
            txtOrderId.setText("Đơn #" + order.getId().substring(0, Math.min(8, order.getId().length())));

            // Hiển thị items
            StringBuilder itemsText = new StringBuilder();
            if (order.getItems() != null) {
                for (Object item : order.getItems()) {
                    if (item instanceof Map) {
                        Map<String, Object> itemMap = (Map<String, Object>) item;
                        String name = (String) itemMap.get("name");
                        Object qtyObj = itemMap.get("quantity");
                        int qty = qtyObj instanceof Number ? ((Number) qtyObj).intValue() : 1;
                        if (name != null) {
                            itemsText.append(name).append(" x").append(qty).append("\n");
                        }
                    }
                }
            }
            txtOrderItems.setText(itemsText.toString().trim());

            // Hiển thị thời gian
            if (order.getCreatedAt() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm dd/MM", Locale.getDefault());
                txtOrderTime.setText(sdf.format(order.getCreatedAt().toDate()));
            }

            // Hiển thị status
            String statusText;
            int statusColor;
            switch (order.getStatus()) {
                case "pending":
                    statusText = "Chờ xác nhận";
                    statusColor = 0xFFFF9800;
                    break;
                case "confirmed":
                    statusText = "Đã xác nhận";
                    statusColor = 0xFF2196F3;
                    break;
                case "preparing":
                    statusText = "Đang chuẩn bị";
                    statusColor = 0xFF9C27B0;
                    break;
                case "served":
                    statusText = "Đã phục vụ";
                    statusColor = 0xFF4CAF50;
                    break;
                case "paid":
                    statusText = "Đã thanh toán";
                    statusColor = 0xFF607D8B;
                    break;
                default:
                    statusText = order.getStatus();
                    statusColor = 0xFF9E9E9E;
            }
            chipStatus.setText(statusText);
            chipStatus.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(statusColor));

            btnUpdateStatus.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onOrderClick(order);
                }
            });
        }
    }
}
