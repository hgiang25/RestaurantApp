package com.example.restaurantapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.restaurantapp.R;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OrderHistoryAdapter extends RecyclerView.Adapter<OrderHistoryAdapter.ViewHolder> {

    private final List<DocumentSnapshot> orders;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    public OrderHistoryAdapter(List<DocumentSnapshot> orders) {
        this.orders = orders;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DocumentSnapshot doc = orders.get(position);

        // Order ID
        String orderId = doc.getId();
        holder.txtOrderId.setText("#" + orderId.substring(0, Math.min(8, orderId.length())).toUpperCase());

        // Date
        Timestamp timestamp = doc.getTimestamp("createdAt");
        if (timestamp != null) {
            holder.txtDate.setText(dateFormat.format(timestamp.toDate()));
        } else {
            holder.txtDate.setText("N/A");
        }

        // Status
        String status = doc.getString("status");
        holder.txtStatus.setText(getStatusText(status));
        holder.txtStatus.setBackgroundResource(getStatusBackground(status));
        holder.txtStatus.setTextColor(holder.itemView.getContext().getColor(getStatusColor(status)));

        // Order type
        String orderType = doc.getString("orderType");
        if ("takeaway".equals(orderType)) {
            holder.txtOrderType.setText("Mang về");
            holder.txtOrderType.setVisibility(View.VISIBLE);
        } else if ("dine_in".equals(orderType)) {
            String tableName = doc.getString("tableName");
            holder.txtOrderType.setText("Tại bàn: " + (tableName != null ? tableName : "N/A"));
            holder.txtOrderType.setVisibility(View.VISIBLE);
        } else {
            holder.txtOrderType.setVisibility(View.GONE);
        }

        // Items
        List<Map<String, Object>> items = (List<Map<String, Object>>) doc.get("items");
        if (items != null && !items.isEmpty()) {
            StringBuilder itemsText = new StringBuilder();
            for (int i = 0; i < items.size(); i++) {
                Map<String, Object> item = items.get(i);
                String name = (String) item.get("name");
                Long qty = (Long) item.get("quantity");
                if (i > 0) itemsText.append(", ");
                itemsText.append(name).append(" x").append(qty);
            }
            holder.txtItems.setText(itemsText.toString());
        } else {
            holder.txtItems.setText("Không có món");
        }

        // Total
        Double total = doc.getDouble("total");
        if (total == null) {
            total = doc.getDouble("totalPrice");
        }
        holder.txtTotal.setText(String.format(Locale.getDefault(), "%,.0fđ", total != null ? total : 0));

        // Discount info
        Double discount = doc.getDouble("discount");
        if (discount != null && discount > 0) {
            holder.layoutDiscount.setVisibility(View.VISIBLE);
            holder.txtDiscount.setText(String.format(Locale.getDefault(), "-%,.0fđ", discount));
        } else {
            holder.layoutDiscount.setVisibility(View.GONE);
        }
    }

    private String getStatusText(String status) {
        if (status == null) return "Không xác định";
        switch (status) {
            case "pending": return "Chờ xác nhận";
            case "confirmed": return "Đã xác nhận";
            case "preparing": return "Đang chuẩn bị";
            case "ready": return "Sẵn sàng";
            case "completed": return "Hoàn thành";
            case "cancelled": return "Đã hủy";
            default: return status;
        }
    }

    private int getStatusBackground(String status) {
        if (status == null) return R.drawable.bg_status_pending;
        switch (status) {
            case "pending": return R.drawable.bg_status_pending;
            case "confirmed":
            case "preparing": return R.drawable.bg_status_confirmed;
            case "ready": return R.drawable.bg_status_ready;
            case "completed": return R.drawable.bg_status_completed;
            case "cancelled": return R.drawable.bg_status_cancelled;
            default: return R.drawable.bg_status_pending;
        }
    }

    private int getStatusColor(String status) {
        if (status == null) return R.color.status_pending;
        switch (status) {
            case "pending": return R.color.status_pending;
            case "confirmed":
            case "preparing": return R.color.status_confirmed;
            case "ready": return R.color.status_ready;
            case "completed": return R.color.status_completed;
            case "cancelled": return R.color.status_cancelled;
            default: return R.color.status_pending;
        }
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtOrderId, txtDate, txtStatus, txtOrderType, txtItems, txtTotal, txtDiscount;
        LinearLayout layoutDiscount;

        ViewHolder(View itemView) {
            super(itemView);
            txtOrderId = itemView.findViewById(R.id.txtOrderId);
            txtDate = itemView.findViewById(R.id.txtDate);
            txtStatus = itemView.findViewById(R.id.txtStatus);
            txtOrderType = itemView.findViewById(R.id.txtOrderType);
            txtItems = itemView.findViewById(R.id.txtItems);
            txtTotal = itemView.findViewById(R.id.txtTotal);
            txtDiscount = itemView.findViewById(R.id.txtDiscount);
            layoutDiscount = itemView.findViewById(R.id.layoutDiscount);
        }
    }
}
