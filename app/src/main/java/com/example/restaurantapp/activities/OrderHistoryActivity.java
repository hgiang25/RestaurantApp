package com.example.restaurantapp.activities;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.restaurantapp.R;
import com.example.restaurantapp.adapters.OrderHistoryAdapter;
import com.example.restaurantapp.api.FirebaseService;
import com.example.restaurantapp.utils.LocaleHelper;
import com.example.restaurantapp.utils.PreferenceManager;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.MetadataChanges;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class OrderHistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerOrders;
    private TextView txtEmpty;
    private OrderHistoryAdapter adapter;
    private List<DocumentSnapshot> orderList = new ArrayList<>();
    private ListenerRegistration ordersListener;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply saved settings
        PreferenceManager prefs = new PreferenceManager(this);
        prefs.applySavedSettings();
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_history);

        initViews();
        loadOrders();
    }

    private void initViews() {
        recyclerOrders = findViewById(R.id.recyclerOrders);
        txtEmpty = findViewById(R.id.txtEmpty);
        ImageButton btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        recyclerOrders.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OrderHistoryAdapter(orderList);
        recyclerOrders.setAdapter(adapter);
    }

    private void loadOrders() {
        String userId = FirebaseService.getInstance().getCurrentUserId();
        if (userId == null) return;

        ordersListener = FirebaseService.getInstance().getDb()
                .collection("orders")
                .whereEqualTo("customerId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener(MetadataChanges.INCLUDE, (value, error) -> {
                    if (error != null || value == null) return;

                    orderList.clear();
                    orderList.addAll(value.getDocuments());
                    adapter.notifyDataSetChanged();

                    if (orderList.isEmpty()) {
                        txtEmpty.setVisibility(View.VISIBLE);
                        recyclerOrders.setVisibility(View.GONE);
                    } else {
                        txtEmpty.setVisibility(View.GONE);
                        recyclerOrders.setVisibility(View.VISIBLE);
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ordersListener != null) {
            ordersListener.remove();
        }
    }
}
