package com.example.restaurantapp.fragments.admin;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.restaurantapp.R;
import com.example.restaurantapp.adapters.RecipeAdapter;
import com.example.restaurantapp.adapters.RecipeIngredientAdapter;
import com.example.restaurantapp.api.FirebaseService;
import com.example.restaurantapp.models.InventoryModel;
import com.example.restaurantapp.models.MenuItem;
import com.example.restaurantapp.models.RecipeModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminRecipeFragment extends Fragment {

    private RecyclerView recyclerRecipes;
    private RecipeAdapter adapter;
    private List<RecipeModel> recipeList = new ArrayList<>();
    private List<InventoryModel> inventoryList = new ArrayList<>();
    private List<MenuItem> menuList = new ArrayList<>();
    
    private TextView tvTotalRecipes, tvAvailableCount, tvMissingCount;
    private LinearLayout layoutEmpty;
    private MaterialButton btnSyncStatus;
    private ExtendedFloatingActionButton fabAddRecipe;

    private ListenerRegistration recipeListener;
    private ListenerRegistration inventoryListener;
    private ListenerRegistration menuListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_recipe, container, false);

        initViews(view);
        setupRecyclerView();
        loadData();
//        addSampleRecipes();

        fabAddRecipe.setOnClickListener(v -> showAddRecipeDialog(null));
        btnSyncStatus.setOnClickListener(v -> syncMenuItemsStatus());

        return view;
    }

    private void addSampleRecipes() {
        // Danh sách món ăn (chỉ những món ăn thực tế, không phải nước/kem)
        String[][] menuItems = {
                {"Cơm trộn Hàn Quốc", "2uDgaMbeimTB1OKAnRRE"},
                {"Bò lúc lắc", "3uGAvzPbhQkHSf49lowq"},
                {"Mì xào", "4cS2rYV69GmxNPWDpnp6"},
                {"Khoai tây chiên", "4j56txF2h3a43QTAujtB"},
                {"Cơm gà", "6cKQpy8vnXedAgr5ngbI"},
                {"Bún chả", "7o1xrbtwXYJTCBvMuR6B"},
                {"Cá chiên xốt chua ngọt", "9uOKJJYRJsX0sLI1bImm"},
                {"Hamburger", "Gbm5TcwwVK2M14kaS3vf"},
                {"Tôm rang muối", "J3PICPiK7aYH3HUeEIsr"},
                {"Phở bò", "JESoq7Zpk5X7jt9sZBrD"},
                {"Gỏi cuốn", "K4xqjsPdfycrewBh9zrb"},
                {"Nem rán", "Nmudr2jaR9We5d8c86HQ"},
                {"Salad trộn", "U6HLmrAyRoLx2mp1bDYz"},
                {"Mỳ ống sốt cà chua", "Uv2YDR4veBZNC9DVOSD9"},
                {"Chè thập cẩm", "V6Kvl1FU4vwY7PTmJJKu"},
                {"Bánh flan", "b9cZUGqFljqYDoNl7xlI"},
                {"Hotdog", "eel8LjsXb9yw4qyuNbgS"},
                {"Bánh xèo", "fuMe3ppGXSf4CGTCG0oI"},
                {"Xôi xoài", "fxGH7XIqCbEj9PudQCUn"},
                {"Snack khoai tây", "hICMXI2cyQv0KKtJdAUD"},
                {"Pizza mini", "iU5i2pf4tEMj3G4zMnzy"},
                {"Bánh mì kẹp thịt", "kOY0ayu8tLhfTvqlYcdi"},
                {"Sườn xào chua ngọt", "l391N5xwPTIQYX8X1S59"},
                {"Súp cua", "mxmBsWXRPGLRAe9oAJmR"},
                {"Cà phê đen", "nxspOrGOQuS3X4eqPcKZ"},
                {"Xúc xích", "oMUpqx2hzkPK6NvclDFQ"},
                {"Bánh bao", "qjhmPUT4Cv3p938gVaRK"},
                {"Bánh crepe", "rRVanUvzEqt92m4dLg9Z"},
                {"Bánh trứng", "s2CbiVWgsmxrGEg6srJC"},
                {"Bắp rang bơ", "t2BvVzJMKq0mqrfzM1mi"},
                {"Cà phê sữa", "usjRHPghrslGSDL8undz"},
                {"Bánh ngọt", "vRl9LPkTXHBairQLb6q"},
                {"Chả giò rế", "vuMPvc5Y3VsicOK3uh7u"},
                {"Bánh bông lan", "xfIWmp828BtU4gh9POMT"},
                {"Gà nướng mật ong", "yJwc1tQZWLfmPQsYTg5u"},
                {"Xôi gà", "yvNSE86iMghp10m3drbF"}
        };

        // Danh sách nguyên liệu mẫu
        String[][] ingredients = {
                {"Gạo", "2h9qOPxsVEVw3DvOlaoj"},
                {"Hành tím", "4XlgEeIrB44hl9RTBVqW"},
                {"Bún", "77VmEzigmPjqV9Wl1fC3"},
                {"Dầu ăn", "8nptQZIakdHAFjBac87q"},
                {"Mực", "8tdRDA4vt7jTjugAQpFg"},
                {"Muối", "BaJ972ycM2N7LwHN2R3g"},
                {"Mì ống", "CsgVPbkAN7XbqmHqJ2qp"},
                {"Trứng gà", "DWHNEztDxUu1Jqm0k8sF"},
                {"Bột năng", "DYB8tEXfVSWut2eRrD2Z"},
                {"Sữa tươi", "ELCaUZ2u3e84nZBcmfP7"},
                {"Nước mắm", "FKRXdznAJmQqfCMhzYQx"},
                {"Cà chua", "M1aiDtbqVXSaoTE7tFfq"},
                {"Rau muống", "OYn0Ksv2WZdCdrRdbFlb"},
                {"Ớt đỏ", "P5X9e2v1831ZkYdTWI8d"},
                {"Miến", "PvILiMrKjOfp2TzAXZZV"},
                {"Carrot", "SUUV87w7qqrFHZxU4ao2"},
                {"Thịt heo", "SlqfQJvqAJCoBkDMaRoR"},
                {"Tiêu", "T0QEofTNfXPmqMcXDB1K"},
                {"Bánh ngọt", "bL2R7EkfVaKs0k8zY2Na"},
                {"Tôm sú", "ck9eMZJa95rnDYJtFoWg"},
                {"Thịt bò", "cpllcwYr0x9b1uJNwazd"},
                {"Rau sống", "cqtpZFLgWOq2XswcRT4D"},
                {"Bột mì", "fIhtapMAeLw4nCObDHL3"},
                {"Đường", "hgKTOlGLNQCkOGVZRrr9"},
                {"Dưa leo", "jsLLmtd2Y9whVFyt3M9j"},
                {"Thịt gà", "keBvYgh2ed07WpWc9KIn"},
                {"Gà nguyên con", "oH9X9cKqbXIrwQMJTNrY"},
                {"Bánh mì", "pNLYilmttVxjSvbVGJmL"},
                {"Tỏi", "phbV5IkZ6kgV9XKydnKo"},
                {"Phô mai", "tdIx54pTMugCMT7KIfJP"},
                {"Cá hồi", "uapjCXk4IJYC4AqidT8G"}
        };

        // Tạo công thức cho mỗi món ăn
        for (String[] menu : menuItems) {
            String menuName = menu[0];
            String menuId = menu[1];

            // Chọn 3-5 nguyên liệu ngẫu nhiên từ list nguyên liệu
            List<Map<String, Object>> recipeIngredients = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                String[] ing = ingredients[i]; // đơn giản chọn 3 nguyên liệu đầu (có thể random)
                Map<String, Object> ingMap = new HashMap<>();
                ingMap.put("ingredientId", ing[1]);
                ingMap.put("ingredientName", ing[0]);
                ingMap.put("quantityRequired", 1.0 + i); // quantity tùy chỉnh
                ingMap.put("unit", "kg"); // unit tùy chỉnh
                recipeIngredients.add(ingMap);
            }

            Map<String, Object> recipeData = new HashMap<>();
            recipeData.put("menuItemId", menuId);
            recipeData.put("menuItemName", menuName);
            recipeData.put("ingredients", recipeIngredients);

            FirebaseService.getInstance().getDb()
                    .collection("recipes")
                    .add(recipeData)
                    .addOnSuccessListener(ref ->
                            Log.d("AddRecipe", "Added recipe for " + menuName))
                    .addOnFailureListener(e ->
                            Log.e("AddRecipe", "Error adding recipe for " + menuName + ": " + e.getMessage()));
        }

        Toast.makeText(getContext(), "Đã gửi yêu cầu thêm công thức mẫu", Toast.LENGTH_SHORT).show();
    }


    private void initViews(View view) {
        recyclerRecipes = view.findViewById(R.id.recyclerRecipes);
        tvTotalRecipes = view.findViewById(R.id.tvTotalRecipes);
        tvAvailableCount = view.findViewById(R.id.tvAvailableCount);
        tvMissingCount = view.findViewById(R.id.tvMissingCount);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);
        btnSyncStatus = view.findViewById(R.id.btnSyncStatus);
        fabAddRecipe = view.findViewById(R.id.fabAddRecipe);
    }

    private void setupRecyclerView() {
        recyclerRecipes.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new RecipeAdapter(recipeList, inventoryList, this::showRecipeDetailDialog);
        recyclerRecipes.setAdapter(adapter);
    }

    private void loadData() {
        // Load menu items
        menuListener = FirebaseService.getInstance().listenMenuRealtime((value, error) -> {
            if (!isAdded() || getContext() == null) return;
            if (error != null || value == null) return;

            menuList.clear();
            for (DocumentSnapshot doc : value.getDocuments()) {
                MenuItem item = doc.toObject(MenuItem.class);
                if (item != null) {
                    item.setId(doc.getId());
                    menuList.add(item);
                }
            }
        });

        // Load inventory
        inventoryListener = FirebaseService.getInstance().getDb()
                .collection("inventory")
                .addSnapshotListener((value, error) -> {
                    if (!isAdded() || getContext() == null) return;
                    if (error != null || value == null) return;

                    inventoryList.clear();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        InventoryModel item = doc.toObject(InventoryModel.class);
                        if (item != null) {
                            item.setId(doc.getId());
                            inventoryList.add(item);
                        }
                    }
                    
                    // Update adapter với inventory mới
                    if (adapter != null) {
                        adapter.setInventoryList(inventoryList);
                    }
                    updateSummary();
                });

        // Load recipes
        recipeListener = FirebaseService.getInstance().getDb()
                .collection("recipes")
                .addSnapshotListener((value, error) -> {
                    if (!isAdded() || getContext() == null) return;
                    if (error != null || value == null) return;

                    recipeList.clear();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        RecipeModel recipe = doc.toObject(RecipeModel.class);
                        if (recipe != null) {
                            recipe.setId(doc.getId());
                            recipeList.add(recipe);
                        }
                    }
                    
                    if (adapter != null) adapter.notifyDataSetChanged();
                    updateUI();
                    updateSummary();
                });
    }

    private void updateUI() {
        if (recipeList.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            recyclerRecipes.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            recyclerRecipes.setVisibility(View.VISIBLE);
        }
    }

    private void updateSummary() {
        tvTotalRecipes.setText(String.valueOf(recipeList.size()));
        
        int available = 0;
        int missing = 0;
        
        for (RecipeModel recipe : recipeList) {
            if (recipe.hasEnoughIngredients(inventoryList)) {
                available++;
            } else {
                missing++;
            }
        }
        
        tvAvailableCount.setText(String.valueOf(available));
        tvMissingCount.setText(String.valueOf(missing));
    }

    /**
     * Đồng bộ trạng thái món ăn dựa trên nguyên liệu trong kho
     */
    private void syncMenuItemsStatus() {
        if (getContext() == null || !isAdded()) return;

        new AlertDialog.Builder(getContext())
                .setTitle("Cập nhật trạng thái món ăn")
                .setMessage("Hệ thống sẽ tự động:\n\n" +
                        "✓ Bật các món có đủ nguyên liệu\n" +
                        "✗ Tắt các món thiếu nguyên liệu\n\n" +
                        "Bạn có muốn tiếp tục?")
                .setPositiveButton("Cập nhật", (dialog, which) -> performSync())
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void performSync() {
        if (recipeList.isEmpty()) {
            Toast.makeText(getContext(), "Chưa có công thức nào để đồng bộ", Toast.LENGTH_SHORT).show();
            return;
        }

        int updatedCount = 0;
        int enabledCount = 0;
        int disabledCount = 0;

        for (RecipeModel recipe : recipeList) {
            // Tìm menu item tương ứng
            MenuItem menuItem = null;
            for (MenuItem item : menuList) {
                if (item.getId().equals(recipe.getMenuItemId())) {
                    menuItem = item;
                    break;
                }
            }

            if (menuItem == null) continue;

            boolean hasEnough = recipe.hasEnoughIngredients(inventoryList);
            
            // Chỉ cập nhật nếu trạng thái khác
            if (menuItem.isAvailable() != hasEnough) {
                updatedCount++;
                if (hasEnough) {
                    enabledCount++;
                } else {
                    disabledCount++;
                }
                
                Map<String, Object> updates = new HashMap<>();
                updates.put("available", hasEnough);
                
                FirebaseService.getInstance().updateMenuItem(recipe.getMenuItemId(), updates,
                        unused -> {},
                        e -> {});
            }
        }

        String message;
        if (updatedCount == 0) {
            message = "Tất cả món ăn đã ở trạng thái đúng!";
        } else {
            message = "Đã cập nhật " + updatedCount + " món:\n" +
                    "• " + enabledCount + " món được bật\n" +
                    "• " + disabledCount + " món được tắt";
        }

        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
    }

    private void showAddRecipeDialog(@Nullable RecipeModel existingRecipe) {
        if (getContext() == null || !isAdded()) return;

        View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_add_recipe, null);

        TextView tvDialogTitle = dialogView.findViewById(R.id.tvDialogTitle);
        AutoCompleteTextView spinnerMenuItem = dialogView.findViewById(R.id.spinnerMenuItem);
        RecyclerView recyclerIngredients = dialogView.findViewById(R.id.recyclerIngredients);
        TextView tvNoIngredients = dialogView.findViewById(R.id.tvNoIngredients);
        AutoCompleteTextView spinnerIngredient = dialogView.findViewById(R.id.spinnerIngredient);
        TextInputEditText edtQuantityRequired = dialogView.findViewById(R.id.edtQuantityRequired);
        TextView tvUnit = dialogView.findViewById(R.id.tvUnit);
        MaterialButton btnAddIngredient = dialogView.findViewById(R.id.btnAddIngredient);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);
        MaterialButton btnSave = dialogView.findViewById(R.id.btnSave);

        // Danh sách nguyên liệu tạm trong dialog
        List<RecipeModel.RecipeIngredient> tempIngredients = new ArrayList<>();
        
        // Nếu đang sửa, copy nguyên liệu hiện có
        if (existingRecipe != null) {
            tvDialogTitle.setText("Sửa công thức");
            if (existingRecipe.getIngredients() != null) {
                tempIngredients.addAll(existingRecipe.getIngredients());
            }
        }

        // Setup RecyclerView cho nguyên liệu
        recyclerIngredients.setLayoutManager(new LinearLayoutManager(getContext()));
        RecipeIngredientAdapter ingredientAdapter = new RecipeIngredientAdapter(tempIngredients, 
                position -> {
                    tempIngredients.remove(position);
                    recyclerIngredients.getAdapter().notifyDataSetChanged();
                    tvNoIngredients.setVisibility(tempIngredients.isEmpty() ? View.VISIBLE : View.GONE);
                });
        recyclerIngredients.setAdapter(ingredientAdapter);
        tvNoIngredients.setVisibility(tempIngredients.isEmpty() ? View.VISIBLE : View.GONE);

        // Lọc các món chưa có công thức (hoặc đang sửa)
        List<MenuItem> availableMenuItems = new ArrayList<>();
        for (MenuItem item : menuList) {
            boolean hasRecipe = false;
            for (RecipeModel recipe : recipeList) {
                if (recipe.getMenuItemId().equals(item.getId())) {
                    // Nếu đang sửa recipe này thì vẫn cho phép
                    if (existingRecipe == null || !existingRecipe.getId().equals(recipe.getId())) {
                        hasRecipe = true;
                    }
                    break;
                }
            }
            if (!hasRecipe) {
                availableMenuItems.add(item);
            }
        }

        // Setup dropdown chọn món ăn
        String[] menuItemNames = new String[availableMenuItems.size()];
        for (int i = 0; i < availableMenuItems.size(); i++) {
            menuItemNames[i] = availableMenuItems.get(i).getName();
        }
        ArrayAdapter<String> menuAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_dropdown_item_1line, menuItemNames);
        spinnerMenuItem.setAdapter(menuAdapter);

        if (existingRecipe != null) {
            spinnerMenuItem.setText(existingRecipe.getMenuItemName(), false);
            spinnerMenuItem.setEnabled(false); // Không cho đổi món khi sửa
        }

        // Setup dropdown chọn nguyên liệu từ kho
        String[] inventoryNames = new String[inventoryList.size()];
        for (int i = 0; i < inventoryList.size(); i++) {
            inventoryNames[i] = inventoryList.get(i).getName() + 
                    " (" + inventoryList.get(i).getQuantity() + " " + 
                    inventoryList.get(i).getUnit() + " trong kho)";
        }
        ArrayAdapter<String> inventoryAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_dropdown_item_1line, inventoryNames);
        spinnerIngredient.setAdapter(inventoryAdapter);

        // Cập nhật đơn vị khi chọn nguyên liệu
        final int[] selectedInventoryIndex = {-1};
        spinnerIngredient.setOnItemClickListener((parent, view, position, id) -> {
            selectedInventoryIndex[0] = position;
            if (position >= 0 && position < inventoryList.size()) {
                tvUnit.setText(inventoryList.get(position).getUnit());
            }
        });

        // Thêm nguyên liệu vào công thức
        btnAddIngredient.setOnClickListener(v -> {
            if (selectedInventoryIndex[0] < 0) {
                Toast.makeText(getContext(), "Vui lòng chọn nguyên liệu", Toast.LENGTH_SHORT).show();
                return;
            }

            String qtyStr = edtQuantityRequired.getText().toString().trim();
            if (qtyStr.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng nhập số lượng", Toast.LENGTH_SHORT).show();
                return;
            }

            double qty = Double.parseDouble(qtyStr);
            InventoryModel inv = inventoryList.get(selectedInventoryIndex[0]);

            // Kiểm tra đã thêm chưa
            for (RecipeModel.RecipeIngredient ing : tempIngredients) {
                if (ing.getIngredientId().equals(inv.getId())) {
                    Toast.makeText(getContext(), "Nguyên liệu này đã có trong công thức", 
                            Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            RecipeModel.RecipeIngredient newIng = new RecipeModel.RecipeIngredient(
                    inv.getId(), inv.getName(), qty, inv.getUnit());
            tempIngredients.add(newIng);
            ingredientAdapter.notifyDataSetChanged();
            tvNoIngredients.setVisibility(View.GONE);

            // Reset input
            spinnerIngredient.setText("", false);
            edtQuantityRequired.setText("");
            tvUnit.setText("(đơn vị)");
            selectedInventoryIndex[0] = -1;
        });

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String menuItemName = spinnerMenuItem.getText().toString().trim();
            
            if (menuItemName.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng chọn món ăn", Toast.LENGTH_SHORT).show();
                return;
            }

            if (tempIngredients.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng thêm ít nhất 1 nguyên liệu", Toast.LENGTH_SHORT).show();
                return;
            }

            // Tìm menuItemId
            String menuItemId = null;
            if (existingRecipe != null) {
                menuItemId = existingRecipe.getMenuItemId();
            } else {
                for (MenuItem item : availableMenuItems) {
                    if (item.getName().equals(menuItemName)) {
                        menuItemId = item.getId();
                        break;
                    }
                }
            }

            if (menuItemId == null) {
                Toast.makeText(getContext(), "Không tìm thấy món ăn", Toast.LENGTH_SHORT).show();
                return;
            }

            // Tạo/cập nhật recipe
            Map<String, Object> recipeData = new HashMap<>();
            recipeData.put("menuItemId", menuItemId);
            recipeData.put("menuItemName", menuItemName);
            
            // Convert ingredients to list of maps
            List<Map<String, Object>> ingredientsList = new ArrayList<>();
            for (RecipeModel.RecipeIngredient ing : tempIngredients) {
                Map<String, Object> ingMap = new HashMap<>();
                ingMap.put("ingredientId", ing.getIngredientId());
                ingMap.put("ingredientName", ing.getIngredientName());
                ingMap.put("quantityRequired", ing.getQuantityRequired());
                ingMap.put("unit", ing.getUnit());
                ingredientsList.add(ingMap);
            }
            recipeData.put("ingredients", ingredientsList);

            if (existingRecipe != null) {
                // Update
                FirebaseService.getInstance().getDb()
                        .collection("recipes")
                        .document(existingRecipe.getId())
                        .update(recipeData)
                        .addOnSuccessListener(unused -> {
                            Toast.makeText(getContext(), "Đã cập nhật công thức", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        })
                        .addOnFailureListener(e -> 
                            Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            } else {
                // Add new
                FirebaseService.getInstance().getDb()
                        .collection("recipes")
                        .add(recipeData)
                        .addOnSuccessListener(ref -> {
                            Toast.makeText(getContext(), "Đã thêm công thức", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        })
                        .addOnFailureListener(e -> 
                            Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });

        dialog.show();
    }

    private void showRecipeDetailDialog(RecipeModel recipe) {
        if (getContext() == null || !isAdded()) return;

        String[] options = {"Sửa công thức", "Xóa công thức", "Hủy"};

        new AlertDialog.Builder(getContext())
                .setTitle(recipe.getMenuItemName())
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // Sửa
                        showAddRecipeDialog(recipe);
                    } else if (which == 1) {
                        // Xóa
                        confirmDeleteRecipe(recipe);
                    }
                })
                .show();
    }

    private void confirmDeleteRecipe(RecipeModel recipe) {
        new AlertDialog.Builder(getContext())
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc muốn xóa công thức của \"" + recipe.getMenuItemName() + "\"?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    FirebaseService.getInstance().getDb()
                            .collection("recipes")
                            .document(recipe.getId())
                            .delete()
                            .addOnSuccessListener(unused -> 
                                Toast.makeText(getContext(), "Đã xóa công thức", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> 
                                Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (recipeListener != null) recipeListener.remove();
        if (inventoryListener != null) inventoryListener.remove();
        if (menuListener != null) menuListener.remove();
    }
}
