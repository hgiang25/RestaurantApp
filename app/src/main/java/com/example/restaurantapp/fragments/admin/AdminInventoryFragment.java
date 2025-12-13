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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminInventoryFragment extends Fragment {

    private RecyclerView recyclerView;
    private InventoryAdapter adapter;
    private List<InventoryModel> inventoryList = new ArrayList<>();
    private FloatingActionButton fabAdd;

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

        loadInventory();

        return view;
    }

    private void loadInventory() {
        FirebaseService.getInstance().listenInventoryRealtime((value, error) -> {
            if (error != null || value == null) return;

            inventoryList.clear();
            for (DocumentSnapshot doc : value.getDocuments()) {
                InventoryModel item = doc.toObject(InventoryModel.class);
                if (item != null) {
                    item.setId(doc.getId());
                    inventoryList.add(item);
                }
            }
            adapter.notifyDataSetChanged();
        });
    }

    private void showAddInventoryDialog() {
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

            double quantity = Double.parseDouble(quantityStr);
            double minQty = minQtyStr.isEmpty() ? 10 : Double.parseDouble(minQtyStr);

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

    private void showInventoryDetail(InventoryModel item) {
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
            updates.put("quantity", Double.parseDouble(quantityStr));
            updates.put("unit", unit);
            updates.put("minQuantity", minQtyStr.isEmpty() ? 10 : Double.parseDouble(minQtyStr));

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
