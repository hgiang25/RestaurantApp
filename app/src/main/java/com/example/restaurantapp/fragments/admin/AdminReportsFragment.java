package com.example.restaurantapp.fragments.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.restaurantapp.R;
import com.example.restaurantapp.api.FirebaseService;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminReportsFragment extends Fragment {

    private TextView tvTodayOrders, tvTodayRevenue, tvMonthRevenue, tvTotalOrders;
    private TextView tvPendingOrders, tvCompletedOrders, tvCancelledOrders;
    
    // Top selling menu items
    private LinearLayout layoutTopSelling;
    private TextView tvTopMenu1, tvTopMenu2, tvTopMenu3;
    private ProgressBar progressTop1, progressTop2, progressTop3;
    
    // Inventory alerts
    private LinearLayout layoutLowStock;
    private TextView tvLowStockCount, tvLowStockItems;
    
    // Customer stats
    private TextView tvTotalCustomers, tvNewCustomersToday;
    
    // Promotion stats
    private TextView tvActivePromotions, tvVouchersUsed;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_reports, container, false);

        // Order stats
        tvTodayOrders = view.findViewById(R.id.tvTodayOrders);
        tvTodayRevenue = view.findViewById(R.id.tvTodayRevenue);
        tvMonthRevenue = view.findViewById(R.id.tvMonthRevenue);
        tvTotalOrders = view.findViewById(R.id.tvTotalOrders);
        tvPendingOrders = view.findViewById(R.id.tvPendingOrders);
        tvCompletedOrders = view.findViewById(R.id.tvCompletedOrders);
        tvCancelledOrders = view.findViewById(R.id.tvCancelledOrders);
        
        // Top selling
        layoutTopSelling = view.findViewById(R.id.layoutTopSelling);
        tvTopMenu1 = view.findViewById(R.id.tvTopMenu1);
        tvTopMenu2 = view.findViewById(R.id.tvTopMenu2);
        tvTopMenu3 = view.findViewById(R.id.tvTopMenu3);
        progressTop1 = view.findViewById(R.id.progressTop1);
        progressTop2 = view.findViewById(R.id.progressTop2);
        progressTop3 = view.findViewById(R.id.progressTop3);
        
        // Low stock
        layoutLowStock = view.findViewById(R.id.layoutLowStock);
        tvLowStockCount = view.findViewById(R.id.tvLowStockCount);
        tvLowStockItems = view.findViewById(R.id.tvLowStockItems);
        
        // Customer stats
        tvTotalCustomers = view.findViewById(R.id.tvTotalCustomers);
        tvNewCustomersToday = view.findViewById(R.id.tvNewCustomersToday);
        
        // Promotion stats
        tvActivePromotions = view.findViewById(R.id.tvActivePromotions);
        tvVouchersUsed = view.findViewById(R.id.tvVouchersUsed);

        loadReports();
        loadTopSellingMenu();
        loadInventoryAlerts();
        loadCustomerStats();
        loadPromotionStats();

        return view;
    }

    private void loadReports() {
        FirebaseService.getInstance().listenOrdersRealtime((value, error) -> {
            if (!isAdded() || getContext() == null) return;
            if (error != null || value == null) return;

            int todayOrders = 0;
            double todayRevenue = 0;
            double monthRevenue = 0;
            int totalOrders = value.size();
            int pendingOrders = 0;
            int completedOrders = 0;
            int cancelledOrders = 0;

            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);

            Calendar monthStart = Calendar.getInstance();
            monthStart.set(Calendar.DAY_OF_MONTH, 1);
            monthStart.set(Calendar.HOUR_OF_DAY, 0);
            monthStart.set(Calendar.MINUTE, 0);
            monthStart.set(Calendar.SECOND, 0);

            for (DocumentSnapshot doc : value.getDocuments()) {
                String status = doc.getString("status");
                Double total = doc.getDouble("total");
                Date createdAt = doc.getTimestamp("createdAt") != null ?
                        doc.getTimestamp("createdAt").toDate() : null;

                if (status != null) {
                    switch (status) {
                        case "pending":
                            pendingOrders++;
                            break;
                        case "paid":
                        case "served":
                            completedOrders++;
                            if (total != null && createdAt != null) {
                                if (createdAt.after(today.getTime())) {
                                    todayOrders++;
                                    todayRevenue += total;
                                }
                                if (createdAt.after(monthStart.getTime())) {
                                    monthRevenue += total;
                                }
                            }
                            break;
                        case "cancelled":
                            cancelledOrders++;
                            break;
                    }
                }
            }

            if (tvTodayOrders != null) tvTodayOrders.setText(String.valueOf(todayOrders));
            if (tvTodayRevenue != null) tvTodayRevenue.setText(String.format(Locale.getDefault(), "%,.0fđ", todayRevenue));
            if (tvMonthRevenue != null) tvMonthRevenue.setText(String.format(Locale.getDefault(), "%,.0fđ", monthRevenue));
            if (tvTotalOrders != null) tvTotalOrders.setText(String.valueOf(totalOrders));
            if (tvPendingOrders != null) tvPendingOrders.setText(String.valueOf(pendingOrders));
            if (tvCompletedOrders != null) tvCompletedOrders.setText(String.valueOf(completedOrders));
            if (tvCancelledOrders != null) tvCancelledOrders.setText(String.valueOf(cancelledOrders));
        });
    }

    private void loadTopSellingMenu() {
        FirebaseService.getInstance().listenTopSellingMenu(3, (value, error) -> {
            if (!isAdded() || getContext() == null) return;
            if (error != null || value == null) return;

            List<DocumentSnapshot> docs = value.getDocuments();
            TextView[] tvMenus = {tvTopMenu1, tvTopMenu2, tvTopMenu3};
            ProgressBar[] progressBars = {progressTop1, progressTop2, progressTop3};
            
            int maxSold = 0;
            for (DocumentSnapshot doc : docs) {
                Long soldCount = doc.getLong("soldCount");
                if (soldCount != null && soldCount > maxSold) {
                    maxSold = soldCount.intValue();
                }
            }

            for (int i = 0; i < 3; i++) {
                if (tvMenus[i] == null || progressBars[i] == null) continue;
                
                if (i < docs.size()) {
                    DocumentSnapshot doc = docs.get(i);
                    String name = doc.getString("name");
                    Long soldCount = doc.getLong("soldCount");
                    int sold = soldCount != null ? soldCount.intValue() : 0;
                    
                    tvMenus[i].setText(String.format("%d. %s (%d)", i + 1, name, sold));
                    tvMenus[i].setVisibility(View.VISIBLE);
                    progressBars[i].setVisibility(View.VISIBLE);
                    progressBars[i].setMax(maxSold > 0 ? maxSold : 1);
                    progressBars[i].setProgress(sold);
                } else {
                    tvMenus[i].setVisibility(View.GONE);
                    progressBars[i].setVisibility(View.GONE);
                }
            }
        });
    }

    private void loadInventoryAlerts() {
        FirebaseService.getInstance().listenInventoryRealtime((value, error) -> {
            if (!isAdded() || getContext() == null) return;
            if (error != null || value == null) return;

            int lowStockCount = 0;
            StringBuilder lowStockNames = new StringBuilder();

            for (DocumentSnapshot doc : value.getDocuments()) {
                Double quantity = doc.getDouble("quantity");
                Double minQuantity = doc.getDouble("minQuantity");
                String name = doc.getString("name");
                
                double min = minQuantity != null ? minQuantity : 10;
                if (quantity != null && quantity <= min) {
                    lowStockCount++;
                    if (lowStockNames.length() > 0) lowStockNames.append(", ");
                    lowStockNames.append(name);
                }
            }

            if (tvLowStockCount != null) {
                tvLowStockCount.setText(String.valueOf(lowStockCount));
                tvLowStockCount.setTextColor(lowStockCount > 0 ? 0xFFF44336 : 0xFF4CAF50);
            }
            if (tvLowStockItems != null) {
                tvLowStockItems.setText(lowStockCount > 0 ? lowStockNames.toString() : "Tất cả nguyên liệu đầy đủ");
            }
        });
    }

    private void loadCustomerStats() {
        FirebaseService.getInstance().listenUsersByRoleRealtime("customer", (value, error) -> {
            if (!isAdded() || getContext() == null) return;
            if (error != null || value == null) return;

            int totalCustomers = value.size();
            int newToday = 0;
            
            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);

            for (DocumentSnapshot doc : value.getDocuments()) {
                com.google.firebase.Timestamp createdAt = doc.getTimestamp("createdAt");
                if (createdAt != null && createdAt.toDate().after(today.getTime())) {
                    newToday++;
                }
            }

            if (tvTotalCustomers != null) tvTotalCustomers.setText(String.valueOf(totalCustomers));
            if (tvNewCustomersToday != null) tvNewCustomersToday.setText("+" + newToday + " hôm nay");
        });
    }

    private void loadPromotionStats() {
        FirebaseService.getInstance().listenPromotionsRealtime((value, error) -> {
            if (!isAdded() || getContext() == null) return;
            if (error != null || value == null) return;

            int activeCount = 0;
            for (DocumentSnapshot doc : value.getDocuments()) {
                Boolean active = doc.getBoolean("active");
                if (active != null && active) {
                    activeCount++;
                }
            }

            if (tvActivePromotions != null) tvActivePromotions.setText(String.valueOf(activeCount));
        });

        // Count vouchers used from orders
        FirebaseService.getInstance().listenOrdersRealtime((value, error) -> {
            if (!isAdded() || getContext() == null) return;
            if (error != null || value == null) return;

            int vouchersUsed = 0;
            for (DocumentSnapshot doc : value.getDocuments()) {
                if (doc.get("voucher") != null) {
                    vouchersUsed++;
                }
            }

            if (tvVouchersUsed != null) tvVouchersUsed.setText(String.valueOf(vouchersUsed));
        });
    }
}
