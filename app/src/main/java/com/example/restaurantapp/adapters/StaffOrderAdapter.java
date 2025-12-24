package com.example.restaurantapp.adapters;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.restaurantapp.R;
import com.example.restaurantapp.models.OrderModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StaffOrderAdapter extends RecyclerView.Adapter<StaffOrderAdapter.OrderViewHolder> {

    public interface OnOrderActionListener {
        void onConfirm(OrderModel order);
        void onPreparing(OrderModel order);
        void onServed(OrderModel order);
        void onPaid(OrderModel order);
        void onCancel(OrderModel order);
        void onViewDetails(OrderModel order);
    }

    private List<OrderModel> orderList;
    private OnOrderActionListener listener;

    public StaffOrderAdapter(List<OrderModel> orderList, OnOrderActionListener listener) {
        this.orderList = orderList;
        this.listener = listener;
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        OrderModel order = orderList.get(position);
        return order.getId() != null ? order.getId().hashCode() : position;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_staff_order, parent, false);
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

    public void updateData(List<OrderModel> newList) {
        this.orderList = newList;
        notifyDataSetChanged();
    }

    class OrderViewHolder extends RecyclerView.ViewHolder {
        View statusIndicator;
        TextView txtOrderId, txtTableInfo, txtOrderType, txtOrderItems, txtOrderTime, txtTotalPrice;
        Chip chipStatus;
        MaterialButton btnAction1, btnAction2, btnCancel;

        OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            statusIndicator = itemView.findViewById(R.id.statusIndicator);
            txtOrderId = itemView.findViewById(R.id.txtOrderId);
            txtTableInfo = itemView.findViewById(R.id.txtTableInfo);
            txtOrderType = itemView.findViewById(R.id.txtOrderType);
            txtOrderItems = itemView.findViewById(R.id.txtOrderItems);
            txtOrderTime = itemView.findViewById(R.id.txtOrderTime);
            txtTotalPrice = itemView.findViewById(R.id.txtTotalPrice);
            chipStatus = itemView.findViewById(R.id.chipStatus);
            btnAction1 = itemView.findViewById(R.id.btnAction1);
            btnAction2 = itemView.findViewById(R.id.btnAction2);
            btnCancel = itemView.findViewById(R.id.btnCancel);
        }

        void bind(OrderModel order) {
            // Order ID
            String shortId = order.getId().length() > 8 
                    ? order.getId().substring(0, 8).toUpperCase() 
                    : order.getId().toUpperCase();
            txtOrderId.setText("Đơn #" + shortId);

            // Table/Delivery info
            String orderType = order.getOrderType();
            if ("dine_in".equals(orderType)) {
                String tableName = order.getTableName();
                txtTableInfo.setText("🪑 " + (tableName != null ? tableName : "Bàn không xác định"));
                txtOrderType.setText("Ăn tại chỗ");
                txtOrderType.setBackgroundTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(itemView.getContext(), R.color.status_confirmed)));
            } else if ("takeaway".equals(orderType)) {
                String address = order.getDeliveryAddress();
                txtTableInfo.setText("🏠 " + (address != null ? address : "Mang về"));
                txtOrderType.setText("Mang về");
                txtOrderType.setBackgroundTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(itemView.getContext(), R.color.secondary)));
            } else {
                txtTableInfo.setText("📋 Đơn hàng");
                txtOrderType.setVisibility(View.GONE);
            }

            // Order items
            StringBuilder itemsText = new StringBuilder();
            int itemCount = 0;
            if (order.getItems() != null) {
                for (Object item : order.getItems()) {
                    if (item instanceof Map) {
                        Map<String, Object> itemMap = (Map<String, Object>) item;
                        String name = (String) itemMap.get("name");
                        Object qtyObj = itemMap.get("quantity");
                        int qty = qtyObj instanceof Number ? ((Number) qtyObj).intValue() : 1;
                        if (name != null) {
                            itemsText.append("• ").append(name).append(" x").append(qty).append("\n");
                            itemCount++;
                        }
                    }
                }
            }
            txtOrderItems.setText(itemsText.toString().trim());

            // Time
            if (order.getCreatedAt() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm dd/MM", Locale.getDefault());
                txtOrderTime.setText("🕐 " + sdf.format(order.getCreatedAt().toDate()));
            }

            // Total price
            Double total = order.getTotal();
            if (total != null && total > 0) {
                txtTotalPrice.setText(String.format(Locale.getDefault(), "%,.0fđ", total));
                txtTotalPrice.setVisibility(View.VISIBLE);
            } else {
                txtTotalPrice.setVisibility(View.GONE);
            }

            // Status styling
            String status = order.getStatus() != null ? order.getStatus() : "pending";
            String statusText;
            int statusColor;
            int indicatorColor;

            switch (status) {
                case "confirmed":
                    statusText = "Đã xác nhận";
                    statusColor = R.color.status_confirmed;
                    indicatorColor = R.color.status_confirmed;
                    break;
                case "preparing":
                    statusText = "Đang chuẩn bị";
                    statusColor = R.color.status_pending;
                    indicatorColor = R.color.status_pending;
                    break;
                case "served":
                    statusText = "Đã phục vụ";
                    statusColor = R.color.status_free;
                    indicatorColor = R.color.status_free;
                    break;
                case "paid":
                    statusText = "Đã thanh toán";
                    statusColor = R.color.text_secondary;
                    indicatorColor = R.color.text_secondary;
                    break;
                case "cancelled":
                    statusText = "Đã hủy";
                    statusColor = R.color.status_cancelled;
                    indicatorColor = R.color.status_cancelled;
                    break;
                default: // pending
                    statusText = "Chờ xác nhận";
                    statusColor = R.color.status_pending;
                    indicatorColor = R.color.status_pending;
            }

            chipStatus.setText(statusText);
            chipStatus.setChipBackgroundColor(ColorStateList.valueOf(
                    ContextCompat.getColor(itemView.getContext(), statusColor)));
            statusIndicator.setBackgroundColor(
                    ContextCompat.getColor(itemView.getContext(), indicatorColor));

            // Action buttons based on status
            btnAction1.setVisibility(View.GONE);
            btnAction2.setVisibility(View.GONE);
            btnCancel.setVisibility(View.GONE);

            switch (status) {
                case "pending":
                    // Pending: Xác nhận / Hủy
                    btnAction1.setVisibility(View.VISIBLE);
                    btnAction1.setText("✓ Xác nhận");
                    btnAction1.setBackgroundTintList(ColorStateList.valueOf(
                            ContextCompat.getColor(itemView.getContext(), R.color.status_confirmed)));
                    btnAction1.setOnClickListener(v -> {
                        if (listener != null) listener.onConfirm(order);
                    });

                    btnCancel.setVisibility(View.VISIBLE);
                    btnCancel.setOnClickListener(v -> {
                        if (listener != null) listener.onCancel(order);
                    });
                    break;

                case "confirmed":
                    // Confirmed: Bắt đầu nấu
                    btnAction1.setVisibility(View.VISIBLE);
                    btnAction1.setText("🍳 Bắt đầu nấu");
                    btnAction1.setBackgroundTintList(ColorStateList.valueOf(
                            ContextCompat.getColor(itemView.getContext(), R.color.status_pending)));
                    btnAction1.setOnClickListener(v -> {
                        android.util.Log.d("StaffOrderAdapter", "=== NÚT BẮT ĐẦU NẤU ĐƯỢC NHẤN ===");
                        android.util.Log.d("StaffOrderAdapter", "Order ID: " + order.getId());
                        android.util.Log.d("StaffOrderAdapter", "Listener null? " + (listener == null));
                        if (listener != null) listener.onPreparing(order);
                    });
                    break;

                case "preparing":
                    // Preparing: Phục vụ
                    btnAction1.setVisibility(View.VISIBLE);
                    btnAction1.setText("🍽 Phục vụ");
                    btnAction1.setBackgroundTintList(ColorStateList.valueOf(
                            ContextCompat.getColor(itemView.getContext(), R.color.status_free)));
                    btnAction1.setOnClickListener(v -> {
                        if (listener != null) listener.onServed(order);
                    });
                    break;

                case "served":
                    // Served: Thanh toán
                    btnAction1.setVisibility(View.VISIBLE);
                    btnAction1.setText("💰 Thanh toán");
                    btnAction1.setBackgroundTintList(ColorStateList.valueOf(
                            ContextCompat.getColor(itemView.getContext(), R.color.secondary)));
                    btnAction1.setOnClickListener(v -> {
                        if (listener != null) listener.onPaid(order);
                    });
                    break;

                // paid/cancelled: no actions
            }

            // View details on card click
            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onViewDetails(order);
            });
        }
    }
}
