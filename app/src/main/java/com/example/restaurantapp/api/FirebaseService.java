package com.example.restaurantapp.api;

import androidx.annotation.NonNull;

import com.example.restaurantapp.models.User;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class FirebaseService {

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

    // ============ AUTH =============== //

    /** Đăng ký tài khoản mới với role tùy chọn */
    public void register(String email, String username, String pass, String role,
                         OnSuccessListener<Void> success,
                         OnFailureListener fail) {

        auth.createUserWithEmailAndPassword(email, pass)
                .addOnSuccessListener(authResult -> {
                    String uid = auth.getCurrentUser().getUid();
                    User user = new User(email, username, role);

                    db.collection("users").document(uid)
                            .set(user)
                            .addOnSuccessListener(success)
                            .addOnFailureListener(fail);

                    // Gửi email xác thực
                    auth.getCurrentUser().sendEmailVerification();
                })
                .addOnFailureListener(fail);
    }


    /** Đăng nhập tài khoản */
    public void login(String email, String pass,
                      OnSuccessListener<Void> success,
                      OnFailureListener fail) {

        auth.signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener(authResult -> {
                    success.onSuccess(null);  // callback thành công
                })
                .addOnFailureListener(fail);
    }


    /** Kiểm tra user đã đăng nhập hay chưa */
    public boolean isLoggedIn() {
        return auth.getCurrentUser() != null;
    }

    /** Đăng xuất */
    public void logout() {
        auth.signOut();
    }

    public String getCurrentUserId() {
        return auth.getCurrentUser() != null
                ? auth.getCurrentUser().getUid()
                : null;
    }

    public FirebaseFirestore getDb() {
        return db;
    }


    // ============================================= //
}
