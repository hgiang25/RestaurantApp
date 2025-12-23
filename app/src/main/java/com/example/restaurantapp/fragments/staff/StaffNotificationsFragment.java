package com.example.restaurantapp.fragments.staff;

import android.app.AlertDialog;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class StaffNotificationsFragment extends Fragment implements NotificationAdapter.OnNotificationClickListener {

    private RecyclerView recyclerView;
    private TextView txtEmpty;
    private NotificationAdapter adapter;
    private List<NotificationModel> notificationList = new ArrayList<>();
    private List<NotificationModel> personalList = new ArrayList<>();
    private List<NotificationModel> broadcastList = new ArrayList<>();
    private Set<String> readNotificationIds = new HashSet<>();
    private ListenerRegistration personalListener;
    private ListenerRegistration broadcastListener;
    private ListenerRegistration readListener;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_notifications, container, false);

        recyclerView = view.findViewById(R.id.recyclerNotifications);
        txtEmpty = view.findViewById(R.id.txtEmpty);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new NotificationAdapter(notificationList, this);
        recyclerView.setAdapter(adapter);

        // Button đọc tất cả
        view.findViewById(R.id.btnMarkAllRead).setOnClickListener(v -> markAllAsRead());

        loadReadStatus();
        loadNotifications();

        return view;
    }

    private void markAllAsRead() {
        if (notificationList.isEmpty()) return;

        for (NotificationModel notification : notificationList) {
            if (!readNotificationIds.contains(notification.getId())) {
                FirebaseService.getInstance().markNotificationAsRead(
                        notification.getId(),
                        unused -> {},
                        e -> Log.e("NOTIF_STAFF", "Error marking as read", e)
                );
            }
        }

        if (getContext() != null) {
            android.widget.Toast.makeText(getContext(), "Đã đánh dấu tất cả đã đọc", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private void loadReadStatus() {
        // Listen realtime trạng thái đã đọc
        readListener = FirebaseService.getInstance().listenReadNotifications(readIds -> {
            if (!isAdded()) return;
            readNotificationIds = readIds;
            adapter.setReadNotificationIds(readIds);
        });
    }

    @Override
    public void onNotificationClick(NotificationModel notification, int position) {
        // Hiển thị dialog chi tiết
        showNotificationDetailDialog(notification);
        
        // Đánh dấu đã đọc
        if (!readNotificationIds.contains(notification.getId())) {
            FirebaseService.getInstance().markNotificationAsRead(
                    notification.getId(),
                    unused -> {
                        Log.d("NOTIF_STAFF", "Marked as read: " + notification.getId());
                    },
                    e -> Log.e("NOTIF_STAFF", "Error marking as read", e)
            );
        }
    }

    private void showNotificationDetailDialog(NotificationModel notification) {
        if (getContext() == null || !isAdded()) return;

        String title = notification.getTitle();
        if (title == null || title.isEmpty()) {
            title = "Thông báo";
        }

        String message = notification.getMessage();
        String time = "";
        if (notification.getCreatedAt() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault());
            time = sdf.format(notification.getCreatedAt().toDate());
        }

        new AlertDialog.Builder(getContext())
                .setTitle(title)
                .setMessage(message + "\n\n📅 " + time)
                .setPositiveButton("Đóng", null)
                .show();
    }

    private void loadNotifications() {
        String userId = FirebaseService.getInstance().getCurrentUserId();
        if (userId == null || !isAdded()) return;

        String role = "staff";

        // 📩 Query 1: Personal notifications (chỉ cho user này)
        personalListener = FirebaseService.getInstance()
                .getDb()
                .collection("notifications")
                .whereEqualTo("type", "personal")
                .whereEqualTo("targetUserId", userId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("NOTIF_STAFF", "Lỗi personal query: " + error.getMessage());
                        return;
                    }
                    if (!isAdded() || value == null) return;

                    Log.d("NOTIF_STAFF", "Personal notifications: " + value.size());

                    personalList.clear();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        NotificationModel n = doc.toObject(NotificationModel.class);
                        if (n != null) {
                            n.setId(doc.getId());
                            personalList.add(n);
                            Log.d("NOTIF_STAFF", "Personal: " + n.getMessage());
                        }
                    }

                    mergeAndUpdateUI();
                });

        // 📢 Query 2: Broadcast notifications (cho role staff)
        broadcastListener = FirebaseService.getInstance()
                .getDb()
                .collection("notifications")
                .whereEqualTo("type", "broadcast")
                .whereArrayContains("roles", role)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("NOTIF_STAFF", "Lỗi broadcast query: " + error.getMessage());
                        return;
                    }
                    if (!isAdded() || value == null) return;

                    Log.d("NOTIF_STAFF", "Broadcast notifications: " + value.size());

                    broadcastList.clear();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        NotificationModel n = doc.toObject(NotificationModel.class);
                        if (n != null) {
                            n.setId(doc.getId());
                            broadcastList.add(n);
                            Log.d("NOTIF_STAFF", "Broadcast: " + n.getMessage());
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

        Log.d("NOTIF_STAFF", "Tổng notifications: " + notificationList.size());

        if (notificationList.isEmpty()) {
            txtEmpty.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            txtEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }

        adapter.notifyDataSetChanged();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (personalListener != null) {
            personalListener.remove();
        }
        if (broadcastListener != null) {
            broadcastListener.remove();
        }
        if (readListener != null) {
            readListener.remove();
        }
    }
}
