package com.example.restaurantapp.fragments.admin;

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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.restaurantapp.R;
import com.example.restaurantapp.adapters.TableAdapter;
import com.example.restaurantapp.api.FirebaseService;
import com.example.restaurantapp.models.TableModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminTablesFragment extends Fragment {

    private RecyclerView recyclerView;
    private TableAdapter adapter;
    private List<TableModel> tableList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_tables, container, false);

        recyclerView = view.findViewById(R.id.recyclerTables);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));

        adapter = new TableAdapter(tableList, this::showEditTableDialog);
        recyclerView.setAdapter(adapter);

        ExtendedFloatingActionButton fabAdd = view.findViewById(R.id.fabAddTable);

        fabAdd.setOnClickListener(v -> showAddTableDialog());

        loadTables();

        return view;
    }

    private void loadTables() {
        FirebaseService.getInstance().listenTablesRealtime((value, error) -> {
            if (!isAdded() || getContext() == null) return;
            if (error != null || value == null) return;

            tableList.clear();
            for (DocumentSnapshot doc : value.getDocuments()) {
                TableModel table = doc.toObject(TableModel.class);
                if (table != null) {
                    table.setId(doc.getId());
                    tableList.add(table);
                }
            }
            if (adapter != null) adapter.notifyDataSetChanged();
        });
    }

    private void showAddTableDialog() {
        if (getContext() == null || !isAdded()) return;
        
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

        new AlertDialog.Builder(getContext())
                .setTitle("Thêm bàn mới")
                .setView(layout)
                .setPositiveButton("Thêm", (dialog, which) -> {
                    String name = edtName.getText().toString().trim();
                    String capacityStr = edtCapacity.getText().toString().trim();

                    if (name.isEmpty() || capacityStr.isEmpty()) {
                        Toast.makeText(getContext(), "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int capacity = Integer.parseInt(capacityStr);
                    FirebaseService.getInstance().createTable(name, capacity,
                            docRef -> Toast.makeText(getContext(), "Thêm bàn thành công!", Toast.LENGTH_SHORT).show(),
                            e -> Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showEditTableDialog(TableModel table) {
        String[] options = {"Đổi trạng thái", "Sửa thông tin", "Hủy"};

        new AlertDialog.Builder(getContext())
                .setTitle(table.getName())
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // Đổi trạng thái
                        String newStatus = "free".equals(table.getStatus()) ? "occupied" : "free";
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("status", newStatus);
                        FirebaseService.getInstance().updateTable(table.getId(), updates,
                                unused -> {},
                                e -> Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    } else if (which == 1) {
                        // Sửa thông tin
                        showEditInfoDialog(table);
                    }
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

        new AlertDialog.Builder(getContext())
                .setTitle("Sửa thông tin bàn")
                .setView(layout)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("name", edtName.getText().toString().trim());
                    updates.put("capacity", Integer.parseInt(edtCapacity.getText().toString().trim()));

                    FirebaseService.getInstance().updateTable(table.getId(), updates,
                            unused -> Toast.makeText(getContext(), "Cập nhật thành công!", Toast.LENGTH_SHORT).show(),
                            e -> Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}
