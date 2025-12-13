package com.example.restaurantapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.restaurantapp.R;
import com.example.restaurantapp.models.PromotionModel;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class PromotionAdapter extends RecyclerView.Adapter<PromotionAdapter.ViewHolder> {

    private List<PromotionModel> promotionList;
    private OnPromotionClickListener listener;

    public interface OnPromotionClickListener {
        void onPromotionClick(PromotionModel promotion);
    }

    public PromotionAdapter(List<PromotionModel> promotionList, OnPromotionClickListener listener) {
        this.promotionList = promotionList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_promotion, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PromotionModel promotion = promotionList.get(position);
        holder.bind(promotion);
    }

    @Override
    public int getItemCount() {
        return promotionList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvCode, tvDiscount, tvExpiry, tvStatus;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvPromoName);
            tvCode = itemView.findViewById(R.id.tvPromoCode);
            tvDiscount = itemView.findViewById(R.id.tvDiscount);
            tvExpiry = itemView.findViewById(R.id.tvExpiry);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }

        void bind(PromotionModel promotion) {
            tvName.setText(promotion.getName());
            tvCode.setText(promotion.getCode());
            tvDiscount.setText(String.format("Giảm %.0f%%", promotion.getDiscountPercent()));

            if (promotion.getValidUntil() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                tvExpiry.setText("HSD: " + sdf.format(promotion.getValidUntil().toDate()));
            }

            tvStatus.setText(promotion.isActive() ? "Đang hoạt động" : "Đã hết hạn");
            tvStatus.setTextColor(promotion.isActive() ? 0xFF4CAF50 : 0xFFF44336);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPromotionClick(promotion);
                }
            });
        }
    }
}
