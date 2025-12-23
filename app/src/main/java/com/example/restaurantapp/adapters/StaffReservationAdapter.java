package com.example.restaurantapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.restaurantapp.R;
import com.example.restaurantapp.models.ReservationModel;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class StaffReservationAdapter extends RecyclerView.Adapter<StaffReservationAdapter.ViewHolder> {

    public interface OnReservationActionListener {
        void onConfirmClick(ReservationModel reservation);
        void onRejectClick(ReservationModel reservation);
        void onCompleteClick(ReservationModel reservation);
    }

    private List<ReservationModel> reservations;
    private OnReservationActionListener listener;

    public StaffReservationAdapter(List<ReservationModel> reservations, OnReservationActionListener listener) {
        this.reservations = reservations;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_staff_reservation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ReservationModel reservation = reservations.get(position);
        holder.bind(reservation);
    }

    @Override
    public int getItemCount() {
        return reservations.size();
    }

    public void updateData(List<ReservationModel> newReservations) {
        this.reservations = newReservations;
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        View statusIndicator;
        TextView tvCustomerName, tvTableName, tvStatus, tvDateTime, tvGuests, tvNote;
        MaterialButton btnConfirm, btnReject, btnComplete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            statusIndicator = itemView.findViewById(R.id.statusIndicator);
            tvCustomerName = itemView.findViewById(R.id.tvCustomerName);
            tvTableName = itemView.findViewById(R.id.tvTableName);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
            tvGuests = itemView.findViewById(R.id.tvGuests);
            tvNote = itemView.findViewById(R.id.tvNote);
            btnConfirm = itemView.findViewById(R.id.btnConfirm);
            btnReject = itemView.findViewById(R.id.btnReject);
            btnComplete = itemView.findViewById(R.id.btnComplete);
        }

        public void bind(ReservationModel reservation) {
            // Customer name
            tvCustomerName.setText(reservation.getCustomerName() != null ? 
                    reservation.getCustomerName() : "Khách hàng");
            
            // Table name
            tvTableName.setText("🪑 " + reservation.getTableName());
            
            // Date time
            tvDateTime.setText("📅 " + formatDate(reservation.getDate()) + " - ⏰ " + reservation.getTime());
            
            // Guests
            tvGuests.setText("👥 " + reservation.getGuests() + " khách");

            // Note
            if (reservation.getNote() != null && !reservation.getNote().isEmpty()) {
                tvNote.setText("📝 " + reservation.getNote());
                tvNote.setVisibility(View.VISIBLE);
            } else {
                tvNote.setVisibility(View.GONE);
            }

            // Status styling
            String status = reservation.getStatus();
            tvStatus.setText(reservation.getStatusDisplay());

            int indicatorColor;
            int statusBgColor;
            
            switch (status) {
                case "confirmed":
                    indicatorColor = R.color.status_confirmed;
                    statusBgColor = R.drawable.bg_status_confirmed;
                    break;
                case "cancelled":
                    indicatorColor = R.color.status_cancelled;
                    statusBgColor = R.drawable.bg_status_cancelled;
                    break;
                case "completed":
                    indicatorColor = R.color.status_free;
                    statusBgColor = R.drawable.bg_status_free;
                    break;
                default: // pending
                    indicatorColor = R.color.status_pending;
                    statusBgColor = R.drawable.bg_status_pending;
            }

            tvStatus.setBackgroundResource(statusBgColor);
            statusIndicator.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), indicatorColor));

            // Button visibility based on status
            btnConfirm.setVisibility(View.GONE);
            btnReject.setVisibility(View.GONE);
            btnComplete.setVisibility(View.GONE);

            switch (status) {
                case "pending":
                    // Show confirm and reject buttons
                    btnConfirm.setVisibility(View.VISIBLE);
                    btnReject.setVisibility(View.VISIBLE);
                    break;
                case "confirmed":
                    // Show complete button
                    btnComplete.setVisibility(View.VISIBLE);
                    break;
                // cancelled and completed: no buttons
            }

            // Button click listeners
            btnConfirm.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onConfirmClick(reservation);
                }
            });

            btnReject.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRejectClick(reservation);
                }
            });

            btnComplete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCompleteClick(reservation);
                }
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
    }
}
