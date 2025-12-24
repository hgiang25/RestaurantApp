package com.example.restaurantapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.restaurantapp.R;
import com.example.restaurantapp.models.RecipeModel;

import java.util.List;

public class RecipeIngredientAdapter extends RecyclerView.Adapter<RecipeIngredientAdapter.ViewHolder> {

    public interface OnRemoveListener {
        void onRemove(int position);
    }

    private List<RecipeModel.RecipeIngredient> ingredients;
    private OnRemoveListener removeListener;

    public RecipeIngredientAdapter(List<RecipeModel.RecipeIngredient> ingredients, 
                                   OnRemoveListener removeListener) {
        this.ingredients = ingredients;
        this.removeListener = removeListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recipe_ingredient, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RecipeModel.RecipeIngredient ingredient = ingredients.get(position);
        
        holder.tvIngredientName.setText(ingredient.getIngredientName());
        holder.tvQuantity.setText("Cần: " + ingredient.getQuantityRequired() + " " + ingredient.getUnit());
        
        holder.btnRemove.setOnClickListener(v -> {
            if (removeListener != null) {
                removeListener.onRemove(holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return ingredients != null ? ingredients.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvIngredientName;
        TextView tvQuantity;
        ImageView btnRemove;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvIngredientName = itemView.findViewById(R.id.tvIngredientName);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);
            btnRemove = itemView.findViewById(R.id.btnRemove);
        }
    }
}
