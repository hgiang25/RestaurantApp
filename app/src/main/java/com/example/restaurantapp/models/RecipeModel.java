package com.example.restaurantapp.models;

import java.util.ArrayList;
import java.util.List;

public class RecipeModel {
    private String id;
    private String menuItemId;      // ID món ăn
    private String menuItemName;    // Tên món ăn
    private List<RecipeIngredient> ingredients;  // Danh sách nguyên liệu

    public RecipeModel() {
        ingredients = new ArrayList<>();
    }

    public RecipeModel(String menuItemId, String menuItemName, List<RecipeIngredient> ingredients) {
        this.menuItemId = menuItemId;
        this.menuItemName = menuItemName;
        this.ingredients = ingredients != null ? ingredients : new ArrayList<>();
    }

    // Getters và Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getMenuItemId() { return menuItemId; }
    public void setMenuItemId(String menuItemId) { this.menuItemId = menuItemId; }

    public String getMenuItemName() { return menuItemName; }
    public void setMenuItemName(String menuItemName) { this.menuItemName = menuItemName; }

    public List<RecipeIngredient> getIngredients() { return ingredients; }
    public void setIngredients(List<RecipeIngredient> ingredients) { 
        this.ingredients = ingredients != null ? ingredients : new ArrayList<>(); 
    }

    /**
     * Kiểm tra xem có đủ nguyên liệu trong kho để làm món này không
     * @param inventoryList danh sách nguyên liệu trong kho
     * @return true nếu đủ nguyên liệu, false nếu thiếu
     */
    public boolean hasEnoughIngredients(List<InventoryModel> inventoryList) {
        if (ingredients == null || ingredients.isEmpty()) {
            return true; // Món không có công thức thì mặc định là có thể làm
        }

        for (RecipeIngredient recipeIng : ingredients) {
            boolean found = false;
            for (InventoryModel inv : inventoryList) {
                if (inv.getId().equals(recipeIng.getIngredientId())) {
                    found = true;
                    if (inv.getQuantity() < recipeIng.getQuantityRequired()) {
                        return false; // Không đủ số lượng
                    }
                    break;
                }
            }
            if (!found) {
                return false; // Không tìm thấy nguyên liệu trong kho
            }
        }
        return true;
    }

    /**
     * Lấy danh sách các nguyên liệu đang thiếu
     * @param inventoryList danh sách nguyên liệu trong kho
     * @return danh sách tên nguyên liệu thiếu
     */
    public List<String> getMissingIngredients(List<InventoryModel> inventoryList) {
        List<String> missing = new ArrayList<>();
        
        if (ingredients == null || ingredients.isEmpty()) {
            return missing;
        }

        for (RecipeIngredient recipeIng : ingredients) {
            boolean found = false;
            for (InventoryModel inv : inventoryList) {
                if (inv.getId().equals(recipeIng.getIngredientId())) {
                    found = true;
                    if (inv.getQuantity() < recipeIng.getQuantityRequired()) {
                        missing.add(recipeIng.getIngredientName() + " (cần: " + 
                                recipeIng.getQuantityRequired() + ", có: " + inv.getQuantity() + ")");
                    }
                    break;
                }
            }
            if (!found) {
                missing.add(recipeIng.getIngredientName() + " (không có trong kho)");
            }
        }
        return missing;
    }

    /**
     * Inner class: Nguyên liệu trong công thức
     */
    public static class RecipeIngredient {
        private String ingredientId;     // ID nguyên liệu trong kho
        private String ingredientName;   // Tên nguyên liệu
        private double quantityRequired; // Số lượng cần dùng
        private String unit;             // Đơn vị

        public RecipeIngredient() {}

        public RecipeIngredient(String ingredientId, String ingredientName, 
                                double quantityRequired, String unit) {
            this.ingredientId = ingredientId;
            this.ingredientName = ingredientName;
            this.quantityRequired = quantityRequired;
            this.unit = unit;
        }

        public String getIngredientId() { return ingredientId; }
        public void setIngredientId(String ingredientId) { this.ingredientId = ingredientId; }

        public String getIngredientName() { return ingredientName; }
        public void setIngredientName(String ingredientName) { this.ingredientName = ingredientName; }

        public double getQuantityRequired() { return quantityRequired; }
        public void setQuantityRequired(double quantityRequired) { this.quantityRequired = quantityRequired; }

        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }
    }
}
