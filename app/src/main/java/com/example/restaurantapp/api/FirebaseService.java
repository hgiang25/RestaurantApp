package com.example.restaurantapp.api;

import android.net.Uri;
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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;


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
    private FirebaseStorage storage;

    public static FirebaseService getInstance() {
        if (instance == null) instance = new FirebaseService();
        return instance;
    }

    private FirebaseService() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
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

    public ListenerRegistration listenUserRealtimeWithReg(String uid, EventListener<DocumentSnapshot> listener) {
        return db.collection("users").document(uid).addSnapshotListener(listener);
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

    /** =================== TABLE IMAGES =================== */
    public void uploadTableImage(Uri imageUri, String filename,
                                 OnSuccessListener<String> success,
                                 OnFailureListener fail) {
        StorageReference ref = storage.getReference().child("table_images/" + filename);

        ref.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> ref.getDownloadUrl()
                        .addOnSuccessListener(uri -> success.onSuccess(uri.toString()))
                        .addOnFailureListener(fail))
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

    /** =================== TABLES =================== */

    private final Map<String, ListenerRegistration> tableListeners = new HashMap<>();

    /** Listen realtime danh sách bàn với key để tách listener */
    public ListenerRegistration listenTablesRealtime(String key, EventListener<QuerySnapshot> listener) {
        removeTableListener(key); // nếu listener cùng key đã tồn tại thì remove

        ListenerRegistration registration = db.collection("tables")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .addSnapshotListener(listener);

        tableListeners.put(key, registration);
        return registration;
    }

    /** Remove listener theo key */
    public void removeTableListener(String key) {
        ListenerRegistration reg = tableListeners.get(key);
        if (reg != null) {
            reg.remove();
            tableListeners.remove(key);
        }
    }





    /** =================== MENU =================== */
    public void addMenuItem(String name, double price, String category, boolean available,
                            String imageUrl, Map<String, Object> options,
                            OnSuccessListener refSuccess,
                            OnFailureListener fail) {
        Map<String, Object> item = new HashMap<>();
        item.put("name", name);
        item.put("price", price);
        item.put("category", category);
        item.put("available", available);
        item.put("imageUrl", imageUrl);
        item.put("options", options); // size, toppings, combo...

        db.collection("menu").add(item)
                .addOnSuccessListener(refSuccess)
                .addOnFailureListener(fail);
    }

    public void updateMenuItem(String menuId, Map<String, Object> updates,
                               OnSuccessListener<Void> success, OnFailureListener fail) {
        db.collection("menu").document(menuId)
                .update(updates)
                .addOnSuccessListener(success)
                .addOnFailureListener(fail);
    }

    public void deleteMenuItem(String menuId, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        db.collection("menu").document(menuId)
                .delete()
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }


    public ListenerRegistration listenMenuRealtime(EventListener<QuerySnapshot> listener) {
        return db.collection("menu").addSnapshotListener(listener);
    }

    /** =================== Upload image =================== */
    public void uploadMenuImage(Uri imageUri, String filename,
                                OnSuccessListener<String> success, OnFailureListener fail) {
        StorageReference ref = storage.getReference().child("menu_images/" + filename);

        ref.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot ->
                        ref.getDownloadUrl().addOnSuccessListener(uri -> {
                            // Trả về URL public của ảnh
                            success.onSuccess(uri.toString());
                        }).addOnFailureListener(fail))
                .addOnFailureListener(fail);
    }

    /** =================== ORDERS =================== */
    public void createOrder(String customerId,
                            String orderType,
                            String tableId,
                            String tableName,
                            String deliveryAddress,
                            String deliveryPhone,
                            List<Map<String, Object>> items,
                            double subtotal,
                            double discount,
                            double total,
                            Map<String, Object> voucher,
                            int pointsUsed,
                            double pointsDiscount,
                            OnSuccessListener<DocumentReference> success,
                            OnFailureListener fail) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userRef = db.collection("users").document(customerId);
        DocumentReference orderRef = db.collection("orders").document();

        db.runTransaction(transaction -> {

                    // ===== LẤY USER =====
                    DocumentSnapshot userSnap = transaction.get(userRef);
                    Long currentPoints = userSnap.getLong("loyaltyPoints");
                    if (currentPoints == null) currentPoints = 0L;

                    // ❌ Không đủ điểm
                    if (pointsUsed > currentPoints) {
                        throw new RuntimeException("Không đủ điểm tích lũy");
                    }

                    // ===== TRỪ ĐIỂM =====
                    transaction.update(userRef,
                            "loyaltyPoints", currentPoints - pointsUsed);

                    // ===== TẠO ORDER =====
                    Map<String, Object> order = new HashMap<>();
                    order.put("customerId", customerId);
                    order.put("orderType", orderType);
                    order.put("items", items);
                    order.put("status", "pending");
                    order.put("createdAt", Timestamp.now());

                    order.put("subtotal", subtotal);
                    order.put("discount", discount);
                    order.put("pointsUsed", pointsUsed);
                    order.put("pointsDiscount", pointsDiscount);
                    order.put("pointsRefunded", false); // ✅ thêm
                    order.put("total", total);

                    if (voucher != null) {
                        order.put("voucher", voucher);
                    }

                    if ("dine_in".equals(orderType)) {
                        if (tableId != null) order.put("tableId", tableId);
                        if (tableName != null) order.put("tableName", tableName);
                    } else if ("takeaway".equals(orderType)) {
                        if (deliveryAddress != null) order.put("deliveryAddress", deliveryAddress);
                        if (deliveryPhone != null) order.put("deliveryPhone", deliveryPhone);
                    }

                    transaction.set(orderRef, order);

                    return orderRef;
                })
                .addOnSuccessListener(success)
                .addOnFailureListener(fail);
    }

    public void cancelOrderWithRefund(
            String orderId,
            String customerId,
            String reason,
            OnSuccessListener<Void> success,
            OnFailureListener failure
    ) {
        FirebaseFirestore db = getDb();

        DocumentReference orderRef = db.collection("orders").document(orderId);
        DocumentReference userRef = db.collection("users").document(customerId);

        db.runTransaction(transaction -> {

                    DocumentSnapshot orderSnap = transaction.get(orderRef);
                    if (!orderSnap.exists()) {
                        throw new RuntimeException("Order not found");
                    }

                    String status = orderSnap.getString("status");
                    Long usedPoints = orderSnap.getLong("pointsUsed"); // ✅ đúng field
                    Boolean refunded = orderSnap.getBoolean("pointsRefunded");

                    // ❌ Không cho hủy nếu đã thanh toán
                    if ("paid".equals(status)) {
                        throw new RuntimeException("Đơn hàng đã thanh toán");
                    }

                    // ❌ Không hoàn điểm nếu đang chuẩn bị
                    if ("preparing".equals(status)) {
                        throw new RuntimeException("Đơn hàng đang được chuẩn bị, không hoàn điểm");
                    }

                    Map<String, Object> orderUpdate = new HashMap<>();
                    orderUpdate.put("status", "cancelled");
                    orderUpdate.put("cancelledAt", System.currentTimeMillis());
                    orderUpdate.put("updatedAt", System.currentTimeMillis());

                    if (reason != null && !reason.isEmpty()) {
                        orderUpdate.put("cancelReason", reason);
                    }

                    // ✅ HOÀN ĐIỂM
                    if (usedPoints != null && usedPoints > 0 && !Boolean.TRUE.equals(refunded)) {

                        DocumentSnapshot userSnap = transaction.get(userRef);
                        Long currentPoints = userSnap.getLong("loyaltyPoints");
                        if (currentPoints == null) currentPoints = 0L;

                        transaction.update(
                                userRef,
                                "loyaltyPoints",
                                currentPoints + usedPoints
                        );

                        orderUpdate.put("pointsRefunded", true);
                    }

                    transaction.update(orderRef, orderUpdate);
                    return null;
                })
                .addOnSuccessListener(unused -> {
                    if (success != null) success.onSuccess(null);
                })
                .addOnFailureListener(failure);
    }





    public void updateUserLoyaltyPoints(String userId, int delta) {
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .update("loyaltyPoints", FieldValue.increment(delta));
    }


    public void updateOrderStatus(String orderId, String status,
                                  OnSuccessListener<Void> success, OnFailureListener fail) {
        db.collection("orders").document(orderId)
                .update("status", status)
                .addOnSuccessListener(success)
                .addOnFailureListener(fail);
    }

    public void requestPayment(String orderId, String paymentMethod, String note,
                               OnSuccessListener<Void> success, OnFailureListener fail) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("paymentStatus", "pending");
        updates.put("paymentMethod", paymentMethod);
        updates.put("paymentNote", note);
        updates.put("paymentRequestedAt", Timestamp.now());
        
        db.collection("orders").document(orderId)
                .update(updates)
                .addOnSuccessListener(success)
                .addOnFailureListener(fail);
    }

    public void confirmPayment(String orderId, 
                               OnSuccessListener<Void> success, OnFailureListener fail) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("paymentStatus", "completed");
        updates.put("status", "paid");
        updates.put("paidAt", Timestamp.now());
        
        db.collection("orders").document(orderId)
                .update(updates)
                .addOnSuccessListener(success)
                .addOnFailureListener(fail);
    }

    public ListenerRegistration listenOrdersByCustomerRealtime(String customerId, EventListener<QuerySnapshot> listener) {
        // Bỏ orderBy để không cần composite index - sort ở client side
        return db.collection("orders")
                .whereEqualTo("customerId", customerId)
                .addSnapshotListener(MetadataChanges.INCLUDE, listener);
    }

    public ListenerRegistration listenOrdersRealtime(EventListener<QuerySnapshot> listener) {
        return db.collection("orders")
                .addSnapshotListener(MetadataChanges.INCLUDE, listener);
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
    public void sendNotification(String userId, String title, String message,
                                 OnSuccessListener<DocumentReference> success,
                                 OnFailureListener fail) {

        Map<String, Object> notif = new HashMap<>();
        notif.put("type", "personal");
        notif.put("targetUserId", userId);
        notif.put("title", title);
        notif.put("message", message);
        notif.put("createdAt", Timestamp.now());

        db.collection("notifications").add(notif)
                .addOnSuccessListener(success)
                .addOnFailureListener(fail);
    }

    /** Đánh dấu notification đã đọc */
    public void markNotificationAsRead(String notificationId, 
                                       OnSuccessListener<Void> success,
                                       OnFailureListener fail) {
        String userId = getCurrentUserId();
        if (userId == null) return;

        Map<String, Object> data = new HashMap<>();
        data.put("readAt", Timestamp.now());

        db.collection("users").document(userId)
                .collection("readNotifications").document(notificationId)
                .set(data)
                .addOnSuccessListener(success)
                .addOnFailureListener(fail);
    }

    /** Lấy danh sách notification đã đọc */
    public void getReadNotificationIds(java.util.function.Consumer<java.util.Set<String>> onResult) {
        String userId = getCurrentUserId();
        if (userId == null) {
            onResult.accept(new java.util.HashSet<>());
            return;
        }

        db.collection("users").document(userId)
                .collection("readNotifications")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    java.util.Set<String> readIds = new java.util.HashSet<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        readIds.add(doc.getId());
                    }
                    onResult.accept(readIds);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting read notifications", e);
                    onResult.accept(new java.util.HashSet<>());
                });
    }

    /** Listen realtime danh sách notification đã đọc */
    public ListenerRegistration listenReadNotifications(java.util.function.Consumer<java.util.Set<String>> onUpdate) {
        String userId = getCurrentUserId();
        if (userId == null) return null;

        return db.collection("users").document(userId)
                .collection("readNotifications")
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) {
                        onUpdate.accept(new java.util.HashSet<>());
                        return;
                    }
                    java.util.Set<String> readIds = new java.util.HashSet<>();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        readIds.add(doc.getId());
                    }
                    onUpdate.accept(readIds);
                });
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
    public void broadcastNotification(String title, String message, List<String> roles,
                                      OnSuccessListener<Void> success,
                                      OnFailureListener fail) {

        Map<String, Object> notif = new HashMap<>();
        notif.put("type", "broadcast");
        notif.put("roles", roles);
        notif.put("title", title);
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

    public ListenerRegistration listenInventoryRealtime(EventListener<QuerySnapshot> listener) {
        return db.collection("inventory")
                .addSnapshotListener(MetadataChanges.INCLUDE, listener);
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

    // Trong FirebaseService.java
    // Trong FirebaseService.java

    // Trong FirebaseService.java
    public void updateLoyaltyPoints(String customerId, double orderTotal, OnSuccessListener<Void> successListener) {
        if (customerId == null || customerId.isEmpty()) {
            android.util.Log.e("LOYALTY_DEBUG", "Không thể cộng điểm: customerId bị NULL");
            return;
        }

        int pointsToAdd = User.calculatePointsFromBill(orderTotal);
        if (pointsToAdd <= 0) {
            android.util.Log.d("LOYALTY_DEBUG", "Số tiền nhỏ hơn 10k, không có điểm để cộng");
            return;
        }

        android.util.Log.d("LOYALTY_DEBUG", "Đang cộng " + pointsToAdd + " điểm cho: " + customerId);

        db.collection("users").document(customerId)
                .update("loyaltyPoints", FieldValue.increment(pointsToAdd))
                .addOnSuccessListener(aVoid -> {
                    android.util.Log.d("LOYALTY_DEBUG", "✅ Đã cập nhật điểm lên Firebase thành công");
                    if (successListener != null) successListener.onSuccess(aVoid);
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("LOYALTY_DEBUG", "❌ Lỗi Firebase: " + e.getMessage());
                    // Nếu lỗi "No document to update", có thể do ID user sai
                });
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

    // Validate voucher code
    public void validateVoucher(String voucherCode, double orderTotal,
                                OnSuccessListener<DocumentSnapshot> success,
                                OnFailureListener fail) {
        db.collection("promotions")
                .whereEqualTo("code", voucherCode.toUpperCase())
                .whereEqualTo("active", true)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        fail.onFailure(new Exception("Mã giảm giá không tồn tại hoặc đã hết hạn"));
                        return;
                    }
                    
                    DocumentSnapshot promo = querySnapshot.getDocuments().get(0);
                    
                    // Check expiry
                    Timestamp validUntil = promo.getTimestamp("validUntil");
                    if (validUntil != null && validUntil.toDate().before(new java.util.Date())) {
                        fail.onFailure(new Exception("Mã giảm giá đã hết hạn"));
                        return;
                    }
                    
                    // Check min order amount
                    Double minOrder = promo.getDouble("minOrderAmount");
                    if (minOrder != null && orderTotal < minOrder) {
                        fail.onFailure(new Exception("Đơn hàng tối thiểu " + String.format("%,.0f", minOrder) + "đ để sử dụng mã này"));
                        return;
                    }
                    
                    success.onSuccess(promo);
                })
                .addOnFailureListener(fail);
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

    /** =================== RESERVATIONS =================== */
    public void createReservation(Map<String, Object> reservation,
                                  OnSuccessListener<DocumentReference> success,
                                  OnFailureListener fail) {
        db.collection("reservations").add(reservation)
                .addOnSuccessListener(success)
                .addOnFailureListener(fail);
    }

    public void updateReservation(String reservationId, Map<String, Object> updates,
                                  OnSuccessListener<Void> success, OnFailureListener fail) {
        db.collection("reservations").document(reservationId).update(updates)
                .addOnSuccessListener(success)
                .addOnFailureListener(fail);
    }

    public ListenerRegistration listenReservationsByCustomer(String customerId, EventListener<QuerySnapshot> listener) {
        return db.collection("reservations")
                .whereEqualTo("customerId", customerId)
                .addSnapshotListener(listener);
    }

    public ListenerRegistration listenAllReservations(EventListener<QuerySnapshot> listener) {
        return db.collection("reservations")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener(listener);
    }

    public ListenerRegistration listenReservationsByStatus(String status, EventListener<QuerySnapshot> listener) {
        return db.collection("reservations")
                .whereEqualTo("status", status)
                .addSnapshotListener(listener);
    }

    public void checkTableAvailability(String tableId, String date, String time,
                                       OnSuccessListener<QuerySnapshot> success,
                                       OnFailureListener fail) {
        db.collection("reservations")
                .whereEqualTo("tableId", tableId)
                .whereEqualTo("date", date)
                .whereIn("status", java.util.Arrays.asList("pending", "confirmed"))
                .get()
                .addOnSuccessListener(success)
                .addOnFailureListener(fail);
    }

    public ListenerRegistration listenFreeTables(EventListener<QuerySnapshot> listener) {
        return db.collection("tables")
                .whereEqualTo("status", "free")
                .addSnapshotListener(listener);
    }

    /** =================== RECIPES =================== */

    /**
     * Thêm công thức mới
     */
    public void addRecipe(Map<String, Object> recipeData,
                          OnSuccessListener<DocumentReference> success,
                          OnFailureListener fail) {
        recipeData.put("createdAt", Timestamp.now());
        db.collection("recipes").add(recipeData)
                .addOnSuccessListener(success)
                .addOnFailureListener(fail);
    }

    /**
     * Cập nhật công thức
     */
    public void updateRecipe(String recipeId, Map<String, Object> updates,
                             OnSuccessListener<Void> success, OnFailureListener fail) {
        updates.put("updatedAt", Timestamp.now());
        db.collection("recipes").document(recipeId)
                .update(updates)
                .addOnSuccessListener(success)
                .addOnFailureListener(fail);
    }

    /**
     * Xóa công thức
     */
    public void deleteRecipe(String recipeId,
                             OnSuccessListener<Void> success,
                             OnFailureListener fail) {
        db.collection("recipes").document(recipeId)
                .delete()
                .addOnSuccessListener(success)
                .addOnFailureListener(fail);
    }

    /**
     * Lắng nghe realtime danh sách công thức
     */
    public ListenerRegistration listenRecipesRealtime(EventListener<QuerySnapshot> listener) {
        return db.collection("recipes").addSnapshotListener(listener);
    }

    /**
     * Lấy công thức theo menu item ID
     */
    public void getRecipeByMenuItemId(String menuItemId,
                                      OnSuccessListener<QuerySnapshot> success,
                                      OnFailureListener fail) {
        db.collection("recipes")
                .whereEqualTo("menuItemId", menuItemId)
                .get()
                .addOnSuccessListener(success)
                .addOnFailureListener(fail);
    }

    /**
     * Cập nhật trạng thái món ăn dựa trên nguyên liệu
     * Gọi method này khi cần đồng bộ hàng loạt
     */
    public void updateMenuItemAvailability(String menuItemId, boolean available,
                                           OnSuccessListener<Void> success,
                                           OnFailureListener fail) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("available", available);
        updates.put("lastStockCheck", Timestamp.now());

        db.collection("menu").document(menuItemId)
                .update(updates)
                .addOnSuccessListener(success)
                .addOnFailureListener(fail);
    }

    /**
     * Trừ nguyên liệu khi đơn hàng được xác nhận
     * Dựa trên công thức của từng món
     */
    public void deductIngredientsFromRecipe(String menuItemId, int quantity,
                                            OnSuccessListener<Void> success,
                                            OnFailureListener fail) {
        Log.d(TAG, "deductIngredientsFromRecipe: menuItemId=" + menuItemId + ", quantity=" + quantity);
        
        getRecipeByMenuItemId(menuItemId,
                querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        Log.d(TAG, "Không tìm thấy công thức cho menuItemId: " + menuItemId);
                        success.onSuccess(null); // Không có công thức thì bỏ qua
                        return;
                    }

                    DocumentSnapshot recipeDoc = querySnapshot.getDocuments().get(0);
                    List<Map<String, Object>> ingredients =
                            (List<Map<String, Object>>) recipeDoc.get("ingredients");

                    if (ingredients == null || ingredients.isEmpty()) {
                        Log.d(TAG, "Công thức không có nguyên liệu: " + menuItemId);
                        success.onSuccess(null);
                        return;
                    }

                    Log.d(TAG, "Tìm thấy " + ingredients.size() + " nguyên liệu cần trừ");

                    // Lấy danh sách inventory để tìm đúng ID theo tên
                    db.collection("inventory").get()
                            .addOnSuccessListener(inventorySnapshot -> {
                                WriteBatch batch = db.batch();
                                boolean hasUpdates = false;

                                for (Map<String, Object> ing : ingredients) {
                                    String ingId = (String) ing.get("ingredientId");
                                    String ingName = (String) ing.get("ingredientName");
                                    Object qtyObj = ing.get("quantityRequired");
                                    double qtyRequired = 0;
                                    if (qtyObj instanceof Double) {
                                        qtyRequired = (Double) qtyObj;
                                    } else if (qtyObj instanceof Long) {
                                        qtyRequired = ((Long) qtyObj).doubleValue();
                                    }

                                    double totalDeduct = qtyRequired * quantity;
                                    
                                    // Tìm đúng ID trong inventory
                                    String actualIngId = null;
                                    
                                    // Ưu tiên tìm theo ID
                                    for (DocumentSnapshot invDoc : inventorySnapshot.getDocuments()) {
                                        if (invDoc.getId().equals(ingId)) {
                                            actualIngId = ingId;
                                            break;
                                        }
                                    }
                                    
                                    // Fallback: Tìm theo TÊN
                                    if (actualIngId == null && ingName != null) {
                                        for (DocumentSnapshot invDoc : inventorySnapshot.getDocuments()) {
                                            String invName = invDoc.getString("name");
                                            if (invName != null && invName.equalsIgnoreCase(ingName)) {
                                                actualIngId = invDoc.getId();
                                                Log.d(TAG, "Fallback tìm theo tên: " + ingName + " -> ID: " + actualIngId);
                                                break;
                                            }
                                        }
                                    }
                                    
                                    if (actualIngId != null) {
                                        Log.d(TAG, "Trừ nguyên liệu: " + ingName + " - " + totalDeduct + " (ID: " + actualIngId + ")");
                                        DocumentReference stockRef = db.collection("inventory").document(actualIngId);
                                        batch.update(stockRef, "quantity", FieldValue.increment(-totalDeduct));
                                        hasUpdates = true;
                                    } else {
                                        Log.w(TAG, "Không tìm thấy nguyên liệu: " + ingName);
                                    }
                                }

                                if (hasUpdates) {
                                    batch.commit()
                                            .addOnSuccessListener(aVoid -> {
                                                Log.d(TAG, "✅ Đã trừ kho thành công cho menuItemId: " + menuItemId);
                                                success.onSuccess(null);
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.e(TAG, "❌ Lỗi trừ kho: " + e.getMessage());
                                                if (e.getMessage() != null && e.getMessage().contains("PERMISSION_DENIED")) {
                                                    Log.e(TAG, "❌ LỖI PERMISSION: Staff không có quyền update inventory!");
                                                }
                                                fail.onFailure(e);
                                            });
                                } else {
                                    Log.w(TAG, "Không có nguyên liệu nào để trừ");
                                    success.onSuccess(null);
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Lỗi lấy danh sách inventory: " + e.getMessage());
                                fail.onFailure(e);
                            });
                },
                e -> {
                    Log.e(TAG, "Lỗi tìm công thức: " + e.getMessage());
                    fail.onFailure(e);
                }
        );
    }

    /**
     * Kiểm tra nguyên liệu có đủ cho đơn hàng không
     * @param items Danh sách items trong đơn (List<Map> hoặc List<OrderItem>)
     * @param callback Callback với kết quả: null nếu đủ, hoặc danh sách thiếu nếu không đủ
     */
    public void checkIngredientsAvailability(List<?> items, 
                                             OnSuccessListener<List<String>> callback,
                                             OnFailureListener fail) {
        if (items == null || items.isEmpty()) {
            callback.onSuccess(null); // Không có items thì pass
            return;
        }

        // Lấy inventory hiện tại
        db.collection("inventory").get()
                .addOnSuccessListener(inventorySnapshot -> {
                    // Tạo map inventory: tên -> số lượng hiện có
                    Map<String, Double> inventoryMap = new HashMap<>();
                    Map<String, String> inventoryIdMap = new HashMap<>(); // tên -> id
                    
                    for (DocumentSnapshot doc : inventorySnapshot.getDocuments()) {
                        String name = doc.getString("name");
                        Double qty = doc.getDouble("quantity");
                        if (name != null) {
                            inventoryMap.put(name.toLowerCase(), qty != null ? qty : 0);
                            inventoryIdMap.put(doc.getId(), name.toLowerCase());
                        }
                    }

                    // Tính tổng nguyên liệu cần dùng cho tất cả items
                    Map<String, Double> totalRequired = new HashMap<>();
                    final int[] pendingRecipes = {0};
                    final int[] completedRecipes = {0};
                    List<String> missingIngredients = new ArrayList<>();

                    // Đếm số recipe cần fetch
                    for (Object itemObj : items) {
                        String menuItemId = extractMenuItemIdFromObject(itemObj);
                        if (menuItemId != null && !menuItemId.isEmpty()) {
                            pendingRecipes[0]++;
                        }
                    }

                    if (pendingRecipes[0] == 0) {
                        callback.onSuccess(null);
                        return;
                    }

                    // Lấy công thức cho từng món
                    for (Object itemObj : items) {
                        String menuItemId = extractMenuItemIdFromObject(itemObj);
                        int orderQty = extractQuantityFromObject(itemObj);
                        String itemName = extractNameFromObject(itemObj);

                        if (menuItemId != null && !menuItemId.isEmpty()) {
                            getRecipeByMenuItemId(menuItemId,
                                    recipeSnapshot -> {
                                        if (!recipeSnapshot.isEmpty()) {
                                            DocumentSnapshot recipeDoc = recipeSnapshot.getDocuments().get(0);
                                            List<Map<String, Object>> ingredients = 
                                                (List<Map<String, Object>>) recipeDoc.get("ingredients");

                                            if (ingredients != null) {
                                                for (Map<String, Object> ing : ingredients) {
                                                    String ingName = (String) ing.get("ingredientName");
                                                    String ingId = (String) ing.get("ingredientId");
                                                    Object qtyObj = ing.get("quantityRequired");
                                                    double qtyRequired = 0;
                                                    if (qtyObj instanceof Double) {
                                                        qtyRequired = (Double) qtyObj;
                                                    } else if (qtyObj instanceof Long) {
                                                        qtyRequired = ((Long) qtyObj).doubleValue();
                                                    }

                                                    double totalNeed = qtyRequired * orderQty;
                                                    
                                                    // Tìm key trong inventory (theo ID hoặc tên)
                                                    String key = null;
                                                    if (ingId != null && inventoryIdMap.containsKey(ingId)) {
                                                        key = inventoryIdMap.get(ingId);
                                                    } else if (ingName != null) {
                                                        key = ingName.toLowerCase();
                                                    }

                                                    if (key != null) {
                                                        totalRequired.merge(key, totalNeed, Double::sum);
                                                    }
                                                }
                                            }
                                        }

                                        completedRecipes[0]++;
                                        if (completedRecipes[0] >= pendingRecipes[0]) {
                                            // Đã fetch xong tất cả recipe, kiểm tra
                                            for (Map.Entry<String, Double> entry : totalRequired.entrySet()) {
                                                String ingKey = entry.getKey();
                                                double required = entry.getValue();
                                                double available = inventoryMap.getOrDefault(ingKey, 0.0);

                                                if (available < required) {
                                                    missingIngredients.add(String.format("%s: cần %.1f, còn %.1f", 
                                                            ingKey, required, available));
                                                }
                                            }

                                            if (missingIngredients.isEmpty()) {
                                                callback.onSuccess(null); // Đủ nguyên liệu
                                            } else {
                                                callback.onSuccess(missingIngredients); // Thiếu
                                            }
                                        }
                                    },
                                    e -> {
                                        completedRecipes[0]++;
                                        if (completedRecipes[0] >= pendingRecipes[0]) {
                                            if (missingIngredients.isEmpty()) {
                                                callback.onSuccess(null);
                                            } else {
                                                callback.onSuccess(missingIngredients);
                                            }
                                        }
                                    });
                        }
                    }
                })
                .addOnFailureListener(fail);
    }

    private String extractMenuItemIdFromObject(Object obj) {
        if (obj instanceof Map) {
            Object id = ((Map<?, ?>) obj).get("menuItemId");
            return id != null ? id.toString() : null;
        }
        return null;
    }

    private int extractQuantityFromObject(Object obj) {
        if (obj instanceof Map) {
            Object qty = ((Map<?, ?>) obj).get("quantity");
            if (qty instanceof Number) {
                return ((Number) qty).intValue();
            }
        }
        return 1;
    }

    private String extractNameFromObject(Object obj) {
        if (obj instanceof Map) {
            Object name = ((Map<?, ?>) obj).get("name");
            return name != null ? name.toString() : "Unknown";
        }
        return "Unknown";
    }

    /** Tìm kiếm nhân viên theo tên (username) */
    public void searchStaffByName(String nameQuery,
                                  OnSuccessListener<List<User>> success,
                                  OnFailureListener fail) {
        if (nameQuery == null || nameQuery.trim().isEmpty()) {
            // Nếu query rỗng, trả về tất cả nhân viên
            listenUsersByRoleRealtime("staff", (value, error) -> {
                if (error != null || value == null) {
                    fail.onFailure(error != null ? new Exception(error.getMessage()) : new Exception("Unknown error"));
                    return;
                }
                List<User> result = new ArrayList<>();
                for (DocumentSnapshot doc : value.getDocuments()) {
                    User user = doc.toObject(User.class);
                    if (user != null) {
                        user.setId(doc.getId());
                        result.add(user);
                    }
                }
                success.onSuccess(result);
            });
            return;
        }

        // Dùng startAt / endAt để tìm kiếm tên gần đúng
        String queryLower = nameQuery.toLowerCase();
        db.collection("users")
                .whereEqualTo("role", "staff")
                .orderBy("usernameLower") // cần có field usernameLower trong Firestore (username.toLowerCase())
                .startAt(queryLower)
                .endAt(queryLower + "\uf8ff")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<User> result = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        User user = doc.toObject(User.class);
                        if (user != null) {
                            user.setId(doc.getId());
                            result.add(user);
                        }
                    }
                    success.onSuccess(result);
                })
                .addOnFailureListener(fail);
    }

}
