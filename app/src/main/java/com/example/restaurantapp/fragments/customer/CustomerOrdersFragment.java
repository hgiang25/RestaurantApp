package com.example.restaurantapp.fragments.customer;

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.restaurantapp.R;
import com.example.restaurantapp.adapters.CustomerOrderAdapter;
import com.example.restaurantapp.api.FirebaseService;
import com.example.restaurantapp.models.OrderModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class CustomerOrdersFragment extends Fragment implements CustomerOrderAdapter.OnPaymentRequestListener {

    private static final String TAG = "CustomerOrders";
    private RecyclerView recyclerView;
    private TextView txtEmpty;
    private CustomerOrderAdapter adapter;
    private List<OrderModel> orderList = new ArrayList<>();
    private ListenerRegistration ordersListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_customer_orders, container, false);

        recyclerView = view.findViewById(R.id.recyclerOrders);
        txtEmpty = view.findViewById(R.id.txtEmpty);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new CustomerOrderAdapter(orderList, this);
        recyclerView.setAdapter(adapter);

        loadOrders();

        return view;
    }

    @Override
    public void onPaymentRequest(OrderModel order) {
        showPaymentDialog(order);
    }

    private void showPaymentDialog(OrderModel order) {
        if (getContext() == null) return;

        Dialog dialog = new Dialog(getContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_payment);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        // Find views
        ImageButton btnClose = dialog.findViewById(R.id.btnClose);
        TextView tvOrderId = dialog.findViewById(R.id.tvOrderId);
        TextView tvTableName = dialog.findViewById(R.id.tvTableName);
        LinearLayout layoutTableInfo = dialog.findViewById(R.id.layoutTableInfo);
        LinearLayout layoutSubtotal = dialog.findViewById(R.id.layoutSubtotal);
        LinearLayout layoutDiscount = dialog.findViewById(R.id.layoutDiscount);
        TextView tvSubtotal = dialog.findViewById(R.id.tvSubtotal);
        TextView tvDiscount = dialog.findViewById(R.id.tvDiscount);
        TextView tvTotalAmount = dialog.findViewById(R.id.tvTotalAmount);
        RadioGroup radioGroupPaymentMethod = dialog.findViewById(R.id.radioGroupPaymentMethod);
        CardView cardBankInfo = dialog.findViewById(R.id.cardBankInfo);
        TextView tvTransferContent = dialog.findViewById(R.id.tvTransferContent);
        TextInputEditText edtNote = dialog.findViewById(R.id.edtNote);
        MaterialButton btnCancel = dialog.findViewById(R.id.btnCancel);
        MaterialButton btnRequestPayment = dialog.findViewById(R.id.btnRequestPayment);

        // Set order info
        String shortId = order.getId().substring(0, Math.min(8, order.getId().length()));
        tvOrderId.setText("#" + shortId);
        tvTransferContent.setText("Nội dung CK: DH_" + shortId);

        // Table info
        if (order.getTableName() != null && !order.getTableName().isEmpty()) {
            tvTableName.setText(order.getTableName());
            layoutTableInfo.setVisibility(View.VISIBLE);
        } else if ("takeaway".equals(order.getOrderType())) {
            tvTableName.setText("Mang đi");
            layoutTableInfo.setVisibility(View.VISIBLE);
        } else {
            layoutTableInfo.setVisibility(View.GONE);
        }

        // Price info
        Double subtotal = order.getSubtotal();
        Double discount = order.getDiscount();
        Double total = order.getTotal();

        if (subtotal != null && discount != null && discount > 0) {
            layoutSubtotal.setVisibility(View.VISIBLE);
            layoutDiscount.setVisibility(View.VISIBLE);
            tvSubtotal.setText(String.format("%,.0fđ", subtotal));
            tvDiscount.setText(String.format("-%,.0fđ", discount));
        } else {
            layoutSubtotal.setVisibility(View.GONE);
            layoutDiscount.setVisibility(View.GONE);
        }

        if (total != null) {
            tvTotalAmount.setText(String.format("%,.0fđ", total));
        } else {
            tvTotalAmount.setText("0đ");
        }

        // Radio group listener - show bank info when bank transfer selected
        radioGroupPaymentMethod.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioBankTransfer) {
                cardBankInfo.setVisibility(View.VISIBLE);
            } else {
                cardBankInfo.setVisibility(View.GONE);
            }
        });

        // Close button
        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        // Request payment button
        btnRequestPayment.setOnClickListener(v -> {
            String paymentMethod;
            int selectedId = radioGroupPaymentMethod.getCheckedRadioButtonId();

            if (selectedId == R.id.radioCash) {
                paymentMethod = "cash";
            } else if (selectedId == R.id.radioBankTransfer) {
                paymentMethod = "bank_transfer";
            } else if (selectedId == R.id.radioEWallet) {
                paymentMethod = "e_wallet";
            } else if (selectedId == R.id.radioCard) {
                paymentMethod = "card";
            } else {
                paymentMethod = "cash";
            }

            String note = edtNote.getText() != null ? edtNote.getText().toString().trim() : "";

            // Disable button to prevent double click
            btnRequestPayment.setEnabled(false);
            btnRequestPayment.setText("Đang xử lý...");

            FirebaseService.getInstance().requestPayment(order.getId(), paymentMethod, note,
                    aVoid -> {
                        Toast.makeText(getContext(), "Đã gọi thanh toán! Nhân viên sẽ đến phục vụ.", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    },
                    e -> {
                        Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        btnRequestPayment.setEnabled(true);
                        btnRequestPayment.setText("Gọi thanh toán");
                    });
        });

        dialog.show();
    }

    private void loadOrders() {
        String customerId = FirebaseService.getInstance().getCurrentUserId();
        if (customerId == null) {
            Log.e(TAG, "customerId is null!");
            return;
        }

        // Hủy listener cũ nếu có
        if (ordersListener != null) {
            ordersListener.remove();
        }

        Log.d(TAG, "Bắt đầu listen orders cho customer: " + customerId);

        ordersListener = FirebaseService.getInstance().listenOrdersByCustomerRealtime(customerId, (value, error) -> {
            if (error != null) {
                Log.e(TAG, "Lỗi listen orders: " + error.getMessage());
                return;
            }
            if (value == null) {
                Log.w(TAG, "Value is null");
                return;
            }
            if (!isAdded()) {
                Log.w(TAG, "Fragment not attached");
                return;
            }

            // Log metadata để biết data từ cache hay server
            boolean fromCache = value.getMetadata().isFromCache();
            Log.d(TAG, "Nhận " + value.size() + " orders (fromCache: " + fromCache + ")");

            orderList.clear();
            for (DocumentSnapshot doc : value.getDocuments()) {
                OrderModel order = doc.toObject(OrderModel.class);
                if (order != null) {
                    order.setId(doc.getId());
                    
                    // Fix: Lấy items trực tiếp từ document vì toObject không map đúng List<Map>
                    Object itemsObj = doc.get("items");
                    if (itemsObj instanceof List) {
                        order.setRawItems((List<?>) itemsObj);
                    }
                    
                    orderList.add(order);
                    Log.d(TAG, "Order: " + doc.getId() + " - Status: " + order.getStatus());
                }
            }

            // Sắp xếp theo thời gian tạo (mới nhất trước)
            orderList.sort((o1, o2) -> {
                if (o1.getCreatedAt() == null || o2.getCreatedAt() == null) return 0;
                return o2.getCreatedAt().compareTo(o1.getCreatedAt());
            });

            if (orderList.isEmpty()) {
                txtEmpty.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                txtEmpty.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            }

            adapter.notifyDataSetChanged();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (ordersListener != null) {
            ordersListener.remove();
            ordersListener = null;
        }
    }
}
