package com.example.restaurantapp.fragments.admin;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.restaurantapp.R;
import com.example.restaurantapp.api.FirebaseService;
import com.example.restaurantapp.authentication.LoginActivity;
import com.google.android.material.button.MaterialButton;

import java.util.Arrays;

public class AdminMoreFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_more, container, false);

        MaterialButton btnInventory = view.findViewById(R.id.btnInventory);
        MaterialButton btnPromotions = view.findViewById(R.id.btnPromotions);
        MaterialButton btnReports = view.findViewById(R.id.btnReports);
        MaterialButton btnNotifications = view.findViewById(R.id.btnNotifications);
        MaterialButton btnLogout = view.findViewById(R.id.btnLogout);

        btnInventory.setOnClickListener(v -> navigateToFragment(new AdminInventoryFragment()));
        btnPromotions.setOnClickListener(v -> navigateToFragment(new AdminPromotionsFragment()));
        btnReports.setOnClickListener(v -> navigateToFragment(new AdminReportsFragment()));
        btnNotifications.setOnClickListener(v -> showBroadcastDialog());
        
        btnLogout.setOnClickListener(v -> {
            FirebaseService.getInstance().logout();
            startActivity(new Intent(getActivity(), LoginActivity.class));
            getActivity().finish();
        });

        return view;
    }

    private void navigateToFragment(Fragment fragment) {
        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void showBroadcastDialog() {
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        EditText edtMessage = new EditText(getContext());
        edtMessage.setHint("Nội dung thông báo");
        edtMessage.setMinLines(3);
        layout.addView(edtMessage);

        String[] roles = {"staff", "customer"};
        String[] roleLabels = {"Nhân viên", "Khách hàng", "Tất cả"};

        new AlertDialog.Builder(getContext())
                .setTitle("Gửi thông báo")
                .setView(layout)
                .setItems(roleLabels, (dialog, which) -> {
                    String message = edtMessage.getText().toString().trim();
                    if (message.isEmpty()) {
                        Toast.makeText(getContext(), "Vui lòng nhập nội dung", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    java.util.List<String> targetRoles;
                    if (which == 2) {
                        targetRoles = Arrays.asList("staff", "customer");
                    } else {
                        targetRoles = Arrays.asList(roles[which]);
                    }

                    FirebaseService.getInstance().broadcastNotification(message, targetRoles,
                            unused -> Toast.makeText(getContext(), "Đã gửi thông báo!", Toast.LENGTH_SHORT).show(),
                            e -> Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}
