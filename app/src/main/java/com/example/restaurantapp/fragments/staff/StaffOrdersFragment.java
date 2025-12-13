package com.example.restaurantapp.fragments.staff;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.restaurantapp.R;
import com.example.restaurantapp.adapters.OrderAdapter;
import com.example.restaurantapp.api.FirebaseService;
import com.example.restaurantapp.models.OrderModel;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class StaffOrdersFragment extends Fragment {

    private RecyclerView recyclerView;
    private OrderAdapter adapter;
    private List<OrderModel> orderList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_staff_orders, container, false);

        recyclerView = view.findViewById(R.id.recyclerOrders);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new OrderAdapter(orderList, this::showOrderStatusDialog);
        recyclerView.setAdapter(adapter);

        loadOrders();

        return view;
    }

    private void loadOrders() {
        FirebaseService.getInstance().listenOrdersRealtime((value, error) -> {
            if (error != null || value == null) return;

            orderList.clear();
            for (DocumentSnapshot doc : value.getDocuments()) {
                OrderModel order = doc.toObject(OrderModel.class);
                if (order != null) {
                    order.setId(doc.getId());
                    orderList.add(order);
                }
            }
            // Sắp xếp theo thời gian tạo
            orderList.sort((o1, o2) -> {
                if (o1.getCreatedAt() == null || o2.getCreatedAt() == null) return 0;
                return o2.getCreatedAt().compareTo(o1.getCreatedAt());
            });
            adapter.notifyDataSetChanged();
        });
    }

    private void showOrderStatusDialog(OrderModel order) {
        String[] statuses = {"pending", "confirmed", "preparing", "served", "paid"};
        String[] statusLabels = {"Chờ xác nhận", "Đã xác nhận", "Đang chuẩn bị", "Đã phục vụ", "Đã thanh toán"};

        new android.app.AlertDialog.Builder(getContext())
                .setTitle("Cập nhật trạng thái đơn hàng")
                .setItems(statusLabels, (dialog, which) -> {
                    FirebaseService.getInstance().updateOrderStatus(order.getId(), statuses[which],
                            unused -> {},
                            e -> {});
                })
                .show();
    }
}
