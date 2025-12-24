package com.example.restaurantapp.adapters;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
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

public class CustomerOrderAdapter extends RecyclerView.Adapter<CustomerOrderAdapter.CustomerOrderViewHolder> {

    private List<OrderModel> orderList;
    private OnPaymentRequestListener paymentListener;

    public interface OnPaymentRequestListener {
        void onPaymentRequest(OrderModel order);
    }

    public CustomerOrderAdapter(List<OrderModel> orderList) {
        this.orderList = orderList;
    }

    public CustomerOrderAdapter(List<OrderModel> orderList, OnPaymentRequestListener listener) {
        this.orderList = orderList;
        this.paymentListener = listener;
    }

    public void setPaymentListener(OnPaymentRequestListener listener) {
        this.paymentListener = listener;
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
        TextView txtOrderId, txtOrderItems, txtOrderTime, txtTotal, txtPaymentMethod;
        Chip chipStatus, chipPaymentStatus;
        MaterialButton btnPayment;
        LinearLayout layoutPaymentStatus;

        CustomerOrderViewHolder(@NonNull View itemView) {
            super(itemView);
            txtOrderId = itemView.findViewById(R.id.txtOrderId);
            txtOrderItems = itemView.findViewById(R.id.txtOrderItems);
            txtOrderTime = itemView.findViewById(R.id.txtOrderTime);
            txtTotal = itemView.findViewById(R.id.txtTotal);
            chipStatus = itemView.findViewById(R.id.chipStatus);
            btnPayment = itemView.findViewById(R.id.btnPayment);
            layoutPaymentStatus = itemView.findViewById(R.id.layoutPaymentStatus);
            txtPaymentMethod = itemView.findViewById(R.id.txtPaymentMethod);
            chipPaymentStatus = itemView.findViewById(R.id.chipPaymentStatus);
        }

        void bind(OrderModel order) {

            // ===== Order ID =====
            if (order.getId() != null) {
                txtOrderId.setText(
                        "Đơn #" + order.getId().substring(0, Math.min(8, order.getId().length()))
                );
            } else {
                txtOrderId.setText("Đơn #---");
            }

            // ===== Hiển thị danh sách món (KHÔNG tính lại total) =====
            StringBuilder itemsText = new StringBuilder();
            List<?> itemsList = order.getItemsForDisplay();
            if (itemsList != null) {
                for (Object item : itemsList) {
                    if (item instanceof Map) {
                        Map<String, Object> itemMap = (Map<String, Object>) item;

                        String name = (String) itemMap.get("name");
                        Object qtyObj = itemMap.get("quantity");
                        int qty = qtyObj instanceof Number ? ((Number) qtyObj).intValue() : 1;

                        if (name != null) {
                            itemsText.append("• ")
                                    .append(name)
                                    .append(" x")
                                    .append(qty)
                                    .append("\n");
                        }
                    }
                }
            }

            txtOrderItems.setText(
                    itemsText.length() > 0 ? itemsText.toString().trim() : "Không có món"
            );

            // ===== Tổng tiền (LẤY TRỰC TIẾP TỪ FIRESTORE) =====
            Double total = order.getTotal();
            if (total == null) total = 0.0;
            txtTotal.setText(String.format("Tổng: %,.0f đ", total));

            // ===== Thời gian =====
            if (order.getCreatedAt() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault());
                txtOrderTime.setText(sdf.format(order.getCreatedAt().toDate()));
            } else {
                txtOrderTime.setText("");
            }

            // ===== Reset chip (QUAN TRỌNG để tránh lỗi RecyclerView) =====
            chipStatus.setChipBackgroundColor(null);

            // ===== Status =====
            String status = order.getStatus();
            String statusText;
            int statusColor;

            if (status == null) status = "";

            switch (status) {
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
                    statusText = status.isEmpty() ? "Không rõ" : status;
                    statusColor = 0xFF9E9E9E;
                    break;
            }

            chipStatus.setText(statusText);
            chipStatus.setChipBackgroundColor(
                    android.content.res.ColorStateList.valueOf(statusColor)
            );

            // ===== Payment Button & Status =====
            String paymentStatus = order.getPaymentStatus();
            String paymentMethod = order.getPaymentMethod();

            // Reset views
            btnPayment.setVisibility(View.GONE);
            layoutPaymentStatus.setVisibility(View.GONE);

            if ("served".equals(status)) {
                // Order đã phục vụ - có thể thanh toán
                if (paymentStatus == null || paymentStatus.isEmpty()) {
                    // Chưa yêu cầu thanh toán - hiển thị nút
                    btnPayment.setVisibility(View.VISIBLE);
                    btnPayment.setText("💳 Thanh toán");
                    btnPayment.setOnClickListener(v -> {
                        if (paymentListener != null) {
                            paymentListener.onPaymentRequest(order);
                        }
                    });
                } else if ("pending".equals(paymentStatus)) {
                    // Đã gọi thanh toán - đang chờ nhân viên
                    layoutPaymentStatus.setVisibility(View.VISIBLE);
                    String methodText = getPaymentMethodText(paymentMethod);
                    txtPaymentMethod.setText("PT: " + methodText);
                    chipPaymentStatus.setText("⏳ Đang chờ thanh toán");
                    chipPaymentStatus.setChipBackgroundColor(ColorStateList.valueOf(0xFFFF9800));
                }
            } else if ("paid".equals(status)) {
                // Đã thanh toán - hiển thị thông tin
                layoutPaymentStatus.setVisibility(View.VISIBLE);
                String methodText = getPaymentMethodText(paymentMethod);
                txtPaymentMethod.setText("PT: " + methodText);
                chipPaymentStatus.setText("✅ Đã thanh toán");
                chipPaymentStatus.setChipBackgroundColor(ColorStateList.valueOf(0xFF4CAF50));
            }
        }

        private String getPaymentMethodText(String method) {
            if (method == null) return "Tiền mặt";
            switch (method) {
                case "cash": return "Tiền mặt";
                case "bank_transfer": return "Chuyển khoản";
                case "e_wallet": return "Ví điện tử";
                case "card": return "Thẻ";
                default: return method;
            }
        }
    }
}
