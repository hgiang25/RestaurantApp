package com.example.restaurantapp.authentication;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ActivityOptions;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import com.example.restaurantapp.AdminActivity;
import com.example.restaurantapp.CustomerActivity;
import com.example.restaurantapp.R;
import com.example.restaurantapp.StaffActivity;
import com.example.restaurantapp.api.FirebaseService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    TextInputEditText edtEmail, edtPassword;
    MaterialButton btnLogin;
    TextView txtRegister;

    ProgressDialog dialog;
    FirebaseService api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Ánh xạ
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);
        txtRegister = findViewById(R.id.txtRegister);

        dialog = new ProgressDialog(this);
        dialog.setMessage("Đang đăng nhập...");

        api = FirebaseService.getInstance();

        // Xử lý login
        btnLogin.setOnClickListener(v -> handleLogin());

        // Chuyển sang RegisterActivity với animation
        txtRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent, ActivityOptions.makeCustomAnimation(this,
                    R.anim.slide_in_right, R.anim.slide_out_left).toBundle());
        });
    }


    private void handleLogin() {
        String email = edtEmail.getText().toString().trim();
        String pass = edtPassword.getText().toString().trim();

        if (email.isEmpty()) { edtEmail.setError("Email không được để trống"); return; }
        if (pass.isEmpty()) { edtPassword.setError("Mật khẩu không được để trống"); return; }

        dialog.show();

        api.login(email, pass,
                unused -> {
                    String uid = api.getCurrentUserId();
                    // Lấy role từ Firestore
                    api.getDb().collection("users").document(uid).get()
                            .addOnSuccessListener(snapshot -> {
                                dialog.dismiss();
                                String role = snapshot.getString("role");

                                // Chỉ kiểm tra email verify với staff và customer
                                boolean isVerified = FirebaseAuth.getInstance()
                                        .getCurrentUser().isEmailVerified();
                                if (!isVerified && !role.equals("admin")) {
                                    edtEmail.setError("Vui lòng xác thực email trước khi đăng nhập");
                                    return;
                                }

                                // Chuyển màn hình theo role với animation
                                Intent intent;
                                switch (role) {
                                    case "admin":
                                        intent = new Intent(this, AdminActivity.class);
                                        break;
                                    case "staff":
                                        intent = new Intent(this, StaffActivity.class);
                                        break;
                                    default:
                                        intent = new Intent(this, CustomerActivity.class);
                                        break;
                                }
                                startActivity(intent, ActivityOptions.makeCustomAnimation(this,
                                        R.anim.slide_in_right, R.anim.slide_out_left).toBundle());
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                dialog.dismiss();
                                edtEmail.setError("Lỗi lấy thông tin user");
                            });
                },
                e -> {
                    dialog.dismiss();
                    edtPassword.setError("Sai email hoặc mật khẩu");
                });
    }

}
