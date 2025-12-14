package com.example.restaurantapp.fragments.admin;

import android.content.Intent;
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
import com.example.restaurantapp.adapters.StaffAdapter;
import com.example.restaurantapp.api.FirebaseService;
import com.example.restaurantapp.authentication.RegisterStaffActivity;
import com.example.restaurantapp.models.User;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AdminStaffFragment extends Fragment {

    private RecyclerView recyclerView;
    private StaffAdapter adapter;
    private List<User> staffList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_staff, container, false);

        recyclerView = view.findViewById(R.id.recyclerStaff);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new StaffAdapter(staffList, this::onStaffClick);
        recyclerView.setAdapter(adapter);

        FloatingActionButton fabAdd = view.findViewById(R.id.fabAddStaff);
        fabAdd.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), RegisterStaffActivity.class));
        });

        loadStaff();

        return view;
    }

    private void loadStaff() {
        FirebaseService.getInstance().listenUsersByRoleRealtime("staff", (value, error) -> {
            if (!isAdded() || getContext() == null) return;
            if (error != null || value == null) return;

            staffList.clear();
            for (DocumentSnapshot doc : value.getDocuments()) {
                User user = doc.toObject(User.class);
                if (user != null) {
                    user.setId(doc.getId());
                    staffList.add(user);
                }
            }
            if (adapter != null) adapter.notifyDataSetChanged();
        });
    }

    private void onStaffClick(User staff) {
        if (getContext() == null || !isAdded()) return;
        
        // Có thể mở dialog để cập nhật role hoặc xem chi tiết
        new android.app.AlertDialog.Builder(getContext())
                .setTitle(staff.getUsername())
                .setMessage("Email: " + staff.getEmail())
                .setPositiveButton("Đóng", null)
                .show();
    }
}
