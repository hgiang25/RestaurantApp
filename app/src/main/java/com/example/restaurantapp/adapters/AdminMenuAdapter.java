package com.example.restaurantapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.restaurantapp.R;
import com.example.restaurantapp.models.MenuItem;

import java.util.List;

public class AdminMenuAdapter extends RecyclerView.Adapter<AdminMenuAdapter.AdminMenuViewHolder> {

    public interface OnMenuItemClickListener {
        void onMenuItemClick(MenuItem item);
    }

    private List<MenuItem> menuList;
    private OnMenuItemClickListener listener;

    public AdminMenuAdapter(List<MenuItem> menuList, OnMenuItemClickListener listener) {
        this.menuList = menuList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AdminMenuViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_menu, parent, false);
        return new AdminMenuViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AdminMenuViewHolder holder, int position) {
        MenuItem item = menuList.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return menuList.size();
    }

    class AdminMenuViewHolder extends RecyclerView.ViewHolder {
        TextView txtName, txtPrice, txtCategory, txtStatus;
        ImageView imgFood;

        AdminMenuViewHolder(@NonNull View itemView) {
            super(itemView);
            txtName = itemView.findViewById(R.id.txtMenuName);
            txtPrice = itemView.findViewById(R.id.txtMenuPrice);
            txtCategory = itemView.findViewById(R.id.txtMenuCategory);
            txtStatus = itemView.findViewById(R.id.txtMenuStatus);
            imgFood = itemView.findViewById(R.id.imgFood);
        }

        void bind(MenuItem item) {
            txtName.setText(item.getName());
            txtPrice.setText(String.format("%,.0f đ", item.getPrice()));
            txtCategory.setText(item.getCategory());

            if (item.isAvailable()) {
                txtStatus.setText("Có sẵn");
                txtStatus.setTextColor(0xFF4CAF50);
            } else {
                txtStatus.setText("Hết món");
                txtStatus.setTextColor(0xFFF44336);
            }

            // Load ảnh với Glide
            String imageUrl = item.getImageUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_restaurant)
                        .error(R.drawable.ic_close)
                        .into(imgFood);
            } else {
                imgFood.setImageResource(R.drawable.ic_menu_food);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onMenuItemClick(item);
                }
            });
        }

    }
}
