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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AdminReportsFragment extends Fragment {

    private TextView tvTodayOrders, tvTodayRevenue, tvMonthRevenue, tvTotalOrders;
    private TextView tvPendingOrders, tvCompletedOrders, tvCancelledOrders;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_reports, container, false);

        tvTodayOrders = view.findViewById(R.id.tvTodayOrders);
        tvTodayRevenue = view.findViewById(R.id.tvTodayRevenue);
        tvMonthRevenue = view.findViewById(R.id.tvMonthRevenue);
        tvTotalOrders = view.findViewById(R.id.tvTotalOrders);
        tvPendingOrders = view.findViewById(R.id.tvPendingOrders);
        tvCompletedOrders = view.findViewById(R.id.tvCompletedOrders);
        tvCancelledOrders = view.findViewById(R.id.tvCancelledOrders);

        loadReports();

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
}
