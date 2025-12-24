package com.example.restaurantapp.fragments.customer;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.restaurantapp.R;
import com.example.restaurantapp.adapters.CustomerOrderAdapter;
import com.example.restaurantapp.api.FirebaseService;
import com.example.restaurantapp.models.OrderModel;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class CustomerOrdersFragment extends Fragment {

    private static final String TAG = "CustomerOrders";
    private RecyclerView recyclerView;
    private TextView txtEmpty;
    private CustomerOrderAdapter adapter;
    private List<OrderModel> orderList = new ArrayList<>();
    private ListenerRegistration ordersListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_customer_orders, container, false);

        recyclerView = view.findViewById(R.id.recyclerOrders);
        txtEmpty = view.findViewById(R.id.txtEmpty);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new CustomerOrderAdapter(orderList);
        recyclerView.setAdapter(adapter);

        loadOrders();

        return view;
    }

    private void loadOrders() {
        String customerId = FirebaseService.getInstance().getCurrentUserId();
        if (customerId == null) {
            Log.e(TAG, "customerId is null!");
            return;
        }

        // Hủy listener cũ nếu có
        if (ordersListener != null) {
            ordersListener.remove();
        }

        Log.d(TAG, "Bắt đầu listen orders cho customer: " + customerId);

        ordersListener = FirebaseService.getInstance().listenOrdersByCustomerRealtime(customerId, (value, error) -> {
            if (error != null) {
                Log.e(TAG, "Lỗi listen orders: " + error.getMessage());
                return;
            }
            if (value == null) {
                Log.w(TAG, "Value is null");
                return;
            }
            if (!isAdded()) {
                Log.w(TAG, "Fragment not attached");
                return;
            }

            // Log metadata để biết data từ cache hay server
            boolean fromCache = value.getMetadata().isFromCache();
            Log.d(TAG, "Nhận " + value.size() + " orders (fromCache: " + fromCache + ")");

            orderList.clear();
            for (DocumentSnapshot doc : value.getDocuments()) {
                OrderModel order = doc.toObject(OrderModel.class);
                if (order != null) {
                    order.setId(doc.getId());
                    orderList.add(order);
                    Log.d(TAG, "Order: " + doc.getId() + " - Status: " + order.getStatus());
                }
            }

            // Sắp xếp theo thời gian tạo (mới nhất trước)
            orderList.sort((o1, o2) -> {
                if (o1.getCreatedAt() == null || o2.getCreatedAt() == null) return 0;
                return o2.getCreatedAt().compareTo(o1.getCreatedAt());
            });

            if (orderList.isEmpty()) {
                txtEmpty.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                txtEmpty.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            }

            adapter.notifyDataSetChanged();
        });
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
