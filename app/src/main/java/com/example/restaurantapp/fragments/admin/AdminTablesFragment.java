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
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;

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
            if (FILTER_ALL.equals(currentFilter)
                    || currentFilter.equals(table.getStatus())) {
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

        new AlertDialog.Builder(getContext())
                .setTitle("Thêm bàn mới")
                .setView(layout)
                .setPositiveButton("Thêm", (d, w) -> {
                    String name = edtName.getText().toString().trim();
                    String capStr = edtCapacity.getText().toString().trim();

                    if (name.isEmpty() || capStr.isEmpty()) {
                        Toast.makeText(getContext(),
                                "Vui lòng nhập đầy đủ thông tin",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int capacity = Integer.parseInt(capStr);
                    if (capacity <= 0) {
                        Toast.makeText(getContext(),
                                "Số chỗ phải > 0",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    FirebaseService.getInstance().createTable(
                            name,
                            capacity,
                            r -> Toast.makeText(getContext(),
                                    "Thêm bàn thành công",
                                    Toast.LENGTH_SHORT).show(),
                            e -> Toast.makeText(getContext(),
                                    e.getMessage(),
                                    Toast.LENGTH_SHORT).show()
                    );
                })
                .setNegativeButton("Huỷ", null)
                .show();
    }

    /** ================= EDIT ================= */

    private void showEditTableDialog(TableModel table) {
        String[] options = {
                "Đổi trạng thái",
                "Sửa thông tin",
                "Xoá bàn"
        };

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
        String[] values = {
                TableModel.STATUS_FREE,
                TableModel.STATUS_OCCUPIED,
                TableModel.STATUS_RESERVED
        };

        new AlertDialog.Builder(getContext())
                .setTitle("Đổi trạng thái")
                .setItems(labels, (d, i) -> {
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("status", values[i]);

                    FirebaseService.getInstance().updateTable(
                            table.getId(),
                            updates,
                            u -> {},
                            e -> Toast.makeText(getContext(),
                                    e.getMessage(),
                                    Toast.LENGTH_SHORT).show()
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

        new AlertDialog.Builder(getContext())
                .setTitle("Sửa thông tin bàn")
                .setView(layout)
                .setPositiveButton("Lưu", (d, w) -> {
                    int capacity = Integer.parseInt(
                            edtCapacity.getText().toString().trim());

                    if (capacity <= 0) {
                        Toast.makeText(getContext(),
                                "Số chỗ phải > 0",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("name", edtName.getText().toString().trim());
                    updates.put("capacity", capacity);

                    FirebaseService.getInstance().updateTable(
                            table.getId(),
                            updates,
                            u -> Toast.makeText(getContext(),
                                    "Cập nhật thành công",
                                    Toast.LENGTH_SHORT).show(),
                            e -> Toast.makeText(getContext(),
                                    e.getMessage(),
                                    Toast.LENGTH_SHORT).show()
                    );
                })
                .setNegativeButton("Huỷ", null)
                .show();
    }

    private void showDeleteTableDialog(TableModel table) {
        new AlertDialog.Builder(getContext())
                .setTitle("Xoá bàn")
                .setMessage("Bạn chắc chắn muốn xoá " + table.getName() + "?")
                .setPositiveButton("Xoá", (d, w) ->
                        FirebaseService.getInstance().deleteTable(
                                table.getId(),
                                u -> Toast.makeText(getContext(),
                                        "Đã xoá",
                                        Toast.LENGTH_SHORT).show(),
                                e -> Toast.makeText(getContext(),
                                        e.getMessage(),
                                        Toast.LENGTH_SHORT).show()
                        ))
                .setNegativeButton("Huỷ", null)
                .show();
    }
}
