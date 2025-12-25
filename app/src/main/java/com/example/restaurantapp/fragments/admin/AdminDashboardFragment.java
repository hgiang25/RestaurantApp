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
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.DocumentSnapshot;

import android.content.Intent;

import com.example.restaurantapp.fragments.admin.AdminTablesFragment;
import com.example.restaurantapp.fragments.admin.AdminMenuFragment;
import com.example.restaurantapp.fragments.admin.AdminStaffFragment;
import com.example.restaurantapp.fragments.admin.AdminReportsFragment;


public class AdminDashboardFragment extends Fragment {

    private TextView txtTotalOrders, txtTotalRevenue, txtTotalStaff, txtTotalTables,txtFreeTables;
    private TextView txtPendingOrders;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_dashboard, container, false);

        txtTotalOrders = view.findViewById(R.id.txtTotalOrders);
        txtTotalRevenue = view.findViewById(R.id.txtTotalRevenue);
        txtTotalStaff = view.findViewById(R.id.txtTotalStaff);
        txtTotalTables = view.findViewById(R.id.txtTotalTables);
        txtPendingOrders = view.findViewById(R.id.txtPendingOrders);
        txtFreeTables = view.findViewById(R.id.txtFreeTables); // thêm dòng này

        // Quick Actions
        View btnQuickAddTable = view.findViewById(R.id.btnQuickAddTable);
        View btnQuickAddMenu = view.findViewById(R.id.btnQuickAddMenu);
        View btnQuickAddStaff = view.findViewById(R.id.btnQuickAddStaff);
        View btnQuickReports = view.findViewById(R.id.btnQuickReports);

        btnQuickAddTable.setOnClickListener(v ->
                selectBottomNavItem(R.id.nav_tables)
        );

        btnQuickAddMenu.setOnClickListener(v ->
                selectBottomNavItem(R.id.nav_menu)
        );

        btnQuickAddStaff.setOnClickListener(v ->
                selectBottomNavItem(R.id.nav_staff)
        );

        btnQuickReports.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putBoolean("open_reports", true);

            selectBottomNavItem(R.id.nav_more);

            requireActivity()
                    .getSupportFragmentManager()
                    .setFragmentResult("open_more", args);
        });


        loadDashboardData();

        return view;
    }

    private void selectBottomNavItem(int menuItemId) {
        if (!isAdded()) return;

        BottomNavigationView bottomNav =
                requireActivity().findViewById(R.id.bottomNav);

        bottomNav.setSelectedItemId(menuItemId);
    }


    private void openFragment(Fragment fragment) {
        requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit();
    }


    private void loadDashboardData() {
        // Load tổng số đơn hàng
        FirebaseService.getInstance().listenOrdersRealtime((value, error) -> {
            if (!isAdded() || getContext() == null) return;
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
            
            if (txtTotalOrders != null) txtTotalOrders.setText(String.valueOf(total));
            if (txtPendingOrders != null) txtPendingOrders.setText(pending + " chờ xử lý");
            if (txtTotalRevenue != null) txtTotalRevenue.setText(String.format("%,.0f đ", revenue));
        });

        // Load số nhân viên
        FirebaseService.getInstance().listenUsersByRoleRealtime("staff", (value, error) -> {
            if (!isAdded() || getContext() == null) return;
            if (error != null || value == null) return;
            if (txtTotalStaff != null) txtTotalStaff.setText(String.valueOf(value.size()));
        });

        // Load số bàn
        FirebaseService.getInstance().listenTablesRealtime("dashboard", (value, error) -> {
            if (!isAdded() || getContext() == null || value == null) return;

            int totalTables = value.size();
            int freeTables = 0;

            for (DocumentSnapshot doc : value.getDocuments()) {
                String status = doc.getString("status");
                if ("free".equals(status)) {
                    freeTables++;
                }
            }

            if (txtTotalTables != null) txtTotalTables.setText(String.valueOf(totalTables));
            if (txtFreeTables != null) txtFreeTables.setText(freeTables + " còn trống");
        });

    }
}
