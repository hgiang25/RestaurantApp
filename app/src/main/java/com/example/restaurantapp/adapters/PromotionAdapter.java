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
        TextView tvName, tvCode, tvDiscount, tvExpiry, tvStatus, tvMaxDiscount, tvMinOrder;
        View viewStatusBar;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvPromoName);
            tvCode = itemView.findViewById(R.id.tvPromoCode);
            tvDiscount = itemView.findViewById(R.id.tvDiscount);
            tvExpiry = itemView.findViewById(R.id.tvExpiry);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvMaxDiscount = itemView.findViewById(R.id.tvMaxDiscount);
            tvMinOrder = itemView.findViewById(R.id.tvMinOrder);
            viewStatusBar = itemView.findViewById(R.id.viewStatusBar);
        }

        void bind(PromotionModel promotion) {
            tvName.setText(promotion.getName());
            tvCode.setText(promotion.getCode());
            tvDiscount.setText(String.format(Locale.getDefault(), "-%.0f%%", promotion.getDiscountPercent()));

            if (promotion.getValidUntil() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                tvExpiry.setText(sdf.format(promotion.getValidUntil().toDate()));
            } else {
                tvExpiry.setText("Không giới hạn");
            }

            // Max discount
            if (tvMaxDiscount != null) {
                if (promotion.getMaxDiscount() > 0) {
                    tvMaxDiscount.setText(String.format(Locale.getDefault(), "%,.0fđ", promotion.getMaxDiscount()));
                } else {
                    tvMaxDiscount.setText("Không giới hạn");
                }
            }

            // Min order
            if (tvMinOrder != null) {
                if (promotion.getMinOrderAmount() > 0) {
                    tvMinOrder.setText(String.format(Locale.getDefault(), "%,.0fđ", promotion.getMinOrderAmount()));
                } else {
                    tvMinOrder.setText("Không yêu cầu");
                }
            }

            // Status styling
            boolean isActive = promotion.isActive();
            tvStatus.setText(isActive ? "✓ Đang hoạt động" : "✗ Đã tắt");
            tvStatus.setTextColor(isActive ? 0xFF4CAF50 : 0xFFF44336);
            tvStatus.setBackgroundColor(isActive ? 0x1A4CAF50 : 0x1AF44336);

            // Status bar color
            if (viewStatusBar != null) {
                viewStatusBar.setBackgroundColor(isActive ? 0xFF4CAF50 : 0xFFF44336);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPromotionClick(promotion);
                }
            });
        }
    }
}
