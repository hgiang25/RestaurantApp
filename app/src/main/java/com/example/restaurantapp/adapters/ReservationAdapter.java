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

public class ReservationAdapter extends RecyclerView.Adapter<ReservationAdapter.ViewHolder> {

    public interface OnReservationActionListener {
        void onCancelClick(ReservationModel reservation);
    }

    private List<ReservationModel> reservations;
    private OnReservationActionListener listener;

    public ReservationAdapter(List<ReservationModel> reservations, OnReservationActionListener listener) {
        this.reservations = reservations;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_reservation, parent, false);
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
        TextView tvTableName, tvStatus, tvDateTime, tvGuests, tvNote;
        MaterialButton btnCancel;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            statusIndicator = itemView.findViewById(R.id.statusIndicator);
            tvTableName = itemView.findViewById(R.id.tvTableName);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
            tvGuests = itemView.findViewById(R.id.tvGuests);
            tvNote = itemView.findViewById(R.id.tvNote);
            btnCancel = itemView.findViewById(R.id.btnCancel);
        }

        public void bind(ReservationModel reservation) {
            tvTableName.setText(reservation.getTableName());
            tvDateTime.setText("📅 " + formatDate(reservation.getDate()) + " - " + reservation.getTime());
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

            int statusColor, statusBgColor, indicatorColor;
            switch (status) {
                case "confirmed":
                    statusColor = R.color.status_confirmed;
                    statusBgColor = R.drawable.bg_status_confirmed;
                    indicatorColor = R.color.status_confirmed;
                    break;
                case "cancelled":
                    statusColor = R.color.status_cancelled;
                    statusBgColor = R.drawable.bg_status_cancelled;
                    indicatorColor = R.color.status_cancelled;
                    break;
                case "completed":
                    statusColor = R.color.status_free;
                    statusBgColor = R.drawable.bg_status_free;
                    indicatorColor = R.color.status_free;
                    break;
                default: // pending
                    statusColor = R.color.status_pending;
                    statusBgColor = R.drawable.bg_status_pending;
                    indicatorColor = R.color.status_pending;
            }

            tvStatus.setBackgroundResource(statusBgColor);
            statusIndicator.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), indicatorColor));

            // Cancel button - only show for pending or confirmed
            if ("pending".equals(status) || "confirmed".equals(status)) {
                btnCancel.setVisibility(View.VISIBLE);
                btnCancel.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onCancelClick(reservation);
                    }
                });
            } else {
                btnCancel.setVisibility(View.GONE);
            }
        }

        private String formatDate(String date) {
            // Convert yyyy-MM-dd to dd/MM/yyyy
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
