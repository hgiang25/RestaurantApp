package com.example.restaurantapp.fragments.customer;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.restaurantapp.R;
import com.example.restaurantapp.adapters.CartAdapter;
import com.example.restaurantapp.adapters.MenuAdapter;
import com.example.restaurantapp.api.FirebaseService;
import com.example.restaurantapp.models.MenuItem;
import com.example.restaurantapp.models.TableModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomerMenuFragment extends Fragment {

    private RecyclerView recyclerView;
    private ChipGroup chipGroupCategories;
    private MenuAdapter adapter;
    private List<MenuItem> menuList = new ArrayList<>();
    private List<MenuItem> filteredList = new ArrayList<>();
    private String selectedCategory = "Tất cả";

    // Giỏ hàng
    private Map<String, Integer> cart = new HashMap<>();
    
    // Voucher
    private DocumentSnapshot appliedVoucher = null;
    
    // Tables for dine-in
    private List<TableModel> tableList = new ArrayList<>();
    
    // Listener registration để remove khi destroy
    private com.google.firebase.firestore.ListenerRegistration menuListener;
    private com.google.firebase.firestore.ListenerRegistration tablesListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_customer_menu, container, false);

        recyclerView = view.findViewById(R.id.recyclerMenu);
        chipGroupCategories = view.findViewById(R.id.chipGroupCategories);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new MenuAdapter(filteredList, this::onAddToCart);
        recyclerView.setAdapter(adapter);

        view.findViewById(R.id.fabCart).setOnClickListener(v -> showCartDialog());

        loadMenu();

        return view;
    }

    private void loadMenu() {
        // Remove old listener first
        if (menuListener != null) {
            menuListener.remove();
        }
        
        menuListener = FirebaseService.getInstance().listenMenuRealtime((value, error) -> {
            if (error != null || value == null || !isAdded()) return;

            menuList.clear();
            List<String> categories = new ArrayList<>();
            categories.add("Tất cả");

            for (DocumentSnapshot doc : value.getDocuments()) {
                MenuItem item = doc.toObject(MenuItem.class);
                if (item != null) {
                    item.setId(doc.getId());
                    if (item.isAvailable()) {
                        menuList.add(item);
                        if (!categories.contains(item.getCategory())) {
                            categories.add(item.getCategory());
                        }
                    }
                }
            }

            setupCategoryChips(categories);
            filterByCategory();
        });
    }

    private void setupCategoryChips(List<String> categories) {
        chipGroupCategories.removeAllViews();
        for (String category : categories) {
            Chip chip = new Chip(getContext());
            chip.setText(category);
            chip.setCheckable(true);
            chip.setChecked(category.equals(selectedCategory));
            chip.setOnClickListener(v -> {
                selectedCategory = category;
                filterByCategory();
            });
            chipGroupCategories.addView(chip);
        }
    }

    private void filterByCategory() {
        filteredList.clear();
        if (selectedCategory.equals("Tất cả")) {
            filteredList.addAll(menuList);
        } else {
            for (MenuItem item : menuList) {
                if (item.getCategory().equals(selectedCategory)) {
                    filteredList.add(item);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void onAddToCart(MenuItem item) {
        int currentQty = cart.getOrDefault(item.getId(), 0);
        cart.put(item.getId(), currentQty + 1);
        Toast.makeText(getContext(), "Đã thêm " + item.getName() + " vào giỏ", Toast.LENGTH_SHORT).show();
    }

    private void showCartDialog() {
        if (cart.isEmpty()) {
            Toast.makeText(getContext(), "Giỏ hàng trống", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_cart, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // Views
        TextView tvItemCount = dialogView.findViewById(R.id.tvItemCount);
        TextView tvSubtotal = dialogView.findViewById(R.id.tvSubtotal);
        TextView tvDiscount = dialogView.findViewById(R.id.tvDiscount);
        TextView tvTotalPrice = dialogView.findViewById(R.id.tvTotalPrice);
        LinearLayout layoutDiscount = dialogView.findViewById(R.id.layoutDiscount);
        LinearLayout layoutVoucherApplied = dialogView.findViewById(R.id.layoutVoucherApplied);
        TextView tvVoucherInfo = dialogView.findViewById(R.id.tvVoucherInfo);
        TextInputEditText edtVoucherCode = dialogView.findViewById(R.id.edtVoucherCode);
        MaterialButton btnApplyVoucher = dialogView.findViewById(R.id.btnApplyVoucher);
        ImageButton btnRemoveVoucher = dialogView.findViewById(R.id.btnRemoveVoucher);
        RecyclerView recyclerCartItems = dialogView.findViewById(R.id.recyclerCartItems);
        MaterialButton btnClearCart = dialogView.findViewById(R.id.btnClearCart);
        MaterialButton btnPlaceOrder = dialogView.findViewById(R.id.btnPlaceOrder);
        
        // Order type views
        RadioGroup radioGroupOrderType = dialogView.findViewById(R.id.radioGroupOrderType);
        LinearLayout layoutTableSelection = dialogView.findViewById(R.id.layoutTableSelection);
        LinearLayout layoutAddressInput = dialogView.findViewById(R.id.layoutAddressInput);
        Spinner spinnerTables = dialogView.findViewById(R.id.spinnerTables);
        TextInputEditText edtDeliveryAddress = dialogView.findViewById(R.id.edtDeliveryAddress);
        TextInputEditText edtPhoneNumber = dialogView.findViewById(R.id.edtPhoneNumber);

        // Load tables for spinner
        loadTablesForSpinner(spinnerTables);

        // Order type radio button listener
        radioGroupOrderType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioDineIn) {
                layoutTableSelection.setVisibility(View.VISIBLE);
                layoutAddressInput.setVisibility(View.GONE);
            } else if (checkedId == R.id.radioTakeaway) {
                layoutTableSelection.setVisibility(View.GONE);
                layoutAddressInput.setVisibility(View.VISIBLE);
            }
        });

        // Build cart items list
        List<CartAdapter.CartItem> cartItems = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : cart.entrySet()) {
            for (MenuItem item : menuList) {
                if (item.getId().equals(entry.getKey())) {
                    CartAdapter.CartItem cartItem = new CartAdapter.CartItem(
                            item.getId(),
                            item.getName(),
                            item.getPrice(),
                            entry.getValue()
                    );
                    cartItems.add(cartItem);
                    break;
                }
            }
        }

        // Helper method to calculate subtotal
        final double[] subtotal = {0};
        final double[] discount = {0};
        
        Runnable updateTotals = () -> {
            subtotal[0] = 0;
            int itemCount = 0;
            for (CartAdapter.CartItem ci : cartItems) {
                subtotal[0] += ci.getTotal();
                itemCount++;
            }
            tvItemCount.setText(itemCount + " món");
            tvSubtotal.setText(String.format("%,.0fđ", subtotal[0]));
            
            // Calculate discount if voucher applied
            discount[0] = 0;
            if (appliedVoucher != null) {
                Double discountPercent = appliedVoucher.getDouble("discountPercent");
                Double maxDiscount = appliedVoucher.getDouble("maxDiscount");
                
                if (discountPercent != null) {
                    discount[0] = subtotal[0] * discountPercent / 100;
                    if (maxDiscount != null && maxDiscount > 0 && discount[0] > maxDiscount) {
                        discount[0] = maxDiscount;
                    }
                }
                
                layoutDiscount.setVisibility(View.VISIBLE);
                layoutVoucherApplied.setVisibility(View.VISIBLE);
                tvDiscount.setText(String.format("-%,.0fđ", discount[0]));
                
                String voucherInfoText = String.format("Giảm %.0f%%", discountPercent);
                if (maxDiscount != null && maxDiscount > 0) {
                    voucherInfoText += String.format(" - Tối đa %,.0fđ", maxDiscount);
                }
                tvVoucherInfo.setText(voucherInfoText);
            } else {
                layoutDiscount.setVisibility(View.GONE);
                layoutVoucherApplied.setVisibility(View.GONE);
            }
            
            double total = subtotal[0] - discount[0];
            tvTotalPrice.setText(String.format("%,.0fđ", total));
        };

        // Set initial data
        updateTotals.run();

        // Apply voucher
        btnApplyVoucher.setOnClickListener(v -> {
            String code = edtVoucherCode.getText().toString().trim();
            if (code.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng nhập mã giảm giá", Toast.LENGTH_SHORT).show();
                return;
            }
            
            FirebaseService.getInstance().validateVoucher(code, subtotal[0],
                    promo -> {
                        appliedVoucher = promo;
                        edtVoucherCode.setText("");
                        Toast.makeText(getContext(), "Áp dụng mã giảm giá thành công!", Toast.LENGTH_SHORT).show();
                        updateTotals.run();
                    },
                    e -> {
                        Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        // Remove voucher
        btnRemoveVoucher.setOnClickListener(v -> {
            appliedVoucher = null;
            Toast.makeText(getContext(), "Đã xóa mã giảm giá", Toast.LENGTH_SHORT).show();
            updateTotals.run();
        });

        // Setup RecyclerView
        recyclerCartItems.setLayoutManager(new LinearLayoutManager(getContext()));
        CartAdapter cartAdapter = new CartAdapter(cartItems, new CartAdapter.OnCartItemActionListener() {
            @Override
            public void onRemoveItem(CartAdapter.CartItem item) {
                cart.remove(item.menuId);
                int position = cartItems.indexOf(item);
                if (position >= 0) {
                    cartItems.remove(position);
                    recyclerCartItems.getAdapter().notifyItemRemoved(position);
                }
                if (cart.isEmpty()) {
                    dialog.dismiss();
                    Toast.makeText(getContext(), "Giỏ hàng trống", Toast.LENGTH_SHORT).show();
                } else {
                    updateTotals.run();
                }
            }

            @Override
            public void onQuantityChanged(CartAdapter.CartItem item, int newQuantity) {
                cart.put(item.menuId, newQuantity);
                item.quantity = newQuantity;
                int position = cartItems.indexOf(item);
                if (position >= 0) {
                    recyclerCartItems.getAdapter().notifyItemChanged(position);
                }
                updateTotals.run();
            }
        });
        recyclerCartItems.setAdapter(cartAdapter);

        // Clear cart button
        btnClearCart.setOnClickListener(v -> {
            cart.clear();
            appliedVoucher = null;
            dialog.dismiss();
            Toast.makeText(getContext(), "Đã xóa giỏ hàng", Toast.LENGTH_SHORT).show();
        });

        // Place order button
        btnPlaceOrder.setOnClickListener(v -> {
            // Validate order type selection
            int selectedOrderType = radioGroupOrderType.getCheckedRadioButtonId();
            
            if (selectedOrderType == R.id.radioDineIn) {
                // Dine-in - check table selection
                if (tableList.isEmpty()) {
                    Toast.makeText(getContext(), "Không có bàn trống. Vui lòng thử lại sau!", Toast.LENGTH_SHORT).show();
                    return;
                }
                int selectedPosition = spinnerTables.getSelectedItemPosition();
                if (selectedPosition < 0 || selectedPosition >= tableList.size()) {
                    Toast.makeText(getContext(), "Vui lòng chọn bàn!", Toast.LENGTH_SHORT).show();
                    return;
                }
                TableModel selectedTable = tableList.get(selectedPosition);
                dialog.dismiss();
                placeOrder("dine_in", selectedTable.getId(), selectedTable.getName(), null, null);
            } else if (selectedOrderType == R.id.radioTakeaway) {
                // Takeaway - check address
                String address = edtDeliveryAddress.getText().toString().trim();
                String phone = edtPhoneNumber.getText().toString().trim();
                
                if (address.isEmpty()) {
                    edtDeliveryAddress.setError("Vui lòng nhập địa chỉ");
                    return;
                }
                if (phone.isEmpty()) {
                    edtPhoneNumber.setError("Vui lòng nhập số điện thoại");
                    return;
                }
                dialog.dismiss();
                placeOrder("takeaway", null, null, address, phone);
            } else {
                Toast.makeText(getContext(), "Vui lòng chọn hình thức đặt hàng!", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }
    
    private void loadTablesForSpinner(Spinner spinnerTables) {
        // Remove old listener first
        if (tablesListener != null) {
            tablesListener.remove();
        }
        
        tablesListener = FirebaseService.getInstance().listenFreeTables((value, error) -> {
            if (error != null || value == null || !isAdded()) return;
            
            tableList.clear();
            List<String> tableNames = new ArrayList<>();
            
            for (DocumentSnapshot doc : value.getDocuments()) {
                TableModel table = doc.toObject(TableModel.class);
                if (table != null) {
                    table.setId(doc.getId());
                    tableList.add(table);
                    tableNames.add(table.getName() + " (" + table.getCapacity() + " chỗ)");
                }
            }
            
            if (getContext() != null) {
                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        getContext(),
                        android.R.layout.simple_spinner_item,
                        tableNames
                );
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerTables.setAdapter(adapter);
            }
        });
    }

    private void placeOrder(String orderType, String tableId, String tableName, String address, String phone) {
        String customerId = FirebaseService.getInstance().getCurrentUserId();
        if (customerId == null) {
            Toast.makeText(getContext(), "Vui lòng đăng nhập để đặt món!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Tính tổng tiền và giảm giá
        final double[] subtotal = {0};
        final double[] discount = {0};
        
        // Tạo danh sách items cho order
        List<Map<String, Object>> items = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : cart.entrySet()) {
            Map<String, Object> itemMap = new HashMap<>();
            itemMap.put("menuId", entry.getKey());
            itemMap.put("quantity", entry.getValue());
            
            // Tìm tên món
            for (MenuItem menuItem : menuList) {
                if (menuItem.getId().equals(entry.getKey())) {
                    itemMap.put("name", menuItem.getName());
                    itemMap.put("price", menuItem.getPrice());
                    subtotal[0] += menuItem.getPrice() * entry.getValue();
                    break;
                }
            }
            items.add(itemMap);
        }

        // Tính giảm giá nếu có voucher
        Map<String, Object> voucherData = null;
        if (appliedVoucher != null) {
            Double discountPercent = appliedVoucher.getDouble("discountPercent");
            Double maxDiscount = appliedVoucher.getDouble("maxDiscount");
            
            if (discountPercent != null) {
                discount[0] = subtotal[0] * discountPercent / 100;
                if (maxDiscount != null && maxDiscount > 0 && discount[0] > maxDiscount) {
                    discount[0] = maxDiscount;
                }
            }
            
            voucherData = new HashMap<>();
            voucherData.put("code", appliedVoucher.getString("code"));
            voucherData.put("name", appliedVoucher.getString("name"));
            voucherData.put("discountPercent", discountPercent);
            voucherData.put("discountAmount", discount[0]);
        }

        final double finalTotal = subtotal[0] - discount[0];
        final double finalSubtotal = subtotal[0];
        final double finalDiscount = discount[0];

        // Gọi createOrder với đầy params (bỏ update)
        Map<String, Object> voucherDataFinal = voucherData;
        FirebaseService.getInstance().createOrder(customerId, orderType, tableId, tableName, address, phone,
                items, finalSubtotal, finalDiscount, finalTotal, voucherDataFinal,
                docRef -> {
                    String successMsg = "dine_in".equals(orderType)
                            ? "Đặt món thành công! Bàn: " + tableName
                            : "Đặt món mang về thành công!";
                    Toast.makeText(getContext(), successMsg, Toast.LENGTH_SHORT).show();
                    cart.clear();
                    appliedVoucher = null;
                },
                e -> Toast.makeText(getContext(), "Lỗi đặt món: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Remove listeners để tránh memory leak và lag
        if (menuListener != null) {
            menuListener.remove();
            menuListener = null;
        }
        if (tablesListener != null) {
            tablesListener.remove();
            tablesListener = null;
        }
    }
}
