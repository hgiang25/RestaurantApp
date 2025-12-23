package com.example.restaurantapp.fragments.customer;

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
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class CustomerNotificationsFragment extends Fragment {

    private RecyclerView recyclerView;
    private TextView txtEmpty;
    private NotificationAdapter adapter;
    private List<NotificationModel> notificationList = new ArrayList<>();
    private ListenerRegistration notificationListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notifications, container, false);

        recyclerView = view.findViewById(R.id.recyclerNotifications);
        txtEmpty = view.findViewById(R.id.txtEmpty);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new NotificationAdapter(notificationList);
        recyclerView.setAdapter(adapter);

        //loadNotifications();


        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        String userId = FirebaseService.getInstance().getCurrentUserId();
        String role = "customer";

        notificationListener = FirebaseService.getInstance()
                .listenNotificationsForUser(userId, role, mergedList -> {

                    if (!isAdded()) return;

                    notificationList.clear();
                    notificationList.addAll(mergedList);

                    txtEmpty.setVisibility(notificationList.isEmpty() ? View.VISIBLE : View.GONE);
                    recyclerView.setVisibility(notificationList.isEmpty() ? View.GONE : View.VISIBLE);
                    adapter.notifyDataSetChanged();
                });
    }

    @Override
    public void onStop() {
        super.onStop();
        if (notificationListener != null) {
            notificationListener.remove();
            notificationListener = null;
        }
    }


    private void loadNotifications() {
        String userId = FirebaseService.getInstance().getCurrentUserId();
        if (userId == null || !isAdded()) return;

        String role = "customer";

        FirebaseService.getInstance()
                .getDb()
                .collection("notifications")
                .whereIn("type", List.of("personal", "broadcast"))
                .orderBy("createdAt", Query.Direction.DESCENDING)
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
