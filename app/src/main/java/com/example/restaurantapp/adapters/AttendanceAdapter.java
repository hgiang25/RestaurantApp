package com.example.restaurantapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.restaurantapp.R;
import com.example.restaurantapp.models.AttendanceModel;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class AttendanceAdapter extends RecyclerView.Adapter<AttendanceAdapter.AttendanceViewHolder> {

    private List<AttendanceModel> attendanceList;

    public AttendanceAdapter(List<AttendanceModel> attendanceList) {
        this.attendanceList = attendanceList;
    }

    @NonNull
    @Override
    public AttendanceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_attendance, parent, false);
        return new AttendanceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AttendanceViewHolder holder, int position) {
        AttendanceModel attendance = attendanceList.get(position);
        holder.bind(attendance);
    }

    @Override
    public int getItemCount() {
        return attendanceList.size();
    }

    class AttendanceViewHolder extends RecyclerView.ViewHolder {
        TextView txtDate, txtShift, txtCheckIn, txtCheckOut, txtDuration;

        AttendanceViewHolder(@NonNull View itemView) {
            super(itemView);
            txtDate = itemView.findViewById(R.id.txtDate);
            txtShift = itemView.findViewById(R.id.txtShift);
            txtCheckIn = itemView.findViewById(R.id.txtCheckIn);
            txtCheckOut = itemView.findViewById(R.id.txtCheckOut);
            txtDuration = itemView.findViewById(R.id.txtDuration);
        }

        void bind(AttendanceModel attendance) {
            SimpleDateFormat dateFmt = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", Locale.getDefault());

            if (attendance.getCheckIn() != null) {
                txtDate.setText(dateFmt.format(attendance.getCheckIn().toDate()));
                txtCheckIn.setText("Vào: " + timeFmt.format(attendance.getCheckIn().toDate()));
            }

            txtShift.setText("Ca " + attendance.getShift());

            if (attendance.getCheckOut() != null) {
                txtCheckOut.setText("Ra: " + timeFmt.format(attendance.getCheckOut().toDate()));

                // Tính thời gian làm việc
                long duration = attendance.getCheckOut().getSeconds() - attendance.getCheckIn().getSeconds();
                long hours = duration / 3600;
                long minutes = (duration % 3600) / 60;
                txtDuration.setText(String.format(Locale.getDefault(), "%dh %dm", hours, minutes));
            } else {
                txtCheckOut.setText("Ra: --:--");
                txtDuration.setText("Đang làm...");
                txtDuration.setTextColor(0xFF4CAF50);
            }
        }
    }
}
