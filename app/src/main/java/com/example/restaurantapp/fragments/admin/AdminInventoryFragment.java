package com.example.restaurantapp.fragments.admin;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.restaurantapp.R;
import com.example.restaurantapp.adapters.InventoryAdapter;
import com.example.restaurantapp.api.FirebaseService;
import com.example.restaurantapp.models.InventoryModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminInventoryFragment extends Fragment {

    private RecyclerView recyclerView;
    private InventoryAdapter adapter;
    private List<InventoryModel> inventoryList = new ArrayList<>();
    private FloatingActionButton fabAdd;
    private ListenerRegistration inventoryListener;
    private EditText edtSearch;
    private List<InventoryModel> filteredList = new ArrayList<>();


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_inventory, container, false);

        recyclerView = view.findViewById(R.id.recyclerInventory);
        fabAdd = view.findViewById(R.id.fabAddInventory);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new InventoryAdapter(inventoryList, this::showInventoryDetail);
        recyclerView.setAdapter(adapter);

        fabAdd.setOnClickListener(v -> showAddInventoryDialog());

        loadInventory(""); // Bắt đầu với danh sách đầy đủ



        edtSearch = view.findViewById(R.id.edtSearchInventory);

// Gắn TextWatcher để tìm kiếm theo tên nguyên liệu
        edtSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Gọi lại loadInventory với keyword từ EditText
                loadInventory(s.toString().trim());
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });


        return view;
    }
    private void listAllInventoryItems() {
        FirebaseService.getInstance().getDb()
                .collection("inventory")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        Toast.makeText(getContext(), "Chưa có nguyên liệu nào trong kho.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    StringBuilder sb = new StringBuilder();
                    sb.append("Danh sách nguyên liệu:\n");
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        InventoryModel item = doc.toObject(InventoryModel.class);
                        if (item != null) {
                            String id = doc.getId();
                            String name = item.getName();
                            sb.append("Tên: ").append(name).append(" | ID: ").append(id).append("\n");
                        }
                    }

                    // Hiển thị danh sách bằng Toast hoặc log
                    android.util.Log.d("AdminInventory", sb.toString());
                    Toast.makeText(getContext(), sb.toString(), Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Lỗi khi lấy danh sách nguyên liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }


    private void loadInventory(@Nullable String keyword) {
        // Hủy listener cũ nếu có
        if (inventoryListener != null) {
            inventoryListener.remove();
        }

        inventoryListener = FirebaseService.getInstance().listenInventoryRealtime((value, error) -> {
            if (!isAdded() || getContext() == null) return;
            if (error != null) {
                android.util.Log.e("AdminInventory", "Lỗi load inventory: " + error.getMessage());
                return;
            }
            if (value == null) return;

            inventoryList.clear();
            for (DocumentSnapshot doc : value.getDocuments()) {
                InventoryModel item = doc.toObject(InventoryModel.class);
                if (item != null) {
                    item.setId(doc.getId());
                    // Lọc trực tiếp theo keyword nếu có
                    if (keyword == null || keyword.isEmpty() || item.getName().toLowerCase().contains(keyword.toLowerCase())) {
                        inventoryList.add(item);
                    }
                }
            }

            if (adapter != null) adapter.notifyDataSetChanged();
        });
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (inventoryListener != null) {
            inventoryListener.remove();
            inventoryListener = null;
        }
    }

    private void showAddInventoryDialog() {
        if (getContext() == null || !isAdded()) return;
        
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_inventory, null);

        EditText edtName = dialogView.findViewById(R.id.edtItemName);
        EditText edtQuantity = dialogView.findViewById(R.id.edtQuantity);
        EditText edtUnit = dialogView.findViewById(R.id.edtUnit);
        EditText edtMinQuantity = dialogView.findViewById(R.id.edtMinQuantity);

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .create();

        dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.btnSave).setOnClickListener(v -> {
            String name = edtName.getText().toString().trim();
            String quantityStr = edtQuantity.getText().toString().trim();
            String unit = edtUnit.getText().toString().trim();
            String minQtyStr = edtMinQuantity.getText().toString().trim();

            if (name.isEmpty() || quantityStr.isEmpty() || unit.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }

            quantityStr = quantityStr.replace(",", ".");
            minQtyStr = minQtyStr.replace(",", ".");

            double quantity, minQty;
            try {
                quantity = Double.parseDouble(quantityStr);
                minQty = minQtyStr.isEmpty() ? 10 : Double.parseDouble(minQtyStr);
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Số lượng không hợp lệ", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseService.getInstance().addStockItem(name, quantity, unit,
                    ref -> {
                        // Thêm minQuantity
                        ref.update("minQuantity", minQty);
                        Toast.makeText(getContext(), "Thêm thành công", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    },
                    e -> Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show()
            );
        });

        dialog.show();
    }

    private void addSampleInventory() {
        // Danh sách nguyên liệu mẫu
        String[][] items = {
                {"Tỏi", "5", "kg", "1"},
                {"Ớt đỏ", "7", "kg", "2"},
                {"Nước mắm", "50", "lít", "10"},
                {"Dầu ăn", "40", "lít", "5"},
                {"Đường", "30", "kg", "5"},
                {"Muối", "20", "kg", "3"},
                {"Tiêu", "5", "kg", "1"},
                {"Bánh mì", "50", "cái", "10"},
                {"Bột năng", "15", "kg", "3"},
                {"Bột mì", "40", "kg", "5"},
                {"Trứng gà", "100", "cái", "20"},
                {"Sữa tươi", "30", "lít", "5"},
                {"Kem tươi", "20", "lít", "5"},
                {"Phô mai", "15", "kg", "3"},
                {"Bánh ngọt", "50", "cái", "10"},
                {"Mì ống", "25", "kg", "5"},
                {"Rau sống", "20", "kg", "5"}
        };

        for (String[] item : items) {
            String name = item[0];
            double quantity = Double.parseDouble(item[1]);
            String unit = item[2];
            double minQty = Double.parseDouble(item[3]);

            FirebaseService.getInstance().addStockItem(name, quantity, unit,
                    ref -> {
                        // Thêm minQuantity
                        ref.update("minQuantity", minQty);
                    },
                    e -> {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Lỗi thêm " + name + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
            );
        }

        if (getContext() != null) {
            Toast.makeText(getContext(), "Đã gửi yêu cầu thêm nguyên liệu mẫu", Toast.LENGTH_SHORT).show();
        }
    }


    private void showInventoryDetail(InventoryModel item) {
        if (getContext() == null || !isAdded()) return;
        
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_inventory, null);

        EditText edtName = dialogView.findViewById(R.id.edtItemName);
        EditText edtQuantity = dialogView.findViewById(R.id.edtQuantity);
        EditText edtUnit = dialogView.findViewById(R.id.edtUnit);
        EditText edtMinQuantity = dialogView.findViewById(R.id.edtMinQuantity);

        edtName.setText(item.getName());
        edtQuantity.setText(String.valueOf(item.getQuantity()));
        edtUnit.setText(item.getUnit());
        edtMinQuantity.setText(String.valueOf(item.getMinQuantity()));

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .setNegativeButton("Xóa", (d, w) -> {
                    FirebaseService.getInstance().getDb().collection("inventory")
                            .document(item.getId())
                            .delete()
                            .addOnSuccessListener(unused ->
                                    Toast.makeText(getContext(), "Đã xóa", Toast.LENGTH_SHORT).show()
                            );
                })
                .create();

        dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.btnSave).setOnClickListener(v -> {
            String name = edtName.getText().toString().trim();
            String quantityStr = edtQuantity.getText().toString().trim();
            String unit = edtUnit.getText().toString().trim();
            String minQtyStr = edtMinQuantity.getText().toString().trim();

            if (name.isEmpty() || quantityStr.isEmpty() || unit.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("name", name);
            updates.put("unit", unit);
            quantityStr = quantityStr.replace(",", ".");
            minQtyStr = minQtyStr.replace(",", ".");

            double quantity, minQty;
            try {
                quantity = Double.parseDouble(quantityStr);
                minQty = minQtyStr.isEmpty() ? 10 : Double.parseDouble(minQtyStr);
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Số lượng không hợp lệ", Toast.LENGTH_SHORT).show();
                return;
            }

            updates.put("quantity", quantity);
            updates.put("minQuantity", minQty);

            FirebaseService.getInstance().updateStockItem(item.getId(), updates,
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
