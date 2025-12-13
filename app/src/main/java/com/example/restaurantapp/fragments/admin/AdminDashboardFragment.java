package com.example.restaurantapp.fragments.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.restaurantapp.R;
import com.example.restaurantapp.api.FirebaseService;
import com.google.firebase.firestore.DocumentSnapshot;

public class AdminDashboardFragment extends Fragment {

    private TextView txtTotalOrders, txtTotalRevenue, txtTotalStaff, txtTotalTables;
    private TextView txtPendingOrders, txtTodayOrders;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_dashboard, container, false);

        txtTotalOrders = view.findViewById(R.id.txtTotalOrders);
        txtTotalRevenue = view.findViewById(R.id.txtTotalRevenue);
        txtTotalStaff = view.findViewById(R.id.txtTotalStaff);
        txtTotalTables = view.findViewById(R.id.txtTotalTables);
        txtPendingOrders = view.findViewById(R.id.txtPendingOrders);
        txtTodayOrders = view.findViewById(R.id.txtTodayOrders);

        loadDashboardData();

        return view;
    }

    private void loadDashboardData() {
        // Load tổng số đơn hàng
        FirebaseService.getInstance().listenOrdersRealtime((value, error) -> {
            if (error != null || value == null) return;
            
            int total = value.size();
            int pending = 0;
            double revenue = 0;
            
            for (DocumentSnapshot doc : value.getDocuments()) {
                String status = doc.getString("status");
                if ("pending".equals(status)) {
                    pending++;
                }
                if ("paid".equals(status) || "served".equals(status)) {
                    // Tính doanh thu từ các đơn đã thanh toán
                    java.util.List<java.util.Map<String, Object>> items = 
                        (java.util.List<java.util.Map<String, Object>>) doc.get("items");
                    if (items != null) {
                        for (java.util.Map<String, Object> item : items) {
                            Object priceObj = item.get("price");
                            Object qtyObj = item.get("quantity");
                            double price = priceObj instanceof Number ? ((Number) priceObj).doubleValue() : 0;
                            int qty = qtyObj instanceof Number ? ((Number) qtyObj).intValue() : 1;
                            revenue += price * qty;
                        }
                    }
                }
            }
            
            txtTotalOrders.setText(String.valueOf(total));
            txtPendingOrders.setText(pending + " chờ xử lý");
            txtTotalRevenue.setText(String.format("%,.0f đ", revenue));
        });

        // Load số nhân viên
        FirebaseService.getInstance().listenUsersByRoleRealtime("staff", (value, error) -> {
            if (error != null || value == null) return;
            txtTotalStaff.setText(String.valueOf(value.size()));
        });

        // Load số bàn
        FirebaseService.getInstance().listenTablesRealtime((value, error) -> {
            if (error != null || value == null) return;
            txtTotalTables.setText(String.valueOf(value.size()));
        });
    }
}
