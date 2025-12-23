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
import com.example.restaurantapp.models.OrderModel;
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
        updateOrderStatus(order.getId(), "preparing", "Đơn hàng đang được chuẩn bị");
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (ordersListener != null) {
            ordersListener.remove();
            ordersListener = null;
        }
    }
}
