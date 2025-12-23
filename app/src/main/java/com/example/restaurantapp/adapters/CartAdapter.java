package com.example.restaurantapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.restaurantapp.R;

import java.util.List;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.ViewHolder> {

    public interface OnCartItemActionListener {
        void onRemoveItem(CartItem item);
        void onQuantityChanged(CartItem item, int newQuantity);
    }

    public static class CartItem {
        public String menuId;
        public String name;
        public double price;
        public int quantity;

        public CartItem(String menuId, String name, double price, int quantity) {
            this.menuId = menuId;
            this.name = name;
            this.price = price;
            this.quantity = quantity;
        }

        public double getTotal() {
            return price * quantity;
        }
    }

    private List<CartItem> items;
    private OnCartItemActionListener listener;

    public CartAdapter(List<CartItem> items, OnCartItemActionListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cart, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CartItem item = items.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void updateData(List<CartItem> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvItemName, tvUnitPrice, tvQuantity, tvItemTotal;
        ImageButton btnRemove, btnMinus, btnPlus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvItemName = itemView.findViewById(R.id.tvItemName);
            tvUnitPrice = itemView.findViewById(R.id.tvUnitPrice);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);
            tvItemTotal = itemView.findViewById(R.id.tvItemTotal);
            btnRemove = itemView.findViewById(R.id.btnRemove);
            btnMinus = itemView.findViewById(R.id.btnMinus);
            btnPlus = itemView.findViewById(R.id.btnPlus);
        }

        public void bind(CartItem item) {
            tvItemName.setText(item.name);
            tvUnitPrice.setText(String.format("%,.0fđ", item.price));
            tvQuantity.setText(String.valueOf(item.quantity));
            tvItemTotal.setText(String.format("%,.0fđ", item.getTotal()));

            btnRemove.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRemoveItem(item);
                }
            });

            btnMinus.setOnClickListener(v -> {
                if (listener != null) {
                    if (item.quantity > 1) {
                        listener.onQuantityChanged(item, item.quantity - 1);
                    } else {
                        listener.onRemoveItem(item);
                    }
                }
            });

            btnPlus.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onQuantityChanged(item, item.quantity + 1);
                }
            });
        }
    }
}
