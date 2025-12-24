package com.example.restaurantapp.fragments.staff;

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
import com.google.android.material.chip.Chip;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StaffOrdersFragment extends Fragment implements StaffOrderAdapter.OnOrderActionListener {

    private RecyclerView recyclerView;
    private LinearLayout emptyState;
    private TextView tvEmptyMessage;
    private ProgressBar progressBar;
    
    private Chip chipAll, chipPending, chipPreparing, chipServed;
    
    private StaffOrderAdapter adapter;
    private List<OrderModel> allOrders = new ArrayList<>();
    private List<OrderModel> filteredOrders = new ArrayList<>();
    
    private ListenerRegistration ordersListener;
    private String currentFilter = "all";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_staff_orders, container, false);

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
        
        chipAll = view.findViewById(R.id.chipAll);
        chipPending = view.findViewById(R.id.chipPending);
        chipPreparing = view.findViewById(R.id.chipPreparing);
        chipServed = view.findViewById(R.id.chipServed);
        
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    }
    
    private void setupChips() {
        chipAll.setOnClickListener(v -> {
            currentFilter = "all";
            filterOrders();
        });
        
        chipPending.setOnClickListener(v -> {
            currentFilter = "pending";
            filterOrders();
        });
        
        chipPreparing.setOnClickListener(v -> {
            currentFilter = "preparing";
            filterOrders();
        });
        
        chipServed.setOnClickListener(v -> {
            currentFilter = "served";
            filterOrders();
        });
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
        
        ordersListener = FirebaseService.getInstance().listenOrdersRealtime((value, error) -> {
            if (progressBar != null) progressBar.setVisibility(View.GONE);
            
            if (error != null || value == null || !isAdded()) return;

            allOrders.clear();
            for (DocumentSnapshot doc : value.getDocuments()) {
                OrderModel order = doc.toObject(OrderModel.class);
                if (order != null) {
                    order.setId(doc.getId());
                    allOrders.add(order);
                }
            }
            
            // Sort by createdAt descending (newest first) - do at client since no index
            allOrders.sort((o1, o2) -> {
                if (o1.getCreatedAt() == null || o2.getCreatedAt() == null) return 0;
                return o2.getCreatedAt().compareTo(o1.getCreatedAt());
            });
            
            filterOrders();
        });
    }
    
    private void filterOrders() {
        filteredOrders.clear();
        
        for (OrderModel order : allOrders) {
            String status = order.getStatus();
            
            if ("all".equals(currentFilter)) {
                // Show all except paid and cancelled
                if (!"paid".equals(status) && !"cancelled".equals(status)) {
                    filteredOrders.add(order);
                }
            } else if ("pending".equals(currentFilter)) {
                if ("pending".equals(status) || "confirmed".equals(status)) {
                    filteredOrders.add(order);
                }
            } else if ("preparing".equals(currentFilter)) {
                if ("preparing".equals(status)) {
                    filteredOrders.add(order);
                }
            } else if ("served".equals(currentFilter)) {
                if ("served".equals(status)) {
                    filteredOrders.add(order);
                }
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
        android.util.Log.d("StaffOrders", "=== onPreparing ĐƯỢC GỌI ===");
        android.util.Log.d("StaffOrders", "Order ID: " + order.getId());
        android.util.Log.d("StaffOrders", "Order status: " + order.getStatus());
        Toast.makeText(getContext(), "Bắt đầu trừ kho...", Toast.LENGTH_SHORT).show();
        
        // Khi bắt đầu chuẩn bị, trừ nguyên liệu từ kho
        deductIngredientsForOrder(order, () -> {
            updateOrderStatus(order.getId(), "preparing", "Đơn hàng đang được chuẩn bị");
        });
    }

    @Override
    public void onServed(OrderModel order) {
        updateOrderStatus(order.getId(), "served", "Đã phục vụ đơn hàng");
    }

    @Override
    public void onPaid(OrderModel order) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Xác nhận thanh toán")
                .setMessage("Khách đã thanh toán " + 
                        String.format(Locale.getDefault(), "%,.0fđ", order.getTotal() != null ? order.getTotal() : 0) + "?")
                .setPositiveButton("Đã thanh toán", (dialog, which) -> {
                    updateOrderStatus(order.getId(), "paid", "Đã thanh toán");
                    
                    // Update loyalty points for customer
                    if (order.getTotal() != null && order.getCustomerId() != null) {
                        updateLoyaltyPoints(order.getCustomerId(), order.getTotal());
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    public void onCancel(OrderModel order) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Hủy đơn hàng")
                .setMessage("Bạn có chắc muốn hủy đơn hàng này?")
                .setPositiveButton("Hủy đơn", (dialog, which) -> {
                    updateOrderStatus(order.getId(), "cancelled", "Đã hủy đơn hàng");
                })
                .setNegativeButton("Không", null)
                .show();
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
    
    private void updateLoyaltyPoints(String customerId, Double total) {
        // Tính điểm: 10,000đ = 1 điểm
        int pointsToAdd = (int) (total / 10000);
        if (pointsToAdd <= 0) return;
        
        FirebaseService.getInstance().getDb()
                .collection("users")
                .document(customerId)
                .get()
                .addOnSuccessListener(doc -> {
                    Long currentPoints = doc.getLong("loyaltyPoints");
                    long newPoints = (currentPoints != null ? currentPoints : 0) + pointsToAdd;
                    
                    doc.getReference().update("loyaltyPoints", newPoints);
                });
    }

    /**
     * Trừ nguyên liệu từ kho khi bắt đầu chuẩn bị đơn hàng
     * Dựa trên công thức của từng món trong đơn
     */
    private void deductIngredientsForOrder(OrderModel order, Runnable onComplete) {
        // Lấy items - có thể là List<OrderItem> hoặc List<Map>
        Object itemsObj = order.getItems();
        if (itemsObj == null) {
            android.util.Log.d("StaffOrders", "Đơn hàng không có items");
            onComplete.run();
            return;
        }

        List<?> items = (List<?>) itemsObj;
        if (items.isEmpty()) {
            android.util.Log.d("StaffOrders", "Đơn hàng items rỗng");
            onComplete.run();
            return;
        }

        android.util.Log.d("StaffOrders", "Bắt đầu trừ kho cho " + items.size() + " món, kiểu: " + items.get(0).getClass().getSimpleName());

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

        android.util.Log.d("StaffOrders", "Số món cần trừ: " + pendingDeductions[0]);

        if (pendingDeductions[0] == 0) {
            android.util.Log.d("StaffOrders", "Không có món nào có menuItemId để trừ");
            onComplete.run();
            return;
        }

        // Trừ nguyên liệu cho từng món
        for (Object itemObj : items) {
            String menuItemId = extractMenuItemId(itemObj);
            int quantity = extractQuantity(itemObj);
            String itemName = extractItemName(itemObj);

            if (menuItemId != null && !menuItemId.isEmpty()) {
                android.util.Log.d("StaffOrders", "Đang trừ kho cho: " + itemName + " x" + quantity + " (menuItemId: " + menuItemId + ")");
                FirebaseService.getInstance().deductIngredientsFromRecipe(
                        menuItemId,
                        quantity,
                        unused -> {
                            android.util.Log.d("StaffOrders", "Đã trừ kho cho: " + itemName);
                            completedDeductions[0]++;
                            // Khi tất cả đã xong thì gọi callback
                            if (completedDeductions[0] >= pendingDeductions[0]) {
                                if (!hasError[0] && isAdded()) {
                                    // Cập nhật trạng thái món ăn sau khi trừ kho
                                    checkAndUpdateMenuAvailability();
                                }
                                onComplete.run();
                            }
                        },
                        e -> {
                            hasError[0] = true;
                            completedDeductions[0]++;
                            if (completedDeductions[0] >= pendingDeductions[0]) {
                                onComplete.run();
                            }
                            // Log error nhưng không block flow
                            android.util.Log.e("StaffOrders", "Lỗi trừ kho: " + e.getMessage());
                        }
                );
            }
        }
    }

    /**
     * Kiểm tra và cập nhật trạng thái món ăn sau khi trừ kho
     */
    private void checkAndUpdateMenuAvailability() {
        // Gọi service để kiểm tra lại inventory và cập nhật trạng thái món ăn
        // Tính năng này đã có trong AdminRecipeFragment - syncMenuItemsStatus
        // Ở đây ta chỉ log hoặc có thể gửi notification cho admin
        android.util.Log.d("StaffOrders", "Đã trừ nguyên liệu - cần kiểm tra lại trạng thái món");
    }

    /**
     * Lấy menuItemId từ item (có thể là OrderItem hoặc Map)
     */
    private String extractMenuItemId(Object itemObj) {
        if (itemObj instanceof OrderItem) {
            return ((OrderItem) itemObj).getMenuItemId();
        } else if (itemObj instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) itemObj;
            // Thử lấy menuItemId, nếu không có thì lấy menuId
            String menuItemId = (String) map.get("menuItemId");
            if (menuItemId == null || menuItemId.isEmpty()) {
                menuItemId = (String) map.get("menuId");
            }
            return menuItemId;
        }
        return null;
    }

    /**
     * Lấy quantity từ item (có thể là OrderItem hoặc Map)
     */
    private int extractQuantity(Object itemObj) {
        if (itemObj instanceof OrderItem) {
            return ((OrderItem) itemObj).getQuantity();
        } else if (itemObj instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) itemObj;
            Object qtyObj = map.get("quantity");
            if (qtyObj instanceof Number) {
                return ((Number) qtyObj).intValue();
            }
        }
        return 1;
    }

    /**
     * Lấy name từ item (có thể là OrderItem hoặc Map)
     */
    private String extractItemName(Object itemObj) {
        if (itemObj instanceof OrderItem) {
            return ((OrderItem) itemObj).getName();
        } else if (itemObj instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) itemObj;
            return (String) map.get("name");
        }
        return "Unknown";
    }
    
    private void showOrderDetailsDialog(OrderModel order) {
        StringBuilder details = new StringBuilder();
        
        // Order info
        details.append("📋 Đơn #").append(order.getId().substring(0, 8).toUpperCase()).append("\n\n");
        
        // Order type
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
        
        // Items
        if (order.getItems() != null) {
            for (Object item : order.getItems()) {
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
        }
        
        // Totals
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
        
        // Time
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (ordersListener != null) {
            ordersListener.remove();
            ordersListener = null;
        }
    }
}
