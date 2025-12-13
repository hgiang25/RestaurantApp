package com.example.restaurantapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.restaurantapp.R;
import com.example.restaurantapp.models.OrderModel;
import com.google.android.material.chip.Chip;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CustomerOrderAdapter extends RecyclerView.Adapter<CustomerOrderAdapter.CustomerOrderViewHolder> {

    private List<OrderModel> orderList;

    public CustomerOrderAdapter(List<OrderModel> orderList) {
        this.orderList = orderList;
    }

    @NonNull
    @Override
    public CustomerOrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_customer_order, parent, false);
        return new CustomerOrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CustomerOrderViewHolder holder, int position) {
        OrderModel order = orderList.get(position);
        holder.bind(order);
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    class CustomerOrderViewHolder extends RecyclerView.ViewHolder {
        TextView txtOrderId, txtOrderItems, txtOrderTime, txtTotal;
        Chip chipStatus;

        CustomerOrderViewHolder(@NonNull View itemView) {
            super(itemView);
            txtOrderId = itemView.findViewById(R.id.txtOrderId);
            txtOrderItems = itemView.findViewById(R.id.txtOrderItems);
            txtOrderTime = itemView.findViewById(R.id.txtOrderTime);
            txtTotal = itemView.findViewById(R.id.txtTotal);
            chipStatus = itemView.findViewById(R.id.chipStatus);
        }

        void bind(OrderModel order) {
            txtOrderId.setText("Đơn #" + order.getId().substring(0, Math.min(8, order.getId().length())));

            // Hiển thị items và tính tổng
            StringBuilder itemsText = new StringBuilder();
            double total = 0;
            if (order.getItems() != null) {
                for (Object item : order.getItems()) {
                    if (item instanceof Map) {
                        Map<String, Object> itemMap = (Map<String, Object>) item;
                        String name = (String) itemMap.get("name");
                        Object qtyObj = itemMap.get("quantity");
                        Object priceObj = itemMap.get("price");
                        int qty = qtyObj instanceof Number ? ((Number) qtyObj).intValue() : 1;
                        double price = priceObj instanceof Number ? ((Number) priceObj).doubleValue() : 0;
                        total += price * qty;
                        if (name != null) {
                            itemsText.append("• ").append(name).append(" x").append(qty).append("\n");
                        }
                    }
                }
            }
            txtOrderItems.setText(itemsText.toString().trim());
            txtTotal.setText(String.format("Tổng: %,.0f đ", total));

            // Hiển thị thời gian
            if (order.getCreatedAt() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault());
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
        }
    }
}
