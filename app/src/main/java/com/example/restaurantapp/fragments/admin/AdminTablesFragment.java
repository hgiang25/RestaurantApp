package com.example.restaurantapp.fragments.admin;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.restaurantapp.R;
import com.example.restaurantapp.adapters.TableAdapter;
import com.example.restaurantapp.api.FirebaseService;
import com.example.restaurantapp.models.TableModel;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminTablesFragment extends Fragment {

    private static final String FILTER_ALL = "all";

    private RecyclerView recyclerView;
    private TableAdapter adapter;

    private final List<TableModel> allTables = new ArrayList<>();
    private final List<TableModel> tableList = new ArrayList<>();
    private String currentFilter = FILTER_ALL;

    private Uri selectedImageUri = null;
    private ActivityResultLauncher<Intent> pickImageLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_admin_tables, container, false);

        recyclerView = view.findViewById(R.id.recyclerTables);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        adapter = new TableAdapter(tableList, this::showEditTableDialog);
        recyclerView.setAdapter(adapter);

        ExtendedFloatingActionButton fabAdd = view.findViewById(R.id.fabAddTable);
        fabAdd.setOnClickListener(v -> showAddTableDialog());

        setupChips(view);
        loadTables();

        // Khởi tạo ActivityResultLauncher chọn ảnh
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        Toast.makeText(getContext(), "Đã chọn ảnh", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        return view;
    }

    /** ================= FILTER ================= */
    private void setupChips(View view) {
        view.findViewById(R.id.chipAll).setOnClickListener(v -> {
            currentFilter = FILTER_ALL;
            applyFilter();
        });
        view.findViewById(R.id.chipFree).setOnClickListener(v -> {
            currentFilter = TableModel.STATUS_FREE;
            applyFilter();
        });
        view.findViewById(R.id.chipOccupied).setOnClickListener(v -> {
            currentFilter = TableModel.STATUS_OCCUPIED;
            applyFilter();
        });
        view.findViewById(R.id.chipReserved).setOnClickListener(v -> {
            currentFilter = TableModel.STATUS_RESERVED;
            applyFilter();
        });
    }

    private void applyFilter() {
        tableList.clear();
        for (TableModel table : allTables) {
            if (FILTER_ALL.equals(currentFilter) || currentFilter.equals(table.getStatus())) {
                tableList.add(table);
            }
        }
        adapter.notifyDataSetChanged();
    }

    /** ================= REALTIME ================= */
    private void loadTables() {
        FirebaseService.getInstance().listenTablesRealtime("tables", (value, error) -> {
            if (!isAdded() || value == null) return;
            allTables.clear();
            for (DocumentSnapshot doc : value.getDocuments()) {
                TableModel table = doc.toObject(TableModel.class);
                if (table == null) continue;
                table.setId(doc.getId());
                allTables.add(table);
            }
            applyFilter();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        FirebaseService.getInstance().removeTableListener("tables");
    }

    /** ================= ADD ================= */
    private void showAddTableDialog() {
        if (getContext() == null) return;

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        EditText edtName = new EditText(getContext());
        edtName.setHint("Tên bàn (VD: Bàn 1)");
        layout.addView(edtName);

        EditText edtCapacity = new EditText(getContext());
        edtCapacity.setHint("Số chỗ ngồi");
        edtCapacity.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        layout.addView(edtCapacity);

        EditText edtImageUrl = new EditText(getContext());
        edtImageUrl.setHint("URL ảnh bàn (tùy chọn)");
        layout.addView(edtImageUrl);

        Button btnPickImage = new Button(getContext());
        btnPickImage.setText("Chọn ảnh từ thiết bị");
        layout.addView(btnPickImage);
        btnPickImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            pickImageLauncher.launch(intent);
        });

        new AlertDialog.Builder(getContext())
                .setTitle("Thêm bàn mới")
                .setView(layout)
                .setPositiveButton("Thêm", (d, w) -> {
                    String name = edtName.getText().toString().trim();
                    String capStr = edtCapacity.getText().toString().trim();
                    if (name.isEmpty() || capStr.isEmpty()) {
                        Toast.makeText(getContext(), "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int capacity = Integer.parseInt(capStr);
                    if (capacity <= 0) {
                        Toast.makeText(getContext(), "Số chỗ phải > 0", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (selectedImageUri != null) {
                        String filename = "table_" + System.currentTimeMillis() + ".png";
                        FirebaseService.getInstance().uploadTableImage(selectedImageUri, filename,
                                uploadedUrl -> createTableInFirestore(name, capacity, uploadedUrl),
                                e -> Toast.makeText(getContext(), "Lỗi upload ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    } else if (!edtImageUrl.getText().toString().trim().isEmpty()) {
                        createTableInFirestore(name, capacity, edtImageUrl.getText().toString().trim());
                    } else {
                        createTableInFirestore(name, capacity, null);
                    }

                    selectedImageUri = null; // reset
                })
                .setNegativeButton("Huỷ", null)
                .show();
    }

    private void createTableInFirestore(String name, int capacity, String imageUrl) {
        Map<String, Object> table = new HashMap<>();
        table.put("name", name);
        table.put("capacity", capacity);
        table.put("status", TableModel.STATUS_FREE);
        table.put("createdAt", FieldValue.serverTimestamp());
        table.put("updatedAt", FieldValue.serverTimestamp());
        if (imageUrl != null) table.put("imageUrl", imageUrl);

        FirebaseService.getInstance().createTable(
                name,
                capacity,
                r -> Toast.makeText(getContext(), "Thêm bàn thành công", Toast.LENGTH_SHORT).show(),
                e -> Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show()
        );
    }

    /** ================= EDIT ================= */
    private void showEditTableDialog(TableModel table) {
        String[] options = {"Đổi trạng thái", "Sửa thông tin", "Xoá bàn"};
        new AlertDialog.Builder(getContext())
                .setTitle(table.getName())
                .setItems(options, (d, i) -> {
                    if (i == 0) showChangeStatusDialog(table);
                    else if (i == 1) showEditInfoDialog(table);
                    else showDeleteTableDialog(table);
                })
                .show();
    }

    private void showChangeStatusDialog(TableModel table) {
        String[] labels = {"Trống", "Đang phục vụ", "Đã đặt"};
        String[] values = {TableModel.STATUS_FREE, TableModel.STATUS_OCCUPIED, TableModel.STATUS_RESERVED};
        new AlertDialog.Builder(getContext())
                .setTitle("Đổi trạng thái")
                .setItems(labels, (d, i) -> {
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("status", values[i]);
                    FirebaseService.getInstance().updateTable(
                            table.getId(),
                            updates,
                            u -> {},
                            e -> Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
                })
                .show();
    }

    private void showEditInfoDialog(TableModel table) {
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        EditText edtName = new EditText(getContext());
        edtName.setText(table.getName());
        layout.addView(edtName);

        EditText edtCapacity = new EditText(getContext());
        edtCapacity.setText(String.valueOf(table.getCapacity()));
        edtCapacity.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        layout.addView(edtCapacity);

        EditText edtImageUrl = new EditText(getContext());
        edtImageUrl.setHint("URL ảnh bàn (tùy chọn)");
        if (table.getImageUrl() != null) edtImageUrl.setText(table.getImageUrl());
        layout.addView(edtImageUrl);

        Button btnPickImage = new Button(getContext());
        btnPickImage.setText("Chọn ảnh từ thiết bị");
        layout.addView(btnPickImage);
        btnPickImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            pickImageLauncher.launch(intent);
        });

        new AlertDialog.Builder(getContext())
                .setTitle("Sửa thông tin bàn")
                .setView(layout)
                .setPositiveButton("Lưu", (d, w) -> {
                    int capacity = Integer.parseInt(edtCapacity.getText().toString().trim());
                    if (capacity <= 0) {
                        Toast.makeText(getContext(), "Số chỗ phải > 0", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String name = edtName.getText().toString().trim();

                    if (selectedImageUri != null) {
                        String filename = "table_" + System.currentTimeMillis() + ".png";
                        FirebaseService.getInstance().uploadTableImage(selectedImageUri, filename,
                                uploadedUrl -> updateTableInFirestore(table.getId(), name, capacity, uploadedUrl),
                                e -> Toast.makeText(getContext(), "Lỗi upload ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    } else if (!edtImageUrl.getText().toString().trim().isEmpty()) {
                        updateTableInFirestore(table.getId(), name, capacity, edtImageUrl.getText().toString().trim());
                    } else {
                        updateTableInFirestore(table.getId(), name, capacity, null);
                    }

                    selectedImageUri = null;
                })
                .setNegativeButton("Huỷ", null)
                .show();
    }

    private void updateTableInFirestore(String tableId, String name, int capacity, String imageUrl) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("capacity", capacity);
        updates.put("updatedAt", FieldValue.serverTimestamp());
        if (imageUrl != null) updates.put("imageUrl", imageUrl);

        FirebaseService.getInstance().updateTable(
                tableId,
                updates,
                u -> Toast.makeText(getContext(), "Cập nhật thành công", Toast.LENGTH_SHORT).show(),
                e -> Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show()
        );
    }

    private void showDeleteTableDialog(TableModel table) {
        new AlertDialog.Builder(getContext())
                .setTitle("Xoá bàn")
                .setMessage("Bạn chắc chắn muốn xoá " + table.getName() + "?")
                .setPositiveButton("Xoá", (d, w) ->
                        FirebaseService.getInstance().deleteTable(
                                table.getId(),
                                u -> Toast.makeText(getContext(), "Đã xoá", Toast.LENGTH_SHORT).show(),
                                e -> Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show()
                        ))
                .setNegativeButton("Huỷ", null)
                .show();
    }
}
