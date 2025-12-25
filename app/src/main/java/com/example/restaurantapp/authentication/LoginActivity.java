package com.example.restaurantapp.authentication;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ActivityOptions;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import androidx.cardview.widget.CardView;
import com.airbnb.lottie.LottieAnimationView;



import com.example.restaurantapp.AdminActivity;
import com.example.restaurantapp.CustomerActivity;
import com.example.restaurantapp.R;
import com.example.restaurantapp.StaffActivity;
import com.example.restaurantapp.api.FirebaseService;
import com.example.restaurantapp.utils.LocaleHelper;
import com.example.restaurantapp.utils.PreferenceManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import android.widget.CheckBox;
import android.widget.Toast;


public class LoginActivity extends AppCompatActivity {

    TextInputEditText edtEmail, edtPassword;
    MaterialButton btnLogin;
    TextView txtRegister, txtForgotPassword;
    CheckBox cbRemember;

    ProgressDialog dialog;
    FirebaseService api;
    SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Ánh xạ view
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);
        txtRegister = findViewById(R.id.txtRegister);
        txtForgotPassword = findViewById(R.id.txtForgotPassword);
        cbRemember = findViewById(R.id.cbRemember);

        dialog = new ProgressDialog(this);
        dialog.setMessage("Đang đăng nhập...");

        api = FirebaseService.getInstance();

        // SharedPreferences
        prefs = getSharedPreferences("restaurant_prefs", MODE_PRIVATE);

        // Nếu đã lưu, tự động điền
        if (prefs.getBoolean("remember_login", false)) {
            edtEmail.setText(prefs.getString("saved_email", ""));
            edtPassword.setText(prefs.getString("saved_password", ""));
            cbRemember.setChecked(true);


        }

        // Xử lý login
        btnLogin.setOnClickListener(v -> handleLogin());

        // Chuyển sang RegisterActivity
        txtRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });

        // Quên mật khẩu
        txtForgotPassword.setOnClickListener(v -> {
            String email = edtEmail.getText().toString().trim();
            if (email.isEmpty()) {
                edtEmail.setError("Nhập email để khôi phục mật khẩu");
                return;
            }
            FirebaseAuth.getInstance()
                    .sendPasswordResetEmail(email)
                    .addOnSuccessListener(unused ->
                            Toast.makeText(this,
                                    "Đã gửi email đặt lại mật khẩu",
                                    Toast.LENGTH_LONG).show())
                    .addOnFailureListener(e ->
                            Toast.makeText(this,
                                    "Email không tồn tại",
                                    Toast.LENGTH_SHORT).show());
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
                    api.getDb().collection("users").document(uid).get()
                            .addOnSuccessListener(snapshot -> {
                                dialog.dismiss();
                                String role = snapshot.getString("role");
                                boolean isVerified = FirebaseAuth.getInstance()
                                        .getCurrentUser().isEmailVerified();
                                if (!isVerified && !role.equals("admin")) {
                                    edtEmail.setError("Vui lòng xác thực email trước khi đăng nhập");
                                    return;
                                }

                                // Lưu thông tin nếu check Remember
                                SharedPreferences.Editor editor = prefs.edit();
                                if (cbRemember.isChecked()) {
                                    editor.putBoolean("remember_login", true);
                                    editor.putString("saved_email", email);
                                    editor.putString("saved_password", pass);
                                } else {
                                    editor.clear();
                                }
                                editor.apply();

                                redirectToRoleActivity();
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

    private void redirectToRoleActivity() {
        String uid = api.getCurrentUserId();
        api.getDb().collection("users").document(uid).get()
                .addOnSuccessListener(snapshot -> {
                    String role = snapshot.getString("role");
                    Intent intent;
                    switch (role) {
                        case "admin": intent = new Intent(this, AdminActivity.class); break;
                        case "staff": intent = new Intent(this, StaffActivity.class); break;
                        default: intent = new Intent(this, CustomerActivity.class); break;
                    }
                    startActivity(intent);
                    finish();
                });
    }
}

