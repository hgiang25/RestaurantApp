package com.example.restaurantapp.fragments.admin;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.restaurantapp.R;
import com.example.restaurantapp.adapters.PromotionAdapter;
import com.example.restaurantapp.api.FirebaseService;
import com.example.restaurantapp.models.PromotionModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminPromotionsFragment extends Fragment {

    private RecyclerView recyclerView;
    private PromotionAdapter adapter;
    private List<PromotionModel> promotionList = new ArrayList<>();
    private FloatingActionButton fabAdd;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_promotions, container, false);

        recyclerView = view.findViewById(R.id.recyclerPromotions);
        fabAdd = view.findViewById(R.id.fabAddPromotion);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new PromotionAdapter(promotionList, this::showPromotionDetail);
        recyclerView.setAdapter(adapter);

        fabAdd.setOnClickListener(v -> showAddPromotionDialog());

        loadPromotions();

        return view;
    }

    private void loadPromotions() {
        FirebaseService.getInstance().listenPromotionsRealtime((value, error) -> {
            if (error != null || value == null) return;

            promotionList.clear();
            for (DocumentSnapshot doc : value.getDocuments()) {
                PromotionModel promo = doc.toObject(PromotionModel.class);
                if (promo != null) {
                    promo.setId(doc.getId());
                    promotionList.add(promo);
                }
            }
            adapter.notifyDataSetChanged();
        });
    }

    private void showAddPromotionDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_promotion, null);

        EditText edtName = dialogView.findViewById(R.id.edtPromoName);
        EditText edtCode = dialogView.findViewById(R.id.edtVoucherCode);
        EditText edtDiscount = dialogView.findViewById(R.id.edtDiscountPercent);
        EditText edtMaxDiscount = dialogView.findViewById(R.id.edtMaxDiscount);
        EditText edtMinOrder = dialogView.findViewById(R.id.edtMinOrder);
        EditText edtExpiry = dialogView.findViewById(R.id.edtExpiryDate);
        SwitchMaterial switchActive = dialogView.findViewById(R.id.switchActive);

        final Calendar calendar = Calendar.getInstance();
        final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        edtExpiry.setOnClickListener(v -> {
            new DatePickerDialog(getContext(), (view, year, month, dayOfMonth) -> {
                calendar.set(year, month, dayOfMonth);
                edtExpiry.setText(sdf.format(calendar.getTime()));
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .create();

        dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.btnSave).setOnClickListener(v -> {
            String name = edtName.getText().toString().trim();
            String code = edtCode.getText().toString().trim().toUpperCase();
            String discountStr = edtDiscount.getText().toString().trim();
            String maxDiscountStr = edtMaxDiscount.getText().toString().trim();
            String minOrderStr = edtMinOrder.getText().toString().trim();

            if (name.isEmpty() || code.isEmpty() || discountStr.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> promoData = new HashMap<>();
            promoData.put("name", name);
            promoData.put("code", code);
            promoData.put("discountPercent", Double.parseDouble(discountStr));
            promoData.put("maxDiscount", maxDiscountStr.isEmpty() ? 0 : Double.parseDouble(maxDiscountStr));
            promoData.put("minOrderAmount", minOrderStr.isEmpty() ? 0 : Double.parseDouble(minOrderStr));
            promoData.put("validUntil", new Timestamp(calendar.getTime()));
            promoData.put("active", switchActive.isChecked());

            FirebaseService.getInstance().getDb().collection("promotions")
                    .add(promoData)
                    .addOnSuccessListener(ref -> {
                        Toast.makeText(getContext(), "Thêm khuyến mãi thành công", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
        });

        dialog.show();
    }

    private void showPromotionDetail(PromotionModel promotion) {
        new AlertDialog.Builder(getContext())
                .setTitle(promotion.getName())
                .setMessage(String.format("Mã: %s\nGiảm: %.0f%%\nTrạng thái: %s",
                        promotion.getCode(),
                        promotion.getDiscountPercent(),
                        promotion.isActive() ? "Đang hoạt động" : "Đã tắt"))
                .setPositiveButton("Đóng", null)
                .setNegativeButton("Xóa", (dialog, which) -> {
                    FirebaseService.getInstance().getDb().collection("promotions")
                            .document(promotion.getId())
                            .delete()
                            .addOnSuccessListener(unused ->
                                    Toast.makeText(getContext(), "Đã xóa", Toast.LENGTH_SHORT).show()
                            );
                })
                .show();
    }
}
