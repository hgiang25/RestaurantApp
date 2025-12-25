package com.example.restaurantapp.fragments.admin;

import android.app.AlertDialog;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.restaurantapp.R;
import com.example.restaurantapp.adapters.AdminMenuAdapter;
import com.example.restaurantapp.api.FirebaseService;
import com.example.restaurantapp.models.MenuItem;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bumptech.glide.Glide;

public class AdminMenuFragment extends Fragment {

    private RecyclerView recyclerView;
    private AdminMenuAdapter adapter;
    private List<MenuItem> menuList = new ArrayList<>();
    private List<MenuItem> filteredList = new ArrayList<>();

    // Search & Filter
    private EditText edtSearch;
    private ImageView btnClearSearch;
    private ChipGroup chipGroupCategories;
    private TextView tvResultCount;
    private LinearLayout layoutEmpty;
    private TextView tvEmptyMessage;

    private String currentCategory = "Tất cả";
    private String currentSearchQuery = "";

    private String[] categories = {"Món chính", "Món phụ", "Đồ uống", "Tráng miệng", "Khác"};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_menu, container, false);

        initViews(view);
        setupCategoryChips();
        setupSearch();
        setupRecyclerView();

        ExtendedFloatingActionButton fabAdd = view.findViewById(R.id.fabAddMenu);
        fabAdd.setOnClickListener(v -> showAddMenuDialog());

        loadMenu();

        return view;
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerMenu);
        edtSearch = view.findViewById(R.id.edtSearch);
        btnClearSearch = view.findViewById(R.id.btnClearSearch);
        chipGroupCategories = view.findViewById(R.id.chipGroupCategories);
        tvResultCount = view.findViewById(R.id.tvResultCount);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);
        tvEmptyMessage = view.findViewById(R.id.tvEmptyMessage);
    }

    private void setupCategoryChips() {
        // Add "All" chip first
        Chip allChip = createCategoryChip("Tất cả", true);
        chipGroupCategories.addView(allChip);

        // Add category chips
        for (String category : categories) {
            Chip chip = createCategoryChip(category, false);
            chipGroupCategories.addView(chip);
        }

        chipGroupCategories.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                // Keep at least one selected
                allChip.setChecked(true);
                return;
            }

            Chip selectedChip = group.findViewById(checkedIds.get(0));
            if (selectedChip != null) {
                currentCategory = selectedChip.getText().toString();
                filterMenu();
            }
        });
    }

    private Chip createCategoryChip(String text, boolean isChecked) {
        Chip chip = new Chip(getContext());
        chip.setText(text);
        chip.setCheckable(true);
        chip.setChecked(isChecked);
        chip.setChipBackgroundColorResource(R.color.chip_background_selector);
        chip.setTextColor(getResources().getColorStateList(R.color.chip_text_selector, null));
        chip.setChipStrokeWidth(1f);
        chip.setChipStrokeColorResource(R.color.chip_stroke_selector);
        return chip;
    }

    private void setupSearch() {
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().trim();
                btnClearSearch.setVisibility(currentSearchQuery.isEmpty() ? View.GONE : View.VISIBLE);
                filterMenu();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        edtSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                filterMenu();
                return true;
            }
            return false;
        });

        btnClearSearch.setOnClickListener(v -> {
            edtSearch.setText("");
            currentSearchQuery = "";
            filterMenu();
        });
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AdminMenuAdapter(filteredList, this::showEditMenuDialog);
        recyclerView.setAdapter(adapter);
    }

    private void loadMenu() {
        FirebaseService.getInstance().listenMenuRealtime((value, error) -> {
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
            filterMenu();
        });
    }

    private void filterMenu() {
        filteredList.clear();

        for (MenuItem item : menuList) {
            boolean matchesCategory = currentCategory.equals("Tất cả") ||
                    (item.getCategory() != null && item.getCategory().equals(currentCategory));

            boolean matchesSearch = currentSearchQuery.isEmpty() ||
                    (item.getName() != null && item.getName().toLowerCase().contains(currentSearchQuery.toLowerCase()));

            if (matchesCategory && matchesSearch) {
                filteredList.add(item);
            }
        }

        // Update UI
        updateResultCount();
        updateEmptyState();

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void updateResultCount() {
        String categoryText = currentCategory.equals("Tất cả") ? "" : " trong \"" + currentCategory + "\"";
        String searchText = currentSearchQuery.isEmpty() ? "" : " với từ khóa \"" + currentSearchQuery + "\"";
        tvResultCount.setText("Đang hiển thị " + filteredList.size() + " món" + categoryText + searchText);
    }

    private void updateEmptyState() {
        if (filteredList.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);

            if (!currentSearchQuery.isEmpty()) {
                tvEmptyMessage.setText("Không tìm thấy món ăn\nvới từ khóa \"" + currentSearchQuery + "\"");
            } else if (!currentCategory.equals("Tất cả")) {
                tvEmptyMessage.setText("Không có món nào\ntrong danh mục \"" + currentCategory + "\"");
            } else {
                tvEmptyMessage.setText("Chưa có món ăn nào\nNhấn + để thêm món mới");
            }
        } else {
            layoutEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void showAddMenuDialog() {
        if (!isAdded() || getContext() == null) return;

        View view = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_add_menu_item, null);

        EditText edtName = view.findViewById(R.id.edtItemName);
        EditText edtPrice = view.findViewById(R.id.edtPrice);
        EditText edtImageUrl = view.findViewById(R.id.edtImageUrl); // có thể dùng Uri picker
        AutoCompleteTextView spinnerCategory = view.findViewById(R.id.spinnerCategory);
        SwitchMaterial switchAvailable = view.findViewById(R.id.switchAvailable);

        // setup category dropdown
        ArrayAdapter<String> categoryAdapter =
                new ArrayAdapter<>(getContext(),
                        android.R.layout.simple_dropdown_item_1line,
                        categories);
        spinnerCategory.setAdapter(categoryAdapter);

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(view)
                .create();

        view.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());

        view.findViewById(R.id.btnSave).setOnClickListener(v -> {
            String name = edtName.getText().toString().trim();
            String priceStr = edtPrice.getText().toString().trim();
            String category = spinnerCategory.getText().toString().trim();
            boolean available = switchAvailable.isChecked();

            if (name.isEmpty() || priceStr.isEmpty() || category.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }

            double price = Double.parseDouble(priceStr);

            // Nếu admin đã chọn ảnh Uri
            Uri imageUri = /* lấy từ picker, ví dụ imageUri được set sẵn khi chọn ảnh */ null;

            if (imageUri != null) {
                String filename = "menu_" + System.currentTimeMillis() + ".png";

                FirebaseService.getInstance().uploadMenuImage(imageUri, filename,
                        imageUrl -> {
                            // Sau khi upload xong, lưu menu vào Firestore
                            FirebaseService.getInstance().addMenuItem(
                                    name, price, category, available, imageUrl, null,
                                    ref -> {
                                        Toast.makeText(getContext(), "Thêm món thành công", Toast.LENGTH_SHORT).show();
                                        dialog.dismiss();
                                    },
                                    e -> Toast.makeText(getContext(), "Lỗi lưu menu: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                            );
                        },
                        e -> Toast.makeText(getContext(), "Lỗi upload ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            } else {
                // Nếu không chọn ảnh, vẫn lưu menu nhưng imageUrl = null
                FirebaseService.getInstance().addMenuItem(
                        name, price, category, available, null, null,
                        ref -> {
                            Toast.makeText(getContext(), "Thêm món thành công", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        },
                        e -> Toast.makeText(getContext(), "Lỗi lưu menu: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        });

        dialog.show();
    }



    private void showEditMenuDialog(MenuItem item) {
        String[] options = {"Sửa thông tin", item.isAvailable() ? "Tạm hết món" : "Có sẵn", "Xóa món", "Hủy"};

        new AlertDialog.Builder(getContext())
                .setTitle(item.getName())
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            showEditInfoDialog(item);
                            break;
                        case 1:
                            // Toggle available
                            Map<String, Object> updates = new HashMap<>();
                            updates.put("available", !item.isAvailable());
                            FirebaseService.getInstance().updateMenuItem(item.getId(), updates,
                                    unused -> {},
                                    e -> Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                            break;
                        case 2:
                            // Xác nhận xóa
                            new AlertDialog.Builder(getContext())
                                    .setTitle("Xóa món")
                                    .setMessage("Bạn có chắc muốn xóa món \"" + item.getName() + "\"?")
                                    .setPositiveButton("Xóa", (d, w) -> {
                                        FirebaseService.getInstance().deleteMenuItem(item.getId(),
                                                unused -> Toast.makeText(getContext(), "Đã xóa món", Toast.LENGTH_SHORT).show(),
                                                e -> Toast.makeText(getContext(), "Lỗi xóa món: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                        );
                                    })
                                    .setNegativeButton("Hủy", null)
                                    .show();
                            break;
                        case 3:
                            dialog.dismiss();
                            break;
                    }
                })
                .show();
    }


    private void showEditInfoDialog(MenuItem item) {
        if (!isAdded() || getContext() == null) return;

        View view = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_add_menu_item, null);

        EditText edtName = view.findViewById(R.id.edtItemName);
        EditText edtPrice = view.findViewById(R.id.edtPrice);
        EditText edtImageUrl = view.findViewById(R.id.edtImageUrl);
        AutoCompleteTextView spinnerCategory = view.findViewById(R.id.spinnerCategory);
        SwitchMaterial switchAvailable = view.findViewById(R.id.switchAvailable);

        edtName.setText(item.getName());
        edtPrice.setText(String.valueOf(item.getPrice()));
        switchAvailable.setChecked(item.isAvailable());

        ArrayAdapter<String> categoryAdapter =
                new ArrayAdapter<>(getContext(),
                        android.R.layout.simple_dropdown_item_1line,
                        categories);
        spinnerCategory.setAdapter(categoryAdapter);
        spinnerCategory.setText(item.getCategory(), false);

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(view)
                .create();

        view.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());

        view.findViewById(R.id.btnSave).setOnClickListener(v -> {
            String name = edtName.getText().toString().trim();
            String priceStr = edtPrice.getText().toString().trim();
            String category = spinnerCategory.getText().toString().trim();
            boolean available = switchAvailable.isChecked();
            String imageUrlInput = edtImageUrl.getText().toString().trim();

            if (name.isEmpty() || priceStr.isEmpty() || category.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }

            double price = Double.parseDouble(priceStr);

            // Tạo map để update
            Map<String, Object> updates = new HashMap<>();
            updates.put("name", name);
            updates.put("price", price);
            updates.put("category", category);
            updates.put("available", available);
            updates.put("imageUrl", imageUrlInput.isEmpty() ? null : imageUrlInput);

            // Cập nhật document hiện có
            FirebaseService.getInstance().updateMenuItem(item.getId(), updates,
                    unused -> {
                        Toast.makeText(getContext(), "Cập nhật món thành công", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    },
                    e -> Toast.makeText(getContext(), "Lỗi cập nhật món: " + e.getMessage(), Toast.LENGTH_SHORT).show()
            );
        });

        dialog.show();
    }

}
