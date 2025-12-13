package com.example.restaurantapp.fragments.customer;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.restaurantapp.R;
import com.example.restaurantapp.adapters.MenuAdapter;
import com.example.restaurantapp.api.FirebaseService;
import com.example.restaurantapp.models.MenuItem;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomerMenuFragment extends Fragment {

    private RecyclerView recyclerView;
    private ChipGroup chipGroupCategories;
    private MenuAdapter adapter;
    private List<MenuItem> menuList = new ArrayList<>();
    private List<MenuItem> filteredList = new ArrayList<>();
    private String selectedCategory = "Tất cả";

    // Giỏ hàng
    private Map<String, Integer> cart = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_customer_menu, container, false);

        recyclerView = view.findViewById(R.id.recyclerMenu);
        chipGroupCategories = view.findViewById(R.id.chipGroupCategories);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new MenuAdapter(filteredList, this::onAddToCart);
        recyclerView.setAdapter(adapter);

        view.findViewById(R.id.fabCart).setOnClickListener(v -> showCartDialog());

        loadMenu();

        return view;
    }

    private void loadMenu() {
        FirebaseService.getInstance().listenMenuRealtime((value, error) -> {
            if (error != null || value == null) return;

            menuList.clear();
            List<String> categories = new ArrayList<>();
            categories.add("Tất cả");

            for (DocumentSnapshot doc : value.getDocuments()) {
                MenuItem item = doc.toObject(MenuItem.class);
                if (item != null) {
                    item.setId(doc.getId());
                    if (item.isAvailable()) {
                        menuList.add(item);
                        if (!categories.contains(item.getCategory())) {
                            categories.add(item.getCategory());
                        }
                    }
                }
            }

            setupCategoryChips(categories);
            filterByCategory();
        });
    }

    private void setupCategoryChips(List<String> categories) {
        chipGroupCategories.removeAllViews();
        for (String category : categories) {
            Chip chip = new Chip(getContext());
            chip.setText(category);
            chip.setCheckable(true);
            chip.setChecked(category.equals(selectedCategory));
            chip.setOnClickListener(v -> {
                selectedCategory = category;
                filterByCategory();
            });
            chipGroupCategories.addView(chip);
        }
    }

    private void filterByCategory() {
        filteredList.clear();
        if (selectedCategory.equals("Tất cả")) {
            filteredList.addAll(menuList);
        } else {
            for (MenuItem item : menuList) {
                if (item.getCategory().equals(selectedCategory)) {
                    filteredList.add(item);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void onAddToCart(MenuItem item) {
        int currentQty = cart.getOrDefault(item.getId(), 0);
        cart.put(item.getId(), currentQty + 1);
        Toast.makeText(getContext(), "Đã thêm " + item.getName() + " vào giỏ", Toast.LENGTH_SHORT).show();
    }

    private void showCartDialog() {
        if (cart.isEmpty()) {
            Toast.makeText(getContext(), "Giỏ hàng trống", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder cartContent = new StringBuilder();
        double total = 0;

        for (Map.Entry<String, Integer> entry : cart.entrySet()) {
            for (MenuItem item : menuList) {
                if (item.getId().equals(entry.getKey())) {
                    double itemTotal = item.getPrice() * entry.getValue();
                    total += itemTotal;
                    cartContent.append(item.getName())
                            .append(" x").append(entry.getValue())
                            .append(" = ").append(String.format("%,.0f", itemTotal)).append("đ\n");
                    break;
                }
            }
        }
        cartContent.append("\nTổng: ").append(String.format("%,.0f", total)).append("đ");

        new AlertDialog.Builder(getContext())
                .setTitle("Giỏ hàng")
                .setMessage(cartContent.toString())
                .setPositiveButton("Đặt món", (dialog, which) -> placeOrder())
                .setNegativeButton("Tiếp tục chọn", null)
                .setNeutralButton("Xóa giỏ", (dialog, which) -> {
                    cart.clear();
                    Toast.makeText(getContext(), "Đã xóa giỏ hàng", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void placeOrder() {
        String customerId = FirebaseService.getInstance().getCurrentUserId();
        if (customerId == null) return;

        // Tạo danh sách items cho order
        List<Map<String, Object>> items = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : cart.entrySet()) {
            Map<String, Object> itemMap = new HashMap<>();
            itemMap.put("menuId", entry.getKey());
            itemMap.put("quantity", entry.getValue());
            
            // Tìm tên món
            for (MenuItem menuItem : menuList) {
                if (menuItem.getId().equals(entry.getKey())) {
                    itemMap.put("name", menuItem.getName());
                    itemMap.put("price", menuItem.getPrice());
                    break;
                }
            }
            items.add(itemMap);
        }

        // Gọi API tạo order
        FirebaseService.getInstance().createOrder(customerId, null, items,
                docRef -> {
                    Toast.makeText(getContext(), "Đặt món thành công!", Toast.LENGTH_SHORT).show();
                    cart.clear();
                },
                e -> Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
