package com.example.restaurantapp.fragments.customer;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
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
import com.example.restaurantapp.adapters.ReservationAdapter;
import com.example.restaurantapp.adapters.TableBookingAdapter;
import com.example.restaurantapp.api.FirebaseService;
import com.example.restaurantapp.models.ReservationModel;
import com.example.restaurantapp.models.TableModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CustomerReservationFragment extends Fragment {

    private TabLayout tabLayout;
    private RecyclerView recyclerView;
    private LinearLayout emptyState;
    private TextView tvEmptyMessage;
    private ProgressBar progressBar;

    private TableBookingAdapter tableAdapter;
    private ReservationAdapter reservationAdapter;

    private List<TableModel> tableList = new ArrayList<>();
    private List<ReservationModel> reservationList = new ArrayList<>();

    private ListenerRegistration tablesListener;
    private ListenerRegistration reservationsListener;

    private FirebaseUser currentUser;
    private String currentUserName = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_customer_reservation, container, false);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        tabLayout = view.findViewById(R.id.tabLayout);
        recyclerView = view.findViewById(R.id.recyclerView);
        emptyState = view.findViewById(R.id.emptyState);
        tvEmptyMessage = view.findViewById(R.id.tvEmptyMessage);
        progressBar = view.findViewById(R.id.progressBar);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Setup adapters
        tableAdapter = new TableBookingAdapter(tableList, this::showBookingDialog);
        reservationAdapter = new ReservationAdapter(reservationList, this::cancelReservation);

        // Load user name
        loadCurrentUserName();

        // Tab listener
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    showTablesView();
                } else {
                    showReservationsView();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        // Default: show tables
        showTablesView();

        return view;
    }

    private void loadCurrentUserName() {
        if (currentUser != null) {
            FirebaseService.getInstance().getUser(currentUser.getUid(), 
                doc -> {
                    if (doc != null && doc.exists()) {
                        currentUserName = doc.getString("name");
                        if (currentUserName == null) {
                            currentUserName = currentUser.getEmail();
                        }
                    }
                },
                e -> {
                    currentUserName = currentUser.getEmail();
                });
        }
    }

    private void showTablesView() {
        recyclerView.setAdapter(tableAdapter);
        loadFreeTables();
    }

    private void showReservationsView() {
        recyclerView.setAdapter(reservationAdapter);
        loadMyReservations();
    }

    private void loadFreeTables() {
        progressBar.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);

        if (tablesListener != null) {
            tablesListener.remove();
        }

        tablesListener = FirebaseService.getInstance().listenFreeTables((value, error) -> {
            progressBar.setVisibility(View.GONE);

            if (error != null) {
                Toast.makeText(getContext(), "Lỗi: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }

            tableList.clear();
            if (value != null) {
                for (DocumentSnapshot doc : value.getDocuments()) {
                    TableModel table = doc.toObject(TableModel.class);
                    if (table != null) {
                        table.setId(doc.getId());
                        tableList.add(table);
                    }
                }
            }

            if (tableList.isEmpty()) {
                emptyState.setVisibility(View.VISIBLE);
                tvEmptyMessage.setText("Không có bàn trống");
                recyclerView.setVisibility(View.GONE);
            } else {
                emptyState.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            }

            tableAdapter.updateData(tableList);
        });
    }

    private void loadMyReservations() {
        if (currentUser == null) {
            Toast.makeText(getContext(), "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);

        if (reservationsListener != null) {
            reservationsListener.remove();
        }

        reservationsListener = FirebaseService.getInstance().listenReservationsByCustomer(
                currentUser.getUid(),
                (value, error) -> {
                    progressBar.setVisibility(View.GONE);

                    if (error != null) {
                        Toast.makeText(getContext(), "Lỗi: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    reservationList.clear();
                    if (value != null) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            ReservationModel reservation = doc.toObject(ReservationModel.class);
                            if (reservation != null) {
                                reservation.setId(doc.getId());
                                reservationList.add(reservation);
                            }
                        }
                    }

                    if (reservationList.isEmpty()) {
                        emptyState.setVisibility(View.VISIBLE);
                        tvEmptyMessage.setText("Bạn chưa có đặt chỗ nào");
                        recyclerView.setVisibility(View.GONE);
                    } else {
                        emptyState.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                    }

                    reservationAdapter.updateData(reservationList);
                });
    }

    private void showBookingDialog(TableModel table) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_booking, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.bg_card);
        }

        // Views
        TextView tvTableInfo = dialogView.findViewById(R.id.tvTableInfo);
        LinearLayout layoutDate = dialogView.findViewById(R.id.layoutDate);
        LinearLayout layoutTime = dialogView.findViewById(R.id.layoutTime);
        TextView tvDate = dialogView.findViewById(R.id.tvDate);
        TextView tvTime = dialogView.findViewById(R.id.tvTime);
        ImageButton btnMinus = dialogView.findViewById(R.id.btnMinus);
        ImageButton btnPlus = dialogView.findViewById(R.id.btnPlus);
        TextView tvGuestCount = dialogView.findViewById(R.id.tvGuestCount);
        EditText etNote = dialogView.findViewById(R.id.etNote);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);
        MaterialButton btnConfirm = dialogView.findViewById(R.id.btnConfirm);

        // Set table info
        tvTableInfo.setText(table.getName() + " - Sức chứa: " + table.getCapacity() + " người");

        // Guest count
        final int[] guestCount = {2};
        final int maxGuests = table.getCapacity();
        tvGuestCount.setText(String.valueOf(guestCount[0]));

        btnMinus.setOnClickListener(v -> {
            if (guestCount[0] > 1) {
                guestCount[0]--;
                tvGuestCount.setText(String.valueOf(guestCount[0]));
            }
        });

        btnPlus.setOnClickListener(v -> {
            if (guestCount[0] < maxGuests) {
                guestCount[0]++;
                tvGuestCount.setText(String.valueOf(guestCount[0]));
            } else {
                Toast.makeText(getContext(), "Bàn chỉ có sức chứa " + maxGuests + " người", Toast.LENGTH_SHORT).show();
            }
        });

        // Date picker
        final String[] selectedDate = {""};
        Calendar calendar = Calendar.getInstance();

        layoutDate.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    requireContext(),
                    (view, year, month, dayOfMonth) -> {
                        calendar.set(year, month, dayOfMonth);
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                        selectedDate[0] = sdf.format(calendar.getTime());

                        SimpleDateFormat displaySdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                        tvDate.setText(displaySdf.format(calendar.getTime()));
                        tvDate.setTextColor(getResources().getColor(R.color.black));
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            // Min date is today
            datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
            datePickerDialog.show();
        });

        // Time picker
        final String[] selectedTime = {""};

        layoutTime.setOnClickListener(v -> {
            TimePickerDialog timePickerDialog = new TimePickerDialog(
                    requireContext(),
                    (view, hourOfDay, minute) -> {
                        selectedTime[0] = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
                        tvTime.setText(selectedTime[0]);
                        tvTime.setTextColor(getResources().getColor(R.color.black));
                    },
                    18, 0, true
            );
            timePickerDialog.show();
        });

        // Cancel
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        // Confirm
        btnConfirm.setOnClickListener(v -> {
            if (selectedDate[0].isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng chọn ngày", Toast.LENGTH_SHORT).show();
                return;
            }
            if (selectedTime[0].isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng chọn giờ", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check availability and create reservation
            createReservation(table, selectedDate[0], selectedTime[0], guestCount[0], 
                    etNote.getText().toString().trim(), dialog);
        });

        dialog.show();
    }

    private void createReservation(TableModel table, String date, String time, int guests, String note, AlertDialog dialog) {
        if (currentUser == null) {
            Toast.makeText(getContext(), "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if table is available at that time
        FirebaseService.getInstance().checkTableAvailability(table.getId(), date, time,
                querySnapshot -> {
                    // Check for time conflicts (within 2 hours)
                    boolean hasConflict = false;
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String existingTime = doc.getString("time");
                        if (existingTime != null && isTimeConflict(time, existingTime)) {
                            hasConflict = true;
                            break;
                        }
                    }

                    if (hasConflict) {
                        Toast.makeText(getContext(), 
                                "Bàn đã được đặt vào thời gian này. Vui lòng chọn thời gian khác.", 
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    // Create reservation
                    Map<String, Object> reservationData = new HashMap<>();
                    reservationData.put("customerId", currentUser.getUid());
                    reservationData.put("customerName", currentUserName);
                    reservationData.put("tableId", table.getId());
                    reservationData.put("tableName", table.getName());
                    reservationData.put("date", date);
                    reservationData.put("time", time);
                    reservationData.put("guests", guests);
                    reservationData.put("note", note);
                    reservationData.put("status", "pending");
                    reservationData.put("createdAt", System.currentTimeMillis());

                    FirebaseService.getInstance().createReservation(reservationData,
                            documentReference -> {
                                Toast.makeText(getContext(), "Đặt bàn thành công!", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                                
                                // Switch to reservations tab
                                tabLayout.selectTab(tabLayout.getTabAt(1));
                            },
                            e -> {
                                Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                },
                e -> {
                    Toast.makeText(getContext(), "Lỗi kiểm tra: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private boolean isTimeConflict(String newTime, String existingTime) {
        try {
            String[] newParts = newTime.split(":");
            String[] existingParts = existingTime.split(":");
            
            int newMinutes = Integer.parseInt(newParts[0]) * 60 + Integer.parseInt(newParts[1]);
            int existingMinutes = Integer.parseInt(existingParts[0]) * 60 + Integer.parseInt(existingParts[1]);
            
            // Conflict if within 2 hours (120 minutes)
            return Math.abs(newMinutes - existingMinutes) < 120;
        } catch (Exception e) {
            return false;
        }
    }

    private void cancelReservation(ReservationModel reservation) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Xác nhận hủy")
                .setMessage("Bạn có chắc muốn hủy đặt bàn này?")
                .setPositiveButton("Hủy đặt", (dialog, which) -> {
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("status", "cancelled");

                    FirebaseService.getInstance().updateReservation(reservation.getId(), updates,
                            aVoid -> {
                                Toast.makeText(getContext(), "Đã hủy đặt bàn", Toast.LENGTH_SHORT).show();
                            },
                            e -> {
                                Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Không", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (tablesListener != null) {
            tablesListener.remove();
        }
        if (reservationsListener != null) {
            reservationsListener.remove();
        }
    }
}
