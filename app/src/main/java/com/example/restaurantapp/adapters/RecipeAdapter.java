package com.example.restaurantapp.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.restaurantapp.R;
import com.example.restaurantapp.models.InventoryModel;
import com.example.restaurantapp.models.RecipeModel;

import java.util.List;

public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder> {

    public interface OnRecipeClickListener {
        void onRecipeClick(RecipeModel recipe);
    }

    private List<RecipeModel> recipes;
    private List<InventoryModel> inventoryList;
    private OnRecipeClickListener listener;
    private Context context;

    public RecipeAdapter(List<RecipeModel> recipes, List<InventoryModel> inventoryList, 
                         OnRecipeClickListener listener) {
        this.recipes = recipes;
        this.inventoryList = inventoryList;
        this.listener = listener;
    }

    public void setInventoryList(List<InventoryModel> inventoryList) {
        this.inventoryList = inventoryList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_recipe, parent, false);
        return new RecipeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
        RecipeModel recipe = recipes.get(position);
        
        holder.tvMenuItemName.setText(recipe.getMenuItemName());
        
        // Hiển thị số lượng nguyên liệu
        int ingredientCount = recipe.getIngredients() != null ? recipe.getIngredients().size() : 0;
        holder.tvIngredientCount.setText(ingredientCount + " nguyên liệu");
        
        // Kiểm tra trạng thái nguyên liệu
        if (inventoryList != null && !inventoryList.isEmpty()) {
            boolean hasEnough = recipe.hasEnoughIngredients(inventoryList);
            List<String> missing = recipe.getMissingIngredients(inventoryList);
            
            if (hasEnough) {
                holder.tvStatus.setText("✓ Đủ nguyên liệu");
                holder.tvStatus.setTextColor(context.getResources().getColor(android.R.color.holo_green_dark));
                holder.layoutMissing.setVisibility(View.GONE);
            } else {
                holder.tvStatus.setText("✗ Thiếu nguyên liệu");
                holder.tvStatus.setTextColor(context.getResources().getColor(android.R.color.holo_red_dark));
                
                // Hiển thị các nguyên liệu thiếu
                holder.layoutMissing.setVisibility(View.VISIBLE);
                StringBuilder missingText = new StringBuilder();
                for (int i = 0; i < missing.size(); i++) {
                    missingText.append("• ").append(missing.get(i));
                    if (i < missing.size() - 1) {
                        missingText.append("\n");
                    }
                }
                holder.tvMissingItems.setText(missingText.toString());
            }
        } else {
            holder.tvStatus.setText("Chưa xác định");
            holder.tvStatus.setTextColor(context.getResources().getColor(android.R.color.darker_gray));
            holder.layoutMissing.setVisibility(View.GONE);
        }

        // Hiển thị danh sách nguyên liệu tóm tắt
        if (recipe.getIngredients() != null && !recipe.getIngredients().isEmpty()) {
            StringBuilder ingredientsList = new StringBuilder();
            for (int i = 0; i < Math.min(3, recipe.getIngredients().size()); i++) {
                RecipeModel.RecipeIngredient ing = recipe.getIngredients().get(i);
                ingredientsList.append(ing.getIngredientName())
                        .append(" (").append(ing.getQuantityRequired())
                        .append(" ").append(ing.getUnit()).append(")");
                if (i < Math.min(3, recipe.getIngredients().size()) - 1) {
                    ingredientsList.append(", ");
                }
            }
            if (recipe.getIngredients().size() > 3) {
                ingredientsList.append("...");
            }
            holder.tvIngredientsList.setText(ingredientsList.toString());
            holder.tvIngredientsList.setVisibility(View.VISIBLE);
        } else {
            holder.tvIngredientsList.setText("Chưa có công thức");
            holder.tvIngredientsList.setVisibility(View.VISIBLE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRecipeClick(recipe);
            }
        });
    }

    @Override
    public int getItemCount() {
        return recipes != null ? recipes.size() : 0;
    }

    static class RecipeViewHolder extends RecyclerView.ViewHolder {
        TextView tvMenuItemName;
        TextView tvIngredientCount;
        TextView tvStatus;
        TextView tvIngredientsList;
        TextView tvMissingItems;
        LinearLayout layoutMissing;
        ImageView ivArrow;

        RecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMenuItemName = itemView.findViewById(R.id.tvMenuItemName);
            tvIngredientCount = itemView.findViewById(R.id.tvIngredientCount);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvIngredientsList = itemView.findViewById(R.id.tvIngredientsList);
            tvMissingItems = itemView.findViewById(R.id.tvMissingItems);
            layoutMissing = itemView.findViewById(R.id.layoutMissing);
            ivArrow = itemView.findViewById(R.id.ivArrow);
        }
    }
}
