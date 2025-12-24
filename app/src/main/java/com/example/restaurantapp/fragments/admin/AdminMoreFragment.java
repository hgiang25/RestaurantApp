package com.example.restaurantapp.fragments.admin;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.restaurantapp.R;
import com.example.restaurantapp.api.FirebaseService;
import com.example.restaurantapp.authentication.LoginActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AdminMoreFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_more, container, false);

        MaterialButton btnInventory = view.findViewById(R.id.btnInventory);
        MaterialButton btnRecipes = view.findViewById(R.id.btnRecipes);
        MaterialButton btnPromotions = view.findViewById(R.id.btnPromotions);
        MaterialButton btnReports = view.findViewById(R.id.btnReports);
        MaterialButton btnReservations = view.findViewById(R.id.btnReservations);
        MaterialButton btnOrders = view.findViewById(R.id.btnOrders);
        MaterialButton btnNotifications = view.findViewById(R.id.btnNotifications);
        MaterialButton btnLoyalty = view.findViewById(R.id.btnLoyalty);
        MaterialButton btnAuditLogs = view.findViewById(R.id.btnAuditLogs);
        MaterialButton btnLogout = view.findViewById(R.id.btnLogout);

        btnInventory.setOnClickListener(v -> navigateToFragment(new AdminInventoryFragment()));
        btnRecipes.setOnClickListener(v -> navigateToFragment(new AdminRecipeFragment()));
        btnPromotions.setOnClickListener(v -> navigateToFragment(new AdminPromotionsFragment()));
        btnReports.setOnClickListener(v -> navigateToFragment(new AdminReportsFragment()));
        btnReservations.setOnClickListener(v -> navigateToFragment(new AdminReservationsFragment()));
        btnOrders.setOnClickListener(v -> navigateToFragment(new AdminOrdersFragment()));
        btnNotifications.setOnClickListener(v -> showNotificationDialog());
        btnLoyalty.setOnClickListener(v -> showLoyaltyManagementDialog());
        btnAuditLogs.setOnClickListener(v -> showAuditLogsDialog());
        
        btnLogout.setOnClickListener(v -> {
            if (getActivity() == null) return;
            FirebaseService.getInstance().logout();
            startActivity(new Intent(getActivity(), LoginActivity.class));
            getActivity().finish();
        });

        return view;
    }

    private void showNotificationDialog() {
        if (getContext() == null || !isAdded()) return;

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        // Tiêu đề
        EditText edtTitle = new EditText(getContext());
        edtTitle.setHint("Tiêu đề thông báo");
        layout.addView(edtTitle);

        // Nội dung
        EditText edtMessage = new EditText(getContext());
        edtMessage.setHint("Nội dung thông báo");
        edtMessage.setMinLines(3);
        layout.addView(edtMessage);

        new AlertDialog.Builder(getContext())
                .setTitle("Tạo thông báo")
                .setView(layout)
                .setPositiveButton("Tiếp tục", (dialog, which) -> {
                    String title = edtTitle.getText().toString().trim();
                    String message = edtMessage.getText().toString().trim();

                    if (title.isEmpty() || message.isEmpty()) {
                        Toast.makeText(getContext(), "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Chọn kiểu gửi
                    showRecipientSelectionDialog(title, message);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showRecipientSelectionDialog(String title, String message) {
        if (getContext() == null || !isAdded()) return;

        String[] options = {
                "Gửi cho Nhân viên",
                "Gửi cho Khách hàng",
                "Gửi cho Tất cả",
                "Gửi cho 1 người cụ thể"
        };

        new AlertDialog.Builder(getContext())
                .setTitle("Chọn người nhận")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: // Nhân viên
                            sendBroadcastNotification(title, message, Arrays.asList("staff"));
                            break;
                        case 1: // Khách hàng
                            sendBroadcastNotification(title, message, Arrays.asList("customer"));
                            break;
                        case 2: // Tất cả
                            sendBroadcastNotification(title, message, Arrays.asList("staff", "customer"));
                            break;
                        case 3: // 1 người cụ thể
                            showUserIdInputDialog(title, message);
                            break;
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showUserIdInputDialog(String title, String message) {
        if (getContext() == null || !isAdded()) return;

        EditText edtUserId = new EditText(getContext());
        edtUserId.setHint("Nhập User ID người nhận");
        edtUserId.setPadding(50, 40, 50, 10);

        new AlertDialog.Builder(getContext())
                .setTitle("Gửi cho 1 người")
                .setView(edtUserId)
                .setPositiveButton("Gửi", (dialog, which) -> {
                    String userId = edtUserId.getText().toString().trim();
                    if (userId.isEmpty()) {
                        Toast.makeText(getContext(), "Vui lòng nhập User ID", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    sendPersonalNotification(title, message, userId);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void sendBroadcastNotification(String title, String message, java.util.List<String> roles) {
        FirebaseService.getInstance().broadcastNotification(title, message, roles,
                unused -> {
                    if (getContext() != null && isAdded()) {
                        Toast.makeText(getContext(), "Đã gửi thông báo!", Toast.LENGTH_SHORT).show();
                    }
                },
                e -> {
                    if (getContext() != null && isAdded()) {
                        Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void sendPersonalNotification(String title, String message, String userId) {
        FirebaseService.getInstance().sendNotification(userId, title, message,
                ref -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Đã gửi thông báo!", Toast.LENGTH_SHORT).show();
                    }
                },
                e -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private void navigateToFragment(Fragment fragment) {
        try {
            if (!isAdded() || getActivity() == null || getActivity().isFinishing()) return;
            
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .addToBackStack(null)
                    .commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showLoyaltyManagementDialog() {
        if (getContext() == null || !isAdded()) return;

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_loyalty_management, null);
        
        EditText edtCustomerEmail = dialogView.findViewById(R.id.edtCustomerEmail);
        EditText edtPoints = dialogView.findViewById(R.id.edtPoints);
        TextView tvCurrentPoints = dialogView.findViewById(R.id.tvCurrentPoints);
        MaterialButton btnSearch = dialogView.findViewById(R.id.btnSearchCustomer);
        MaterialButton btnAddPoints = dialogView.findViewById(R.id.btnAddPoints);
        MaterialButton btnRedeemPoints = dialogView.findViewById(R.id.btnRedeemPoints);

        final String[] foundCustomerId = {null};

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .create();

        btnSearch.setOnClickListener(v -> {
            String email = edtCustomerEmail.getText().toString().trim();
            if (email.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng nhập email", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseService.getInstance().getDb().collection("users")
                    .whereEqualTo("email", email)
                    .whereEqualTo("role", "customer")
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                            foundCustomerId[0] = doc.getId();
                            Long points = doc.getLong("loyaltyPoints");
                            tvCurrentPoints.setText(String.format("Điểm hiện tại: %d", points != null ? points : 0));
                            tvCurrentPoints.setVisibility(View.VISIBLE);
                        } else {
                            Toast.makeText(getContext(), "Không tìm thấy khách hàng", Toast.LENGTH_SHORT).show();
                            foundCustomerId[0] = null;
                            tvCurrentPoints.setVisibility(View.GONE);
                        }
                    });
        });

        btnAddPoints.setOnClickListener(v -> {
            if (foundCustomerId[0] == null) {
                Toast.makeText(getContext(), "Vui lòng tìm khách hàng trước", Toast.LENGTH_SHORT).show();
                return;
            }
            String pointsStr = edtPoints.getText().toString().trim();
            if (pointsStr.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng nhập số điểm", Toast.LENGTH_SHORT).show();
                return;
            }

            int points = Integer.parseInt(pointsStr);
            FirebaseService.getInstance().addLoyaltyPoints(foundCustomerId[0], points,
                    aVoid -> {
                        Toast.makeText(getContext(), "Đã cộng " + points + " điểm", Toast.LENGTH_SHORT).show();
                        logAction("ADD_LOYALTY_POINTS", foundCustomerId[0], points);
                        btnSearch.performClick(); // Refresh points
                        edtPoints.setText("");
                    },
                    e -> Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show()
            );
        });

        btnRedeemPoints.setOnClickListener(v -> {
            if (foundCustomerId[0] == null) {
                Toast.makeText(getContext(), "Vui lòng tìm khách hàng trước", Toast.LENGTH_SHORT).show();
                return;
            }
            String pointsStr = edtPoints.getText().toString().trim();
            if (pointsStr.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng nhập số điểm", Toast.LENGTH_SHORT).show();
                return;
            }

            int points = Integer.parseInt(pointsStr);
            FirebaseService.getInstance().redeemLoyaltyPoints(foundCustomerId[0], points,
                    aVoid -> {
                        Toast.makeText(getContext(), "Đã trừ " + points + " điểm", Toast.LENGTH_SHORT).show();
                        logAction("REDEEM_LOYALTY_POINTS", foundCustomerId[0], points);
                        btnSearch.performClick(); // Refresh points
                        edtPoints.setText("");
                    },
                    e -> Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show()
            );
        });

        dialogView.findViewById(R.id.btnClose).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void logAction(String action, String target, int points) {
        String userId = FirebaseService.getInstance().getCurrentUserId();
        Map<String, Object> extra = new HashMap<>();
        extra.put("points", points);
        FirebaseService.getInstance().logAction(userId, action, target, extra, ref -> {}, e -> {});
    }

    private void showAuditLogsDialog() {
        if (getContext() == null || !isAdded()) return;

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_audit_logs, null);
        RecyclerView recyclerView = dialogView.findViewById(R.id.recyclerAuditLogs);
        TextView tvEmpty = dialogView.findViewById(R.id.tvEmpty);
        
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .create();

        FirebaseService.getInstance().getDb().collection("audit_logs")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener((value, error) -> {
                    if (!isAdded() || getContext() == null) return;
                    if (error != null || value == null) return;

                    if (value.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    } else {
                        tvEmpty.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                        
                        AuditLogAdapter adapter = new AuditLogAdapter(value.getDocuments());
                        recyclerView.setAdapter(adapter);
                    }
                });

        dialogView.findViewById(R.id.btnClose).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    // Inner class for Audit Log Adapter
    private class AuditLogAdapter extends RecyclerView.Adapter<AuditLogAdapter.ViewHolder> {
        private java.util.List<DocumentSnapshot> logs;
        private SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

        AuditLogAdapter(java.util.List<DocumentSnapshot> logs) {
            this.logs = logs;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_audit_log, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            DocumentSnapshot doc = logs.get(position);
            holder.bind(doc);
        }

        @Override
        public int getItemCount() {
            return logs.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvAction, tvTarget, tvTime, tvExtra;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvAction = itemView.findViewById(R.id.tvAction);
                tvTarget = itemView.findViewById(R.id.tvTarget);
                tvTime = itemView.findViewById(R.id.tvTime);
                tvExtra = itemView.findViewById(R.id.tvExtra);
            }

            void bind(DocumentSnapshot doc) {
                String action = doc.getString("action");
                String target = doc.getString("target");
                com.google.firebase.Timestamp createdAt = doc.getTimestamp("createdAt");
                Map<String, Object> extra = (Map<String, Object>) doc.get("extra");

                tvAction.setText(getActionLabel(action));
                tvTarget.setText("ID: " + (target != null ? target.substring(0, Math.min(8, target.length())) + "..." : "N/A"));
                tvTime.setText(createdAt != null ? sdf.format(createdAt.toDate()) : "");
                
                if (extra != null && !extra.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    for (Map.Entry<String, Object> entry : extra.entrySet()) {
                        sb.append(entry.getKey()).append(": ").append(entry.getValue()).append(" ");
                    }
                    tvExtra.setText(sb.toString().trim());
                    tvExtra.setVisibility(View.VISIBLE);
                } else {
                    tvExtra.setVisibility(View.GONE);
                }
            }

            private String getActionLabel(String action) {
                if (action == null) return "Không xác định";
                switch (action) {
                    case "ADD_LOYALTY_POINTS": return "➕ Cộng điểm";
                    case "REDEEM_LOYALTY_POINTS": return "➖ Trừ điểm";
                    case "CREATE_ORDER": return "📝 Tạo đơn";
                    case "UPDATE_ORDER": return "✏️ Sửa đơn";
                    case "DELETE_PROMOTION": return "🗑️ Xóa KM";
                    case "ADD_MENU_ITEM": return "🍽️ Thêm món";
                    default: return action;
                }
            }
        }
    }
}
