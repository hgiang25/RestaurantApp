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
import com.example.restaurantapp.adapters.NotificationAdapter;
import com.example.restaurantapp.api.FirebaseService;
import com.example.restaurantapp.models.NotificationModel;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CustomerNotificationsFragment extends Fragment {

    private RecyclerView recyclerView;
    private TextView txtEmpty;
    private NotificationAdapter adapter;
    private List<NotificationModel> notificationList = new ArrayList<>();
    private List<NotificationModel> personalList = new ArrayList<>();
    private List<NotificationModel> broadcastList = new ArrayList<>();
    private ListenerRegistration personalListener;
    private ListenerRegistration broadcastListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notifications, container, false);

        recyclerView = view.findViewById(R.id.recyclerNotifications);
        txtEmpty = view.findViewById(R.id.txtEmpty);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new NotificationAdapter(notificationList);
        recyclerView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        loadNotifications();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (personalListener != null) {
            personalListener.remove();
            personalListener = null;
        }
        if (broadcastListener != null) {
            broadcastListener.remove();
            broadcastListener = null;
        }
    }

    private void loadNotifications() {
        String userId = FirebaseService.getInstance().getCurrentUserId();
        if (userId == null || !isAdded()) return;

        String role = "customer";
        Log.d("NOTIF_CUSTOMER", "userId = " + userId + ", role = " + role);

        // 📩 Query 1: Personal notifications (chỉ cho user này)
        personalListener = FirebaseService.getInstance()
                .getDb()
                .collection("notifications")
                .whereEqualTo("type", "personal")
                .whereEqualTo("targetUserId", userId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("NOTIF_CUSTOMER", "Lỗi personal query: " + error.getMessage());
                        return;
                    }
                    if (!isAdded() || value == null) return;

                    Log.d("NOTIF_CUSTOMER", "Personal notifications: " + value.size());

                    personalList.clear();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        NotificationModel n = doc.toObject(NotificationModel.class);
                        if (n != null) {
                            n.setId(doc.getId());
                            personalList.add(n);
                            Log.d("NOTIF_CUSTOMER", "Personal: " + n.getMessage());
                        }
                    }

                    mergeAndUpdateUI();
                });

        // 📢 Query 2: Broadcast notifications (cho role customer)
        broadcastListener = FirebaseService.getInstance()
                .getDb()
                .collection("notifications")
                .whereEqualTo("type", "broadcast")
                .whereArrayContains("roles", role)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("NOTIF_CUSTOMER", "Lỗi broadcast query: " + error.getMessage());
                        return;
                    }
                    if (!isAdded() || value == null) return;

                    Log.d("NOTIF_CUSTOMER", "Broadcast notifications: " + value.size());

                    broadcastList.clear();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        NotificationModel n = doc.toObject(NotificationModel.class);
                        if (n != null) {
                            n.setId(doc.getId());
                            broadcastList.add(n);
                            Log.d("NOTIF_CUSTOMER", "Broadcast: " + n.getMessage());
                        }
                    }

                    mergeAndUpdateUI();
                });
    }

    private void mergeAndUpdateUI() {
        if (!isAdded()) return;

        notificationList.clear();
        notificationList.addAll(personalList);
        notificationList.addAll(broadcastList);

        // Sắp xếp theo thời gian mới nhất
        Collections.sort(notificationList, (a, b) -> {
            if (a.getCreatedAt() == null || b.getCreatedAt() == null) return 0;
            return b.getCreatedAt().compareTo(a.getCreatedAt());
        });

        Log.d("NOTIF_CUSTOMER", "Tổng notifications: " + notificationList.size());

        if (notificationList.isEmpty()) {
            txtEmpty.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            txtEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }

        adapter.notifyDataSetChanged();
    }
}
