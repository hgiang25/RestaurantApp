package com.example.restaurantapp.fragments.staff;

import android.os.Bundle;
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

import java.util.ArrayList;
import java.util.List;

public class StaffNotificationsFragment extends Fragment {

    private RecyclerView recyclerView;
    private TextView txtEmpty;
    private NotificationAdapter adapter;
    private List<NotificationModel> notificationList = new ArrayList<>();

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
        adapter = new NotificationAdapter(notificationList);
        recyclerView.setAdapter(adapter);

        loadNotifications();

        return view;
    }

    private void loadNotifications() {
        String userId = FirebaseService.getInstance().getCurrentUserId();
        if (userId == null || !isAdded()) return;

        String role = "staff";

        FirebaseService.getInstance()
                .getDb()
                .collection("notifications")
                .whereIn("type", List.of("personal", "broadcast"))
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (!isAdded() || error != null || value == null) return;

                    notificationList.clear();

                    for (DocumentSnapshot doc : value.getDocuments()) {

                        String type = doc.getString("type");

                        // 📩 Personal
                        if ("personal".equals(type)) {
                            String targetUserId = doc.getString("targetUserId");
                            if (userId.equals(targetUserId)) {
                                NotificationModel n = doc.toObject(NotificationModel.class);
                                if (n != null) {
                                    n.setId(doc.getId());
                                    notificationList.add(n);
                                }
                            }
                        }

                        // 📢 Broadcast
                        if ("broadcast".equals(type)) {
                            List<String> roles = (List<String>) doc.get("roles");
                            if (roles != null && roles.contains(role)) {
                                NotificationModel n = doc.toObject(NotificationModel.class);
                                if (n != null) {
                                    n.setId(doc.getId());
                                    notificationList.add(n);
                                }
                            }
                        }
                    }

                    if (notificationList.isEmpty()) {
                        txtEmpty.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    } else {
                        txtEmpty.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                    }

                    adapter.notifyDataSetChanged();
                });
    }

}
