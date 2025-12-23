package com.example.restaurantapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.restaurantapp.R;
import com.example.restaurantapp.models.MenuItem;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class MenuAdapter extends RecyclerView.Adapter<MenuAdapter.MenuViewHolder> {

    public interface OnMenuItemClickListener {
        void onAddToCart(MenuItem item);
    }

    private List<MenuItem> menuList;
    private OnMenuItemClickListener listener;

    public MenuAdapter(List<MenuItem> menuList, OnMenuItemClickListener listener) {
        this.menuList = menuList;
        this.listener = listener;
        setHasStableIds(true); // Cải thiện performance
    }
    
    @Override
    public long getItemId(int position) {
        // Trả về stable ID dựa trên ID của item
        MenuItem item = menuList.get(position);
        return item.getId() != null ? item.getId().hashCode() : position;
    }

    @NonNull
    @Override
    public MenuViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_menu, parent, false);
        return new MenuViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MenuViewHolder holder, int position) {
        MenuItem item = menuList.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return menuList.size();
    }

    class MenuViewHolder extends RecyclerView.ViewHolder {
        TextView txtName, txtPrice, txtCategory;
        MaterialButton btnAdd;
        ImageView imgFood;

        MenuViewHolder(@NonNull View itemView) {
            super(itemView);
            txtName = itemView.findViewById(R.id.txtMenuName);
            txtPrice = itemView.findViewById(R.id.txtMenuPrice);
            txtCategory = itemView.findViewById(R.id.txtMenuCategory);
            btnAdd = itemView.findViewById(R.id.btnAddToCart);
            imgFood = itemView.findViewById(R.id.imgFood);
        }

        void bind(MenuItem item) {
            txtName.setText(item.getName());
            txtPrice.setText(String.format("%,.0f đ", item.getPrice()));
            txtCategory.setText(item.getCategory());

            // Load ảnh với Glide + caching
            if (item.getImageUrl() != null && !item.getImageUrl().isEmpty() && imgFood != null) {
                Glide.with(itemView.getContext())
                        .load(item.getImageUrl())
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(R.drawable.ic_menu_food)
                        .error(R.drawable.ic_menu_food)
                        .centerCrop()
                        .into(imgFood);
            } else if (imgFood != null) {
                imgFood.setImageResource(R.drawable.ic_menu_food);
            }

            btnAdd.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAddToCart(item);
                }
            });
        }
    }
}
