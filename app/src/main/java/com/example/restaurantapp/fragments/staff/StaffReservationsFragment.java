package com.example.restaurantapp.fragments.staff;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.restaurantapp.R;
import com.example.restaurantapp.adapters.StaffReservationAdapter;
import com.example.restaurantapp.api.FirebaseService;
import com.example.restaurantapp.models.ReservationModel;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StaffReservationsFragment extends Fragment implements StaffReservationAdapter.OnReservationActionListener {

    private TabLayout tabLayout;
    private RecyclerView recyclerView;
    private LinearLayout emptyState;
    private TextView tvEmptyMessage;
    private ProgressBar progressBar;
    private TextView tvPendingCount;

    private StaffReservationAdapter adapter;
    private List<ReservationModel> reservationList = new ArrayList<>();
    private ListenerRegistration reservationsListener;

    private String currentFilter = "pending"; // pending, confirmed, all

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_staff_reservations, container, false);

        initViews(view);
        setupAdapter();
        setupTabs();
        loadReservations();

        return view;
    }

    private void initViews(View view) {
        tabLayout = view.findViewById(R.id.tabLayout);
        recyclerView = view.findViewById(R.id.recyclerView);
        emptyState = view.findViewById(R.id.emptyState);
        tvEmptyMessage = view.findViewById(R.id.tvEmptyMessage);
        progressBar = view.findViewById(R.id.progressBar);
        tvPendingCount = view.findViewById(R.id.tvPendingCount);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    private void setupAdapter() {
        adapter = new StaffReservationAdapter(reservationList, this);
        recyclerView.setAdapter(adapter);
    }

    private void setupTabs() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        currentFilter = "pending";
                        break;
                    case 1:
                        currentFilter = "confirmed";
                        break;
                    case 2:
                        currentFilter = "all";
                        break;
                }
                loadReservations();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void loadReservations() {
        progressBar.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);

        if (reservationsListener != null) {
            reservationsListener.remove();
        }

        reservationsListener = FirebaseService.getInstance().listenAllReservations((value, error) -> {
            progressBar.setVisibility(View.GONE);

            if (error != null) {
                Toast.makeText(getContext(), "Lỗi: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }

            reservationList.clear();
            int pendingCount = 0;

            if (value != null) {
                for (DocumentSnapshot doc : value.getDocuments()) {
                    ReservationModel reservation = doc.toObject(ReservationModel.class);
                    if (reservation != null) {
                        reservation.setId(doc.getId());

                        // Count pending
                        if ("pending".equals(reservation.getStatus())) {
                            pendingCount++;
                        }

                        // Filter by status
                        if ("all".equals(currentFilter) || 
                            reservation.getStatus().equals(currentFilter)) {
                            reservationList.add(reservation);
                        }
                    }
                }
            }

            // Update pending count badge
            if (tvPendingCount != null) {
                if (pendingCount > 0) {
                    tvPendingCount.setText(String.valueOf(pendingCount));
                    tvPendingCount.setVisibility(View.VISIBLE);
                } else {
                    tvPendingCount.setVisibility(View.GONE);
                }
            }

            // Update UI
            if (reservationList.isEmpty()) {
                emptyState.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
                
                switch (currentFilter) {
                    case "pending":
                        tvEmptyMessage.setText("Không có đặt bàn chờ xác nhận");
                        break;
                    case "confirmed":
                        tvEmptyMessage.setText("Không có đặt bàn đã xác nhận");
                        break;
                    default:
                        tvEmptyMessage.setText("Không có đặt bàn nào");
                }
            } else {
                emptyState.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            }

            adapter.updateData(reservationList);
        });
    }

    @Override
    public void onConfirmClick(ReservationModel reservation) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Xác nhận đặt bàn")
                .setMessage("Xác nhận đặt bàn của " + reservation.getCustomerName() + 
                           "\n\n📅 " + formatDate(reservation.getDate()) + " - " + reservation.getTime() +
                           "\n🪑 " + reservation.getTableName() +
                           "\n👥 " + reservation.getGuests() + " khách")
                .setPositiveButton("Xác nhận", (dialog, which) -> {
                    updateReservationStatus(reservation, "confirmed", "Đã xác nhận đặt bàn");
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    public void onRejectClick(ReservationModel reservation) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Từ chối đặt bàn")
                .setMessage("Bạn có chắc muốn từ chối đặt bàn của " + reservation.getCustomerName() + "?")
                .setPositiveButton("Từ chối", (dialog, which) -> {
                    updateReservationStatus(reservation, "cancelled", "Đã từ chối đặt bàn");
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    public void onCompleteClick(ReservationModel reservation) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Hoàn thành")
                .setMessage("Xác nhận khách đã sử dụng bàn xong?")
                .setPositiveButton("Hoàn thành", (dialog, which) -> {
                    updateReservationStatus(reservation, "completed", "Đã hoàn thành");
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void updateReservationStatus(ReservationModel reservation, String newStatus, String successMessage) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", newStatus);
        updates.put("updatedAt", System.currentTimeMillis());

        FirebaseService.getInstance().updateReservation(reservation.getId(), updates,
                aVoid -> {
                    Toast.makeText(getContext(), successMessage, Toast.LENGTH_SHORT).show();
                },
                e -> {
                    Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private String formatDate(String date) {
        if (date != null && date.contains("-")) {
            String[] parts = date.split("-");
            if (parts.length == 3) {
                return parts[2] + "/" + parts[1] + "/" + parts[0];
            }
        }
        return date;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (reservationsListener != null) {
            reservationsListener.remove();
        }
    }
}
