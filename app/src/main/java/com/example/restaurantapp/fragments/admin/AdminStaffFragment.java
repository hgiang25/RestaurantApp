package com.example.restaurantapp.fragments.admin;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

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
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AdminStaffFragment extends Fragment {

    private RecyclerView recyclerView;
    private EditText edtSearch;
    private StaffAdapter adapter;

    // List hiển thị
    private final List<User> staffList = new ArrayList<>();
    // List gốc để filter
    private final List<User> fullStaffList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.fragment_admin_staff, container, false);

        recyclerView = view.findViewById(R.id.recyclerStaff);
        edtSearch = view.findViewById(R.id.edtSearch);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new StaffAdapter(staffList, this::onStaffClick);
        recyclerView.setAdapter(adapter);

        ExtendedFloatingActionButton fabAdd = view.findViewById(R.id.fabAddStaff);
        fabAdd.setOnClickListener(v ->
                startActivity(new Intent(getActivity(), RegisterStaffActivity.class))
        );

        setupSearch();
        loadStaff();

        return view;
    }

    /**
     * Load toàn bộ nhân viên (realtime)
     */
    private void loadStaff() {
        FirebaseService.getInstance()
                .listenUsersByRoleRealtime("staff", (value, error) -> {
                    if (!isAdded() || value == null) return;

                    staffList.clear();
                    fullStaffList.clear();

                    for (DocumentSnapshot doc : value.getDocuments()) {
                        User user = doc.toObject(User.class);
                        if (user != null) {
                            user.setId(doc.getId());
                            staffList.add(user);
                            fullStaffList.add(user);
                        }
                    }

                    adapter.notifyDataSetChanged();
                });
    }

    /**
     * Setup tìm kiếm theo tên
     */
    private void setupSearch() {
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterStaff(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    /**
     * Lọc danh sách nhân viên theo tên
     */
    private void filterStaff(String keyword) {
        staffList.clear();

        if (keyword == null || keyword.trim().isEmpty()) {
            staffList.addAll(fullStaffList);
        } else {
            String search = keyword.toLowerCase().trim();
            for (User user : fullStaffList) {
                if (user.getUsername() != null &&
                        user.getUsername().toLowerCase().contains(search)) {
                    staffList.add(user);
                }
            }
        }

        adapter.notifyDataSetChanged();
    }

    /**
     * Click vào nhân viên
     */
    private void onStaffClick(User staff) {
        if (!isAdded() || getContext() == null) return;

        new android.app.AlertDialog.Builder(getContext())
                .setTitle(staff.getUsername())
                .setMessage("Email: " + staff.getEmail())
                .setPositiveButton("Đóng", null)
                .show();
    }
}
