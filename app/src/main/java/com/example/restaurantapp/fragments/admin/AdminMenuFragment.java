package com.example.restaurantapp.fragments.admin;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
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
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminMenuFragment extends Fragment {

    private RecyclerView recyclerView;
    private AdminMenuAdapter adapter;
    private List<MenuItem> menuList = new ArrayList<>();

    private String[] categories = {"Món chính", "Món phụ", "Đồ uống", "Tráng miệng", "Khác"};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_menu, container, false);

        recyclerView = view.findViewById(R.id.recyclerMenu);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new AdminMenuAdapter(menuList, this::showEditMenuDialog);
        recyclerView.setAdapter(adapter);

        ExtendedFloatingActionButton fabAdd = view.findViewById(R.id.fabAddMenu);
        fabAdd.setOnClickListener(v -> showAddMenuDialog());

        loadMenu();

        return view;
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
            if (adapter != null) adapter.notifyDataSetChanged();
        });
    }

    private void showAddMenuDialog() {
        if (getContext() == null || !isAdded()) return;
        
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        EditText edtName = new EditText(getContext());
        edtName.setHint("Tên món");
        layout.addView(edtName);

        EditText edtPrice = new EditText(getContext());
        edtPrice.setHint("Giá (VNĐ)");
        edtPrice.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(edtPrice);

        Spinner spinnerCategory = new Spinner(getContext());
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_dropdown_item, categories);
        spinnerCategory.setAdapter(spinnerAdapter);
        layout.addView(spinnerCategory);

        new AlertDialog.Builder(getContext())
                .setTitle("Thêm món mới")
                .setView(layout)
                .setPositiveButton("Thêm", (dialog, which) -> {
                    String name = edtName.getText().toString().trim();
                    String priceStr = edtPrice.getText().toString().trim();
                    String category = spinnerCategory.getSelectedItem().toString();

                    if (name.isEmpty() || priceStr.isEmpty()) {
                        Toast.makeText(getContext(), "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    double price = Double.parseDouble(priceStr);
                    FirebaseService.getInstance().addMenuItem(name, price, category, true, null, null,
                            docRef -> Toast.makeText(getContext(), "Thêm món thành công!", Toast.LENGTH_SHORT).show(),
                            e -> Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showEditMenuDialog(MenuItem item) {
        if (getContext() == null || !isAdded()) return;
        
        String[] options = {"Sửa thông tin", item.isAvailable() ? "Tạm hết món" : "Có sẵn", "Hủy"};

        new AlertDialog.Builder(getContext())
                .setTitle(item.getName())
                .setItems(options, (dialog, which) -> {
                    if (getContext() == null || !isAdded()) return;
                    
                    if (which == 0) {
                        showEditInfoDialog(item);
                    } else if (which == 1) {
                        // Toggle available
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("available", !item.isAvailable());
                        FirebaseService.getInstance().updateMenuItem(item.getId(), updates,
                                unused -> {},
                                e -> {
                                    if (getContext() != null && isAdded()) {
                                        Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                })
                .show();
    }

    private void showEditInfoDialog(MenuItem item) {
        if (getContext() == null || !isAdded()) return;
        
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        EditText edtName = new EditText(getContext());
        edtName.setText(item.getName());
        layout.addView(edtName);

        EditText edtPrice = new EditText(getContext());
        edtPrice.setText(String.valueOf(item.getPrice()));
        edtPrice.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(edtPrice);

        Spinner spinnerCategory = new Spinner(getContext());
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_dropdown_item, categories);
        spinnerCategory.setAdapter(spinnerAdapter);
        // Set selected category
        for (int i = 0; i < categories.length; i++) {
            if (categories[i].equals(item.getCategory())) {
                spinnerCategory.setSelection(i);
                break;
            }
        }
        layout.addView(spinnerCategory);

        new AlertDialog.Builder(getContext())
                .setTitle("Sửa thông tin món")
                .setView(layout)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("name", edtName.getText().toString().trim());
                    updates.put("price", Double.parseDouble(edtPrice.getText().toString().trim()));
                    updates.put("category", spinnerCategory.getSelectedItem().toString());

                    FirebaseService.getInstance().updateMenuItem(item.getId(), updates,
                            unused -> Toast.makeText(getContext(), "Cập nhật thành công!", Toast.LENGTH_SHORT).show(),
                            e -> Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}
