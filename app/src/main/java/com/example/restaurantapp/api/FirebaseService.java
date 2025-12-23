package com.example.restaurantapp.api;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.restaurantapp.models.NotificationModel;
import com.example.restaurantapp.models.TableModel;
import com.example.restaurantapp.models.User;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseService {

    private static final String TAG = "FirebaseService";

    private static FirebaseService instance;
    private final FirebaseAuth auth;
    private final FirebaseFirestore db;

    public static FirebaseService getInstance() {
        if (instance == null) instance = new FirebaseService();
        return instance;
    }

    private FirebaseService() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    public FirebaseFirestore getDb() {
        return db;
    }

    public String getCurrentUserId() {
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
    }

    /** =================== AUTH =================== */
    public void register(String email, String username, String pass, String role,
                         OnSuccessListener<Void> success, OnFailureListener fail) {
        auth.createUserWithEmailAndPassword(email, pass)
                .addOnSuccessListener(authResult -> {
                    String uid = auth.getCurrentUser().getUid();
                    User user = new User(email, username, role);
                    db.collection("users").document(uid)
                            .set(user)
                            .addOnSuccessListener(aVoid -> success.onSuccess(null))
                            .addOnFailureListener(fail);
                    auth.getCurrentUser().sendEmailVerification();
                })
                .addOnFailureListener(fail);
    }

    public void login(String email, String pass,
                      OnSuccessListener<Void> success, OnFailureListener fail) {
        auth.signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener(authResult -> success.onSuccess(null))
                .addOnFailureListener(fail);
    }

    public void logout() { auth.signOut(); }
    public boolean isLoggedIn() { return auth.getCurrentUser() != null; }

    /** =================== USERS =================== */
    public void getUser(String uid, OnSuccessListener<DocumentSnapshot> success, OnFailureListener fail) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(success)
                .addOnFailureListener(fail);
    }

    public void listenUserRealtime(String uid, EventListener<DocumentSnapshot> listener) {
        db.collection("users").document(uid).addSnapshotListener(listener);
    }

    public void updateUserRole(String uid, String role, OnSuccessListener<Void> success, OnFailureListener fail) {
        db.collection("users").document(uid).update("role", role)
                .addOnSuccessListener(success)
                .addOnFailureListener(fail);
    }

    public void listenUsersByRoleRealtime(String role, EventListener<QuerySnapshot> listener) {
        db.collection("users").whereEqualTo("role", role).addSnapshotListener(listener);
    }

    /** =================== TABLES =================== */

    private ListenerRegistration tableListener;

    /** Tạo bàn */
    public void createTable(String name, int capacity,
                            OnSuccessListener<DocumentReference> success,
                            OnFailureListener fail) {

        Map<String, Object> table = new HashMap<>();
        table.put("name", name);
        table.put("capacity", capacity);
        table.put("status", TableModel.STATUS_FREE);
        table.put("createdAt", FieldValue.serverTimestamp());
        table.put("updatedAt", FieldValue.serverTimestamp());

        db.collection("tables")
                .add(table)
                .addOnSuccessListener(success)
                .addOnFailureListener(fail);
    }

    /** Update bàn */
    public void updateTable(String tableId, Map<String, Object> updates,
                            OnSuccessListener<Void> success,
                            OnFailureListener fail) {

        updates.put("updatedAt", FieldValue.serverTimestamp());

        db.collection("tables")
                .document(tableId)
                .update(updates)
                .addOnSuccessListener(success)
                .addOnFailureListener(fail);
    }

    /** Xoá bàn */
    public void deleteTable(String tableId,
                            OnSuccessListener<Void> success,
                            OnFailureListener fail) {

        db.collection("tables")
                .document(tableId)
                .delete()
                .addOnSuccessListener(success)
                .addOnFailureListener(fail);
    }

    /** Listen realtime danh sách bàn */
    public void listenTablesRealtime(EventListener<QuerySnapshot> listener) {
        removeTableListener();

        tableListener = db.collection("tables")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .addSnapshotListener(listener);
    }

    /** Remove listener */
    public void removeTableListener() {
        if (tableListener != null) {
            tableListener.remove();
            tableListener = null;
        }
    }


    /** =================== MENU =================== */
    public void addMenuItem(String name, double price, String category, boolean available,
                            String imageUrl, Map<String, Object> options,
                            OnSuccessListener<DocumentReference> success,
                            OnFailureListener fail) {
        Map<String, Object> item = new HashMap<>();
        item.put("name", name);
        item.put("price", price);
        item.put("category", category);
        item.put("available", available);
        item.put("imageUrl", imageUrl);
        item.put("options", options); // size, toppings, combo...
        db.collection("menu").add(item)
                .addOnSuccessListener(success)
                .addOnFailureListener(fail);
    }

    public void updateMenuItem(String menuId, Map<String, Object> updates,
                               OnSuccessListener<Void> success, OnFailureListener fail) {
        db.collection("menu").document(menuId)
                .update(updates)
                .addOnSuccessListener(success)
                .addOnFailureListener(fail);
    }

    public void listenMenuRealtime(EventListener<QuerySnapshot> listener) {
        db.collection("menu").addSnapshotListener(listener);
    }

    /** =================== ORDERS =================== */
    public void createOrder(String customerId, String tableId, List<Map<String,Object>> items,
                            OnSuccessListener<DocumentReference> success, OnFailureListener fail) {
        Map<String, Object> order = new HashMap<>();
        order.put("customerId", customerId);
        order.put("tableId", tableId);
        order.put("items", items);
        order.put("status", "pending"); // pending, confirmed, preparing, served, paid
        order.put("createdAt", Timestamp.now());
        db.collection("orders").add(order)
                .addOnSuccessListener(success)
                .addOnFailureListener(fail);
    }

    public void updateOrderStatus(String orderId, String status,
                                  OnSuccessListener<Void> success, OnFailureListener fail) {
        db.collection("orders").document(orderId)
                .update("status", status)
                .addOnSuccessListener(success)
                .addOnFailureListener(fail);
    }

    public void listenOrdersByCustomerRealtime(String customerId, EventListener<QuerySnapshot> listener) {
        db.collection("orders").whereEqualTo("customerId", customerId)
                .addSnapshotListener(listener);
    }

    public void listenOrdersRealtime(EventListener<QuerySnapshot> listener) {
        db.collection("orders").addSnapshotListener(listener);
    }

    /** =================== REVIEWS =================== */
    public void addReview(String customerId, String menuId, int rating, String comment,
                          OnSuccessListener<DocumentReference> success, OnFailureListener fail) {
        Map<String, Object> review = new HashMap<>();
        review.put("customerId", customerId);
        review.put("menuId", menuId);
        review.put("rating", rating);
        review.put("comment", comment);
        review.put("createdAt", Timestamp.now());
        db.collection("reviews").add(review)
                .addOnSuccessListener(success)
                .addOnFailureListener(fail);
    }

    public void listenReviewsByMenuRealtime(String menuId, EventListener<QuerySnapshot> listener) {
        db.collection("reviews").whereEqualTo("menuId", menuId)
                .addSnapshotListener(listener);
    }

    public void listenReviewsByCustomerRealtime(String customerId, EventListener<QuerySnapshot> listener) {
        db.collection("reviews").whereEqualTo("customerId", customerId)
                .addSnapshotListener(listener);
    }

    /** =================== NOTIFICATIONS =================== */
    public void sendNotification(String userId, String message,
                                 OnSuccessListener<DocumentReference> success,
                                 OnFailureListener fail) {

        Map<String, Object> notif = new HashMap<>();
        notif.put("type", "personal");
        notif.put("targetUserId", userId);
        notif.put("message", message);
        notif.put("createdAt", Timestamp.now());

        db.collection("notifications").add(notif)
                .addOnSuccessListener(success)
                .addOnFailureListener(fail);
    }


    /** =================== NOTIFICATIONS (REALTIME) =================== */
    public ListenerRegistration listenNotificationsForUser(
            String userId,
            String role,
            java.util.function.Consumer<List<NotificationModel>> onUpdate
    ) {

        List<NotificationModel> personalList = new ArrayList<>();
        List<NotificationModel> broadcastList = new ArrayList<>();

        // 📩 Personal notifications
        ListenerRegistration personalListener =
                db.collection("notifications")
                        .whereEqualTo("type", "personal")
                        .whereEqualTo("targetUserId", userId)
                        .orderBy("createdAt", Query.Direction.DESCENDING)
                        .addSnapshotListener((value, error) -> {
                            if (error != null) {
                                Log.e(TAG, "Lỗi personal notifications: " + error.getMessage());
                                return;
                            }
                            if (value == null) return;
                            
                            Log.d(TAG, "Personal notifications: " + value.size());

                            personalList.clear();
                            for (DocumentSnapshot doc : value.getDocuments()) {
                                NotificationModel n = doc.toObject(NotificationModel.class);
                                if (n != null) {
                                    n.setId(doc.getId());
                                    personalList.add(n);
                                }
                            }

                            onUpdate.accept(mergeAndSort(personalList, broadcastList));
                        });

        // 📢 Broadcast notifications
        ListenerRegistration broadcastListener =
                db.collection("notifications")
                        .whereEqualTo("type", "broadcast")
                        .whereArrayContains("roles", role)
                        .orderBy("createdAt", Query.Direction.DESCENDING)
                        .addSnapshotListener((value, error) -> {
                            if (error != null) {
                                Log.e(TAG, "Lỗi broadcast notifications: " + error.getMessage());
                                return;
                            }
                            if (value == null) return;
                            
                            Log.d(TAG, "Broadcast notifications for role " + role + ": " + value.size());

                            broadcastList.clear();
                            for (DocumentSnapshot doc : value.getDocuments()) {
                                NotificationModel n = doc.toObject(NotificationModel.class);
                                if (n != null) {
                                    n.setId(doc.getId());
                                    broadcastList.add(n);
                                }
                            }

                            onUpdate.accept(mergeAndSort(personalList, broadcastList));
                        });

        return () -> {
            personalListener.remove();
            broadcastListener.remove();
        };
    }

    private List<NotificationModel> mergeAndSort(
            List<NotificationModel> personal,
            List<NotificationModel> broadcast
    ) {
        List<NotificationModel> merged = new ArrayList<>();
        merged.addAll(personal);
        merged.addAll(broadcast);

        Collections.sort(merged, (a, b) ->
                b.getCreatedAt().compareTo(a.getCreatedAt()));

        return merged;
    }


    // 2️⃣ Broadcast notification to roles
    public void broadcastNotification(String message, List<String> roles,
                                      OnSuccessListener<Void> success,
                                      OnFailureListener fail) {

        Map<String, Object> notif = new HashMap<>();
        notif.put("type", "broadcast");
        notif.put("roles", roles);
        notif.put("message", message);
        notif.put("createdAt", Timestamp.now());

        db.collection("notifications").add(notif)
                .addOnSuccessListener(r -> success.onSuccess(null))
                .addOnFailureListener(fail);
    }

    /** =================== INVENTORY / STOCK =================== */
    public void addStockItem(String name, double quantity, String unit, OnSuccessListener<DocumentReference> success, OnFailureListener fail) {
        Map<String,Object> item = new HashMap<>();
        item.put("name", name);
        item.put("quantity", quantity);
        item.put("unit", unit);
        item.put("createdAt", Timestamp.now());
        db.collection("inventory").add(item)
                .addOnSuccessListener(success)
                .addOnFailureListener(fail);
    }

    public void updateStockItem(String stockId, Map<String,Object> updates, OnSuccessListener<Void> success, OnFailureListener fail) {
        db.collection("inventory").document(stockId)
                .update(updates)
                .addOnSuccessListener(success)
                .addOnFailureListener(fail);
    }

    public void listenInventoryRealtime(EventListener<QuerySnapshot> listener) {
        db.collection("inventory").addSnapshotListener(listener);
    }

    /** =================== ATTENDANCE / TIMESHEET =================== */
    public void addAttendance(String staffId, String shift, Timestamp checkIn, OnSuccessListener<DocumentReference> success, OnFailureListener fail) {
        Map<String,Object> record = new HashMap<>();
        record.put("staffId", staffId);
        record.put("shift", shift);
        record.put("checkIn", checkIn);
        record.put("checkOut", null);
        db.collection("attendance").add(record)
                .addOnSuccessListener(success)
                .addOnFailureListener(fail);
    }

    public void updateAttendanceCheckOut(String attendanceId, Timestamp checkOut, OnSuccessListener<Void> success, OnFailureListener fail) {
        db.collection("attendance").document(attendanceId)
                .update("checkOut", checkOut)
                .addOnSuccessListener(success)
                .addOnFailureListener(fail);
    }

    public void listenAttendanceByStaffRealtime(String staffId, EventListener<QuerySnapshot> listener) {
        db.collection("attendance").whereEqualTo("staffId", staffId)
                .addSnapshotListener(listener);
    }

    /** =================== PROMOTIONS / DISCOUNTS =================== */
    public void addPromotion(String name, double discountPercent, Timestamp validUntil,
                             OnSuccessListener<DocumentReference> success, OnFailureListener fail) {
        Map<String,Object> promo = new HashMap<>();
        promo.put("name", name);
        promo.put("discountPercent", discountPercent);
        promo.put("validUntil", validUntil);
        db.collection("promotions").add(promo)
                .addOnSuccessListener(success)
                .addOnFailureListener(fail);
    }

    public void updatePromotion(String promoId, Map<String,Object> updates, OnSuccessListener<Void> success, OnFailureListener fail) {
        db.collection("promotions").document(promoId)
                .update(updates)
                .addOnSuccessListener(success)
                .addOnFailureListener(fail);
    }

    public void listenPromotionsRealtime(EventListener<QuerySnapshot> listener) {
        db.collection("promotions").addSnapshotListener(listener);
    }

    /** =================== REPORTS =================== */
    // Admin-only reports can just query the respective collections with filters
    public void listenOrdersReportRealtime(EventListener<QuerySnapshot> listener) {
        db.collection("orders").addSnapshotListener(listener);
    }

    public void listenRevenueReportRealtime(EventListener<QuerySnapshot> listener) {
        db.collection("orders")
                .whereIn("status", List.of("paid","served"))
                .addSnapshotListener(listener);
    }

    public void listenInventoryReportRealtime(EventListener<QuerySnapshot> listener) {
        db.collection("inventory").addSnapshotListener(listener);
    }

    public void listenMenuReportRealtime(EventListener<QuerySnapshot> listener) {
        db.collection("menu").addSnapshotListener(listener);
    }

    /** =================== ADVANCED FEATURES =================== */

    // 1️⃣ Auto deduct stock after order
    public void deductStockFromOrder(Map<String, Object> order,
                                     OnSuccessListener<Void> success, OnFailureListener fail) {
        List<Map<String, Object>> items = (List<Map<String, Object>>) order.get("items");

        WriteBatch batch = db.batch();
        for (Map<String,Object> menuItem : items) {
            List<Map<String,Object>> ingredients = (List<Map<String,Object>>) menuItem.get("ingredients");
            for (Map<String,Object> ing : ingredients) {
                String ingId = (String) ing.get("id");
                double usedQty = (double) ing.get("quantity");
                DocumentReference stockRef = db.collection("inventory").document(ingId);
                batch.update(stockRef, "quantity", FieldValue.increment(-usedQty));
            }
        }

        batch.commit().addOnSuccessListener(success).addOnFailureListener(fail);
    }




    // 3️⃣ Loyalty points
    public void addLoyaltyPoints(String customerId, int points,
                                 OnSuccessListener<Void> success, OnFailureListener fail) {
        db.collection("users").document(customerId)
                .update("loyaltyPoints", FieldValue.increment(points))
                .addOnSuccessListener(success)
                .addOnFailureListener(fail);
    }

    public void redeemLoyaltyPoints(String customerId, int points,
                                    OnSuccessListener<Void> success, OnFailureListener fail) {
        db.collection("users").document(customerId)
                .update("loyaltyPoints", FieldValue.increment(-points))
                .addOnSuccessListener(success)
                .addOnFailureListener(fail);
    }

    // 4️⃣ Apply voucher
    public void applyVoucherToOrder(String orderId, String voucherCode,
                                    OnSuccessListener<Void> success, OnFailureListener fail) {
        db.collection("vouchers").whereEqualTo("code", voucherCode).get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot voucher = querySnapshot.getDocuments().get(0);
                        db.collection("orders").document(orderId)
                                .update("voucher", voucher.getData())
                                .addOnSuccessListener(success)
                                .addOnFailureListener(fail);
                    } else fail.onFailure(new Exception("Voucher not found"));
                }).addOnFailureListener(fail);
    }

    // 5️⃣ Audit logs
    public void logAction(String userId, String action, String target,
                          Map<String,Object> extra, OnSuccessListener<DocumentReference> success, OnFailureListener fail) {
        Map<String,Object> log = new HashMap<>();
        log.put("userId", userId);
        log.put("action", action);
        log.put("target", target);
        log.put("extra", extra);
        log.put("createdAt", Timestamp.now());
        db.collection("audit_logs").add(log)
                .addOnSuccessListener(success)
                .addOnFailureListener(fail);
    }

    // 6️⃣ Realtime KPI / Reports
    public ListenerRegistration listenTopSellingMenu(int limit, EventListener<QuerySnapshot> listener) {
        return db.collection("menu")
                .orderBy("soldCount", Query.Direction.DESCENDING)
                .limit(limit)
                .addSnapshotListener(listener);
    }

    public ListenerRegistration listenRevenueByPeriod(String status, Timestamp start, Timestamp end, EventListener<QuerySnapshot> listener) {
        return db.collection("orders")
                .whereEqualTo("status", status)
                .whereGreaterThanOrEqualTo("createdAt", start)
                .whereLessThanOrEqualTo("createdAt", end)
                .addSnapshotListener(listener);
    }
}
