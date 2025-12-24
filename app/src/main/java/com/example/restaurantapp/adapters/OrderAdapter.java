package com.example.restaurantapp.adapters;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.restaurantapp.R;
import com.example.restaurantapp.models.OrderItem;
import com.example.restaurantapp.models.OrderModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {

    public interface OnOrderClickListener {
        void onOrderClick(OrderModel order);
    }

    private final List<OrderModel> orderList;
    private final OnOrderClickListener listener;

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
        holder.bind(orderList.get(position));
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

            // ===== ORDER ID =====
            if (order.getId() != null) {
                txtOrderId.setText("Đơn #" + order.getId()
                        .substring(0, Math.min(8, order.getId().length())));
            }

            // ===== ORDER ITEMS (FIX CHẮC CHẮN HIỆN TÊN MÓN) =====
            StringBuilder itemsText = new StringBuilder();

            if (order.getItems() != null) {
                for (Object obj : order.getItems()) {

                    // Trường hợp map đúng OrderItem
                    if (obj instanceof OrderItem) {
                        OrderItem item = (OrderItem) obj;
                        if (item.getName() != null) {
                            itemsText.append(item.getName())
                                    .append(" x")
                                    .append(item.getQuantity())
                                    .append("\n");
                        }
                    }

                    // Trường hợp Firestore trả raw Map (GIỐNG CustomerOrderAdapter)
                    else if (obj instanceof java.util.Map) {
                        java.util.Map<String, Object> map =
                                (java.util.Map<String, Object>) obj;

                        String name = (String) map.get("name");
                        Object qtyObj = map.get("quantity");
                        int qty = qtyObj instanceof Number
                                ? ((Number) qtyObj).intValue()
                                : 1;

                        if (name != null) {
                            itemsText.append(name)
                                    .append(" x")
                                    .append(qty)
                                    .append("\n");
                        }
                    }
                }
            }

            txtOrderItems.setText(
                    itemsText.length() > 0
                            ? itemsText.toString().trim()
                            : "Không có món"
            );


            // ===== TIME =====
            if (order.getCreatedAt() != null) {
                SimpleDateFormat sdf =
                        new SimpleDateFormat("HH:mm dd/MM", Locale.getDefault());
                txtOrderTime.setText(sdf.format(order.getCreatedAt().toDate()));
            }

            // ===== STATUS =====
            chipStatus.setText(order.getStatusDisplay());

            int color;
            switch (order.getStatus()) {
                case "pending":
                    color = 0xFFFF9800;
                    break;
                case "confirmed":
                    color = 0xFF2196F3;
                    break;
                case "preparing":
                    color = 0xFF9C27B0;
                    break;
                case "served":
                    color = 0xFF4CAF50;
                    break;
                case "paid":
                    color = 0xFF607D8B;
                    break;
                case "cancelled":
                    color = 0xFFF44336;
                    break;
                default:
                    color = 0xFF9E9E9E;
            }

            chipStatus.setChipBackgroundColor(ColorStateList.valueOf(color));

            // ===== CLICK =====
            btnUpdateStatus.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onOrderClick(order);
                }
            });
        }
    }
}
