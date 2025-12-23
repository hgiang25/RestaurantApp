package com.example.restaurantapp.fragments.admin;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
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
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
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
        if (!isAdded() || getContext() == null) return;

        View view = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_add_menu_item, null);

        EditText edtName = view.findViewById(R.id.edtItemName);
        EditText edtPrice = view.findViewById(R.id.edtPrice);
        EditText edtImageUrl = view.findViewById(R.id.edtImageUrl);
        EditText edtDescription = view.findViewById(R.id.edtDescription);
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
            String imageUrl = edtImageUrl.getText().toString().trim();
            boolean available = switchAvailable.isChecked();

            if (name.isEmpty() || priceStr.isEmpty() || category.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }

            double price = Double.parseDouble(priceStr);

            FirebaseService.getInstance().addMenuItem(
                    name,
                    price,
                    category,
                    available,
                    imageUrl.isEmpty() ? null : imageUrl,
                    null,
                    ref -> {
                        Toast.makeText(getContext(), "Thêm món thành công", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    },
                    e -> Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show()
            );
        });

        dialog.show();
    }


    private void showEditMenuDialog(MenuItem item) {
        String[] options = {"Sửa thông tin", item.isAvailable() ? "Tạm hết món" : "Có sẵn", "Hủy"};

        new AlertDialog.Builder(getContext())
                .setTitle(item.getName())
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showEditInfoDialog(item);
                    } else if (which == 1) {
                        // Toggle available
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("available", !item.isAvailable());
                        FirebaseService.getInstance().updateMenuItem(item.getId(), updates,
                                unused -> {},
                                e -> Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
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
            Map<String, Object> updates = new HashMap<>();
            updates.put("name", edtName.getText().toString().trim());
            updates.put("price", Double.parseDouble(edtPrice.getText().toString().trim()));
            updates.put("category", spinnerCategory.getText().toString().trim());
            updates.put("available", switchAvailable.isChecked());

            FirebaseService.getInstance().updateMenuItem(
                    item.getId(),
                    updates,
                    unused -> {
                        Toast.makeText(getContext(), "Cập nhật thành công", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    },
                    e -> Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show()
            );
        });

        dialog.show();
    }

}
