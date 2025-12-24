package com.example.restaurantapp.fragments.staff;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.restaurantapp.R;
import com.example.restaurantapp.adapters.TableAdapter;
import com.example.restaurantapp.api.FirebaseService;
import com.example.restaurantapp.models.TableModel;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class StaffTablesFragment extends Fragment {

    private RecyclerView recyclerView;
    private TextView txtFreeTables, txtOccupiedTables, txtReservedTables;

    private TableAdapter adapter;
    private List<TableModel> tableList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_staff_tables, container, false);

        txtFreeTables = view.findViewById(R.id.txtFreeTables);
        txtOccupiedTables = view.findViewById(R.id.txtOccupiedTables);
        txtReservedTables = view.findViewById(R.id.txtReservedTables);

        recyclerView = view.findViewById(R.id.recyclerTables);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));

        adapter = new TableAdapter(tableList, this::showTableOptionsDialog);
        recyclerView.setAdapter(adapter);

        loadTables();

        return view;
    }


    private static final String TABLE_LISTENER_KEY = "staff_tables";

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Remove listener khi fragment không còn hiển thị
        FirebaseService.getInstance().removeTableListener(TABLE_LISTENER_KEY);
    }

    private void loadTables() {
        FirebaseService.getInstance().listenTablesRealtime(TABLE_LISTENER_KEY, (value, error) -> {
            if (error != null || value == null) return;

            tableList.clear();

            int free = 0;
            int occupied = 0;
            int reserved = 0;

            for (DocumentSnapshot doc : value.getDocuments()) {
                TableModel table = doc.toObject(TableModel.class);
                if (table == null) continue;

                table.setId(doc.getId());
                tableList.add(table);

                if ("free".equals(table.getStatus())) {
                    free++;
                } else if ("occupied".equals(table.getStatus())) {
                    occupied++;
                } else if ("reserved".equals(table.getStatus())) {
                    reserved++;
                }
            }

            adapter.notifyDataSetChanged();

            // 🔥 UPDATE TEXTVIEW
            txtFreeTables.setText(String.valueOf(free));
            txtOccupiedTables.setText(String.valueOf(occupied));
            txtReservedTables.setText(String.valueOf(reserved));
        });
    }


    private void showTableOptionsDialog(TableModel table) {
        String[] options = {"Trống", "Có khách"};
        new android.app.AlertDialog.Builder(getContext())
                .setTitle("Cập nhật trạng thái bàn: " + table.getName())
                .setItems(options, (dialog, which) -> {
                    String newStatus = which == 0 ? "free" : "occupied";
                    java.util.Map<String, Object> updates = new java.util.HashMap<>();
                    updates.put("status", newStatus);
                    FirebaseService.getInstance().updateTable(table.getId(), updates,
                            unused -> {},
                            e -> {});
                })
                .show();
    }
}
