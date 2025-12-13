package com.example.restaurantapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.restaurantapp.R;
import com.example.restaurantapp.models.User;

import java.util.List;

public class StaffAdapter extends RecyclerView.Adapter<StaffAdapter.StaffViewHolder> {

    public interface OnStaffClickListener {
        void onStaffClick(User staff);
    }

    private List<User> staffList;
    private OnStaffClickListener listener;

    public StaffAdapter(List<User> staffList, OnStaffClickListener listener) {
        this.staffList = staffList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public StaffViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_staff, parent, false);
        return new StaffViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StaffViewHolder holder, int position) {
        User staff = staffList.get(position);
        holder.bind(staff);
    }

    @Override
    public int getItemCount() {
        return staffList.size();
    }

    class StaffViewHolder extends RecyclerView.ViewHolder {
        TextView txtName, txtEmail, txtRole;

        StaffViewHolder(@NonNull View itemView) {
            super(itemView);
            txtName = itemView.findViewById(R.id.txtStaffName);
            txtEmail = itemView.findViewById(R.id.txtStaffEmail);
            txtRole = itemView.findViewById(R.id.txtStaffRole);
        }

        void bind(User staff) {
            txtName.setText(staff.getUsername());
            txtEmail.setText(staff.getEmail());
            txtRole.setText(staff.getRole());

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onStaffClick(staff);
                }
            });
        }
    }
}
