package com.example.restaurantapp.fragments.staff;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.restaurantapp.R;
import com.example.restaurantapp.adapters.AttendanceAdapter;
import com.example.restaurantapp.api.FirebaseService;
import com.example.restaurantapp.models.AttendanceModel;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StaffTimesheetFragment extends Fragment {

    private MaterialButton btnCheckIn, btnCheckOut;
    private TextView txtCurrentShift, txtCurrentDate;
    private RecyclerView recyclerAttendance;
    private AttendanceAdapter adapter;
    private List<AttendanceModel> attendanceList = new ArrayList<>();
    private String currentAttendanceId = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_staff_timesheet, container, false);

        btnCheckIn = view.findViewById(R.id.btnCheckIn);
        btnCheckOut = view.findViewById(R.id.btnCheckOut);
        txtCurrentShift = view.findViewById(R.id.txtCurrentShift);
        txtCurrentDate = view.findViewById(R.id.txtCurrentDate);
        recyclerAttendance = view.findViewById(R.id.recyclerAttendance);

        recyclerAttendance.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AttendanceAdapter(attendanceList);
        recyclerAttendance.setAdapter(adapter);

        // Hiển thị ngày hiện tại
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd/MM/yyyy", new Locale("vi", "VN"));
        txtCurrentDate.setText(sdf.format(new Date()));

        btnCheckIn.setOnClickListener(v -> handleCheckIn());
        btnCheckOut.setOnClickListener(v -> handleCheckOut());

        loadAttendance();

        return view;
    }

    private void loadAttendance() {
        String staffId = FirebaseService.getInstance().getCurrentUserId();
        if (staffId == null) return;

        FirebaseService.getInstance().listenAttendanceByStaffRealtime(staffId, (value, error) -> {
            if (!isAdded() || getContext() == null) return;
            if (error != null || value == null) return;

            attendanceList.clear();
            currentAttendanceId = null;

            for (DocumentSnapshot doc : value.getDocuments()) {
                AttendanceModel attendance = doc.toObject(AttendanceModel.class);
                if (attendance != null) {
                    attendance.setId(doc.getId());
                    attendanceList.add(attendance);

                    // Kiểm tra xem có ca làm chưa checkout không
                    if (attendance.getCheckOut() == null) {
                        currentAttendanceId = doc.getId();
                        if (txtCurrentShift != null) {
                            txtCurrentShift.setText("Ca hiện tại: " + attendance.getShift());
                        }
                        if (btnCheckIn != null) btnCheckIn.setEnabled(false);
                        if (btnCheckOut != null) btnCheckOut.setEnabled(true);
                    }
                }
            }

            if (currentAttendanceId == null) {
                if (txtCurrentShift != null) txtCurrentShift.setText("Chưa check-in");
                if (btnCheckIn != null) btnCheckIn.setEnabled(true);
                if (btnCheckOut != null) btnCheckOut.setEnabled(false);
            }

            // Sắp xếp theo thời gian
            attendanceList.sort((a1, a2) -> {
                if (a1.getCheckIn() == null || a2.getCheckIn() == null) return 0;
                return a2.getCheckIn().compareTo(a1.getCheckIn());
            });

            if (adapter != null) adapter.notifyDataSetChanged();
        });
    }

    private void handleCheckIn() {
        String staffId = FirebaseService.getInstance().getCurrentUserId();
        if (staffId == null || !isAdded() || getContext() == null) return;

        // Xác định ca làm dựa trên giờ hiện tại
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        String shift;
        if (hour < 12) {
            shift = "Sáng";
        } else if (hour < 18) {
            shift = "Chiều";
        } else {
            shift = "Tối";
        }

        FirebaseService.getInstance().addAttendance(staffId, shift, Timestamp.now(),
                docRef -> {
                    if (isAdded() && getContext() != null) {
                        Toast.makeText(getContext(), "Check-in thành công!", Toast.LENGTH_SHORT).show();
                    }
                },
                e -> {
                    if (isAdded() && getContext() != null) {
                        Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void handleCheckOut() {
        if (currentAttendanceId == null || !isAdded() || getContext() == null) return;

        FirebaseService.getInstance().updateAttendanceCheckOut(currentAttendanceId, Timestamp.now(),
                unused -> {
                    if (isAdded() && getContext() != null) {
                        Toast.makeText(getContext(), "Check-out thành công!", Toast.LENGTH_SHORT).show();
                    }
                },
                e -> {
                    if (isAdded() && getContext() != null) {
                        Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
