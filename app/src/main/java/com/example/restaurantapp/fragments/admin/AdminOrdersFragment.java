package com.example.restaurantapp.fragments.admin;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.restaurantapp.R;
import com.example.restaurantapp.adapters.StaffOrderAdapter;
import com.example.restaurantapp.api.FirebaseService;
import com.example.restaurantapp.models.OrderItem;
import com.example.restaurantapp.models.OrderModel;
import com.example.restaurantapp.models.User;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminOrdersFragment extends Fragment implements StaffOrderAdapter.OnOrderActionListener {

    private RecyclerView recyclerView;
    private LinearLayout emptyState;
    private TextView tvEmptyMessage, tvTotalRevenue, tvOrderCount;
    private ProgressBar progressBar;
    
    private Chip chipAll, chipPending, chipPreparing, chipServed, chipPaid, chipCancelled;
    
    private StaffOrderAdapter adapter;
    private List<OrderModel> allOrders = new ArrayList<>();
    private List<OrderModel> filteredOrders = new ArrayList<>();
    
    private ListenerRegistration ordersListener;
    private String currentFilter = "all";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_orders, container, false);

        initViews(view);
        setupChips();
        setupAdapter();
        loadOrders();

        return view;
    }
    
    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerOrders);
        emptyState = view.findViewById(R.id.emptyState);
        tvEmptyMessage = view.findViewById(R.id.tvEmptyMessage);
        progressBar = view.findViewById(R.id.progressBar);
        tvTotalRevenue = view.findViewById(R.id.tvTotalRevenue);
        tvOrderCount = view.findViewById(R.id.tvOrderCount);
        
        chipAll = view.findViewById(R.id.chipAll);
        chipPending = view.findViewById(R.id.chipPending);
        chipPreparing = view.findViewById(R.id.chipPreparing);
        chipServed = view.findViewById(R.id.chipServed);
        chipPaid = view.findViewById(R.id.chipPaid);
        chipCancelled = view.findViewById(R.id.chipCancelled);
        
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    }
    
    private void setupChips() {
        View.OnClickListener chipListener = v -> {
            if (v == chipAll) currentFilter = "all";
            else if (v == chipPending) currentFilter = "pending";
            else if (v == chipPreparing) currentFilter = "preparing";
            else if (v == chipServed) currentFilter = "served";
            else if (v == chipPaid) currentFilter = "paid";
            else if (v == chipCancelled) currentFilter = "cancelled";
            
            filterOrders();
        };
        
        chipAll.setOnClickListener(chipListener);
        chipPending.setOnClickListener(chipListener);
        chipPreparing.setOnClickListener(chipListener);
        chipServed.setOnClickListener(chipListener);
        chipPaid.setOnClickListener(chipListener);
        chipCancelled.setOnClickListener(chipListener);
    }
    
    private void setupAdapter() {
        adapter = new StaffOrderAdapter(filteredOrders, this);
        recyclerView.setAdapter(adapter);
    }

    private void loadOrders() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        if (emptyState != null) emptyState.setVisibility(View.GONE);
        
        if (ordersListener != null) {
            ordersListener.remove();
        }
        
        // Admin sees all orders
        ordersListener = FirebaseService.getInstance().getDb()
                .collection("orders")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    
                    if (error != null || value == null || !isAdded()) return;

                    allOrders.clear();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        OrderModel order = doc.toObject(OrderModel.class);
                        if (order != null) {
                            order.setId(doc.getId());
                            
                            // Fix: Lấy items trực tiếp từ document vì toObject không map đúng List<Map>
                            Object itemsObj = doc.get("items");
                            if (itemsObj instanceof List) {
                                order.setRawItems((List<?>) itemsObj);
                            }
                            
                            allOrders.add(order);
                        }
                    }
                    
                    updateStatistics();
                    filterOrders();
                });
    }
    
    private void updateStatistics() {
        // Calculate today's revenue
        double todayRevenue = 0;
        int todayOrders = 0;
        
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        Date todayStart = calendar.getTime();
        
        for (OrderModel order : allOrders) {
            if (order.getCreatedAt() != null && order.getCreatedAt().toDate().after(todayStart)) {
                if ("paid".equals(order.getStatus()) && order.getTotal() != null) {
                    todayRevenue += order.getTotal();
                }
                todayOrders++;
            }
        }
        
        if (tvTotalRevenue != null) {
            tvTotalRevenue.setText(String.format(Locale.getDefault(), "%,.0fđ", todayRevenue));
        }
        if (tvOrderCount != null) {
            tvOrderCount.setText(String.valueOf(todayOrders));
        }
    }
    
    private void filterOrders() {
        filteredOrders.clear();
        
        for (OrderModel order : allOrders) {
            String status = order.getStatus();
            
            if ("all".equals(currentFilter)) {
                filteredOrders.add(order);
            } else if ("pending".equals(currentFilter)) {
                if ("pending".equals(status) || "confirmed".equals(status)) {
                    filteredOrders.add(order);
                }
            } else if (currentFilter.equals(status)) {
                filteredOrders.add(order);
            }
        }
        
        // Update UI
        if (filteredOrders.isEmpty()) {
            if (emptyState != null) emptyState.setVisibility(View.VISIBLE);
            if (tvEmptyMessage != null) tvEmptyMessage.setText("Không có đơn hàng");
            recyclerView.setVisibility(View.GONE);
        } else {
            if (emptyState != null) emptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
        
        adapter.updateData(filteredOrders);
    }

    @Override
    public void onConfirm(OrderModel order) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Xác nhận đơn hàng")
                .setMessage("Xác nhận đơn #" + order.getId().substring(0, 8).toUpperCase() + "?")
                .setPositiveButton("Xác nhận", (dialog, which) -> {
                    updateOrderStatus(order.getId(), "confirmed", "Đã xác nhận đơn hàng");
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    public void onPreparing(OrderModel order) {
        android.util.Log.d("AdminOrders", "=== onPreparing ĐƯỢC GỌI ===");
        android.util.Log.d("AdminOrders", "Order ID: " + order.getId());
        
        List<?> items = order.getItemsForDisplay();
        
        // Kiểm tra nguyên liệu trước khi chuẩn bị
        FirebaseService.getInstance().checkIngredientsAvailability(items,
                missingList -> {
                    if (missingList != null && !missingList.isEmpty()) {
                        // Thiếu nguyên liệu - hiển thị cảnh báo
                        StringBuilder message = new StringBuilder();
                        message.append("Không đủ nguyên liệu để chuẩn bị đơn hàng:\n\n");
                        for (String missing : missingList) {
                            message.append("• ").append(missing).append("\n");
                        }
                        message.append("\nVui lòng nhập thêm nguyên liệu vào kho!");

                        new AlertDialog.Builder(requireContext())
                                .setTitle("⚠️ Thiếu nguyên liệu")
                                .setMessage(message.toString())
                                .setPositiveButton("Đóng", null)
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .show();
                    } else {
                        // Đủ nguyên liệu - tiến hành trừ kho
                        Toast.makeText(getContext(), "Bắt đầu trừ kho...", Toast.LENGTH_SHORT).show();
                        deductIngredientsForOrder(order, () -> {
                            updateOrderStatus(order.getId(), "preparing", "Đơn hàng đang được chuẩn bị");
                        });
                    }
                },
                e -> {
                    android.util.Log.e("AdminOrders", "Lỗi kiểm tra nguyên liệu: " + e.getMessage());
                    // Nếu lỗi kiểm tra, vẫn cho phép chuẩn bị (để không block)
                    Toast.makeText(getContext(), "Bắt đầu trừ kho...", Toast.LENGTH_SHORT).show();
                    deductIngredientsForOrder(order, () -> {
                        updateOrderStatus(order.getId(), "preparing", "Đơn hàng đang được chuẩn bị");
                    });
                });
    }

    @Override
    public void onServed(OrderModel order) {
        updateOrderStatus(order.getId(), "served", "Đã phục vụ đơn hàng");
    }

    // Trong AdminOrdersFragment.java
    @Override
    public void onPaid(OrderModel order) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Xác nhận thanh toán")
                .setMessage("Thu tiền: " + String.format(Locale.getDefault(), "%,.0fđ", order.getTotal()))
                .setPositiveButton("Xác nhận", (dialog, which) -> {

                    FirebaseService.getInstance().confirmPayment(order.getId(), aVoid -> {
                        // Gọi service cộng điểm
                        FirebaseService.getInstance().updateLoyaltyPoints(
                                order.getCustomerId(),
                                order.getTotal(),
                                unused -> Toast.makeText(getContext(), "Đã cộng điểm tích lũy!", Toast.LENGTH_SHORT).show()
                        );
                    }, e -> Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());

                }).show();
    }

    @Override
    public void onCancel(OrderModel order) {
        // Tạo input để nhập lý do hủy
        android.widget.EditText inputReason = new android.widget.EditText(requireContext());
        inputReason.setHint("Nhập lý do hủy (không bắt buộc)");
        inputReason.setPadding(48, 32, 48, 16);
        
        String status = order.getStatus();
        String warningMessage = "";
        
        if ("preparing".equals(status)) {
            warningMessage = "⚠️ Đơn hàng đang được chuẩn bị!\nNguyên liệu đã trừ sẽ không được hoàn lại.\n\n";
        }
        
        new AlertDialog.Builder(requireContext())
                .setTitle("🚫 Hủy đơn hàng")
                .setMessage(warningMessage + "Bạn có chắc muốn hủy đơn #" + 
                        order.getId().substring(0, Math.min(8, order.getId().length())).toUpperCase() + "?")
                .setView(inputReason)
                .setPositiveButton("Hủy đơn", (dialog, which) -> {
                    String reason = inputReason.getText().toString().trim();
                    cancelOrder(order.getId(), reason);
                })
                .setNegativeButton("Không", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }
    
    private void cancelOrder(String orderId, String reason) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "cancelled");
        updates.put("updatedAt", System.currentTimeMillis());
        updates.put("cancelledAt", System.currentTimeMillis());
        if (reason != null && !reason.isEmpty()) {
            updates.put("cancelReason", reason);
        }
        
        FirebaseService.getInstance().getDb()
                .collection("orders")
                .document(orderId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Đã hủy đơn hàng", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onViewDetails(OrderModel order) {
        showOrderDetailsDialog(order);
    }
    
    private void updateOrderStatus(String orderId, String status, String successMessage) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);
        updates.put("updatedAt", System.currentTimeMillis());
        
        FirebaseService.getInstance().getDb()
                .collection("orders")
                .document(orderId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), successMessage, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    

    
    private void showOrderDetailsDialog(OrderModel order) {
        StringBuilder details = new StringBuilder();
        
        details.append("📋 Đơn #").append(order.getId().substring(0, 8).toUpperCase()).append("\n\n");
        
        if ("dine_in".equals(order.getOrderType())) {
            details.append("🪑 ").append(order.getTableName() != null ? order.getTableName() : "Ăn tại chỗ").append("\n");
        } else if ("takeaway".equals(order.getOrderType())) {
            details.append("🏠 Mang về\n");
            if (order.getDeliveryAddress() != null) {
                details.append("📍 ").append(order.getDeliveryAddress()).append("\n");
            }
            if (order.getDeliveryPhone() != null) {
                details.append("📞 ").append(order.getDeliveryPhone()).append("\n");
            }
        }
        
        details.append("\n--- Món đặt ---\n");
        
        List<?> itemsList = order.getItemsForDisplay();
        if (itemsList != null && !itemsList.isEmpty()) {
            for (Object item : itemsList) {
                if (item instanceof Map) {
                    Map<String, Object> itemMap = (Map<String, Object>) item;
                    String name = (String) itemMap.get("name");
                    Object qtyObj = itemMap.get("quantity");
                    Object priceObj = itemMap.get("price");
                    int qty = qtyObj instanceof Number ? ((Number) qtyObj).intValue() : 1;
                    double price = priceObj instanceof Number ? ((Number) priceObj).doubleValue() : 0;
                    
                    if (name != null) {
                        details.append("• ").append(name).append(" x").append(qty);
                        if (price > 0) {
                            details.append(" - ").append(String.format(Locale.getDefault(), "%,.0fđ", price * qty));
                        }
                        details.append("\n");
                    }
                }
            }
        } else {
            details.append("(Không có thông tin món)\n");
        }
        
        details.append("\n");
        if (order.getSubtotal() != null) {
            details.append("Tạm tính: ").append(String.format(Locale.getDefault(), "%,.0fđ", order.getSubtotal())).append("\n");
        }
        if (order.getDiscount() != null && order.getDiscount() > 0) {
            details.append("Giảm giá: -").append(String.format(Locale.getDefault(), "%,.0fđ", order.getDiscount())).append("\n");
        }
        if (order.getTotal() != null) {
            details.append("💰 Tổng cộng: ").append(String.format(Locale.getDefault(), "%,.0fđ", order.getTotal())).append("\n");
        }
        
        if (order.getCreatedAt() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault());
            details.append("\n🕐 ").append(sdf.format(order.getCreatedAt().toDate()));
        }
        
        new AlertDialog.Builder(requireContext())
                .setTitle("Chi tiết đơn hàng")
                .setMessage(details.toString())
                .setPositiveButton("Đóng", null)
                .show();
    }

    /**
     * Trừ nguyên liệu từ kho khi bắt đầu chuẩn bị đơn hàng
     */
    private void deductIngredientsForOrder(OrderModel order, Runnable onComplete) {
        List<?> items = order.getItemsForDisplay();
        if (items == null || items.isEmpty()) {
            android.util.Log.d("AdminOrders", "Đơn hàng không có items");
            onComplete.run();
            return;
        }

        android.util.Log.d("AdminOrders", "Bắt đầu trừ kho cho " + items.size() + " món");

        final int[] pendingDeductions = {0};
        final int[] completedDeductions = {0};
        final boolean[] hasError = {false};

        // Đếm số món cần trừ
        for (Object itemObj : items) {
            String menuItemId = extractMenuItemId(itemObj);
            if (menuItemId != null && !menuItemId.isEmpty()) {
                pendingDeductions[0]++;
            }
        }

        android.util.Log.d("AdminOrders", "Số món cần trừ: " + pendingDeductions[0]);

        if (pendingDeductions[0] == 0) {
            android.util.Log.d("AdminOrders", "Không có món nào có menuItemId để trừ");
            onComplete.run();
            return;
        }

        // Trừ nguyên liệu cho từng món
        for (Object itemObj : items) {
            String menuItemId = extractMenuItemId(itemObj);
            int quantity = extractQuantity(itemObj);
            String itemName = extractItemName(itemObj);

            if (menuItemId != null && !menuItemId.isEmpty()) {
                android.util.Log.d("AdminOrders", "Đang trừ kho cho: " + itemName + " x" + quantity + " (menuItemId: " + menuItemId + ")");
                FirebaseService.getInstance().deductIngredientsFromRecipe(
                        menuItemId,
                        quantity,
                        unused -> {
                            android.util.Log.d("AdminOrders", "Đã trừ kho cho: " + itemName);
                            completedDeductions[0]++;
                            if (completedDeductions[0] >= pendingDeductions[0]) {
                                onComplete.run();
                            }
                        },
                        e -> {
                            hasError[0] = true;
                            completedDeductions[0]++;
                            android.util.Log.e("AdminOrders", "Lỗi trừ kho: " + e.getMessage());
                            if (completedDeductions[0] >= pendingDeductions[0]) {
                                onComplete.run();
                            }
                        }
                );
            }
        }
    }

    private String extractMenuItemId(Object itemObj) {
        if (itemObj instanceof OrderItem) {
            return ((OrderItem) itemObj).getMenuItemId();
        } else if (itemObj instanceof Map) {
            Object id = ((Map<?, ?>) itemObj).get("menuItemId");
            return id != null ? id.toString() : null;
        }
        return null;
    }

    private int extractQuantity(Object itemObj) {
        if (itemObj instanceof OrderItem) {
            return ((OrderItem) itemObj).getQuantity();
        } else if (itemObj instanceof Map) {
            Object qty = ((Map<?, ?>) itemObj).get("quantity");
            if (qty instanceof Number) {
                return ((Number) qty).intValue();
            }
        }
        return 1;
    }

    private String extractItemName(Object itemObj) {
        if (itemObj instanceof OrderItem) {
            return ((OrderItem) itemObj).getName();
        } else if (itemObj instanceof Map) {
            Object name = ((Map<?, ?>) itemObj).get("name");
            return name != null ? name.toString() : "Unknown";
        }
        return "Unknown";
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (ordersListener != null) {
            ordersListener.remove();
            ordersListener = null;
        }
    }
}
