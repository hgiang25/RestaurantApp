package com.example.restaurantapp.authentication;

import androidx.appcompat.app.AppCompatActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.example.restaurantapp.R;
import com.example.restaurantapp.api.FirebaseService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class RegisterActivity extends AppCompatActivity {

    TextInputEditText edtEmail, edtUsername, edtPassword, edtRePassword;
    MaterialButton btnRegister;
    TextView txtLogin;
    ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        edtEmail = findViewById(R.id.edtEmail);
        edtUsername = findViewById(R.id.edtUsername);
        edtPassword = findViewById(R.id.edtPassword);
        edtRePassword = findViewById(R.id.edtRePassword);
        btnRegister = findViewById(R.id.btnRegister);
        txtLogin = findViewById(R.id.txtLogin);

        dialog = new ProgressDialog(this);
        dialog.setMessage("Đang tạo tài khoản...");

        btnRegister.setOnClickListener(v -> handleRegister());

        txtLogin.setOnClickListener(v ->
                startActivity(new Intent(RegisterActivity.this, LoginActivity.class)));
    }

    private void handleRegister() {
        String email = edtEmail.getText().toString().trim();
        String username = edtUsername.getText().toString().trim();
        String pass = edtPassword.getText().toString().trim();
        String repass = edtRePassword.getText().toString().trim();

        if (email.isEmpty()) { edtEmail.setError("Email không được để trống"); return; }
        if (username.isEmpty()) { edtUsername.setError("Username không được để trống"); return; }
        if (pass.isEmpty()) { edtPassword.setError("Mật khẩu không được để trống"); return; }
        if (!pass.equals(repass)) { edtRePassword.setError("Mật khẩu không khớp"); return; }

        dialog.show();

        FirebaseService.getInstance().register(email, username, pass, "customer",
                unused -> {
                    dialog.dismiss();
                    Toast.makeText(this, "Đăng ký thành công! Vui lòng kiểm tra email xác thực.", Toast.LENGTH_LONG).show();
                    startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                    finish();
                },
                e -> {
                    dialog.dismiss();
                    edtEmail.setError(e.getMessage());
                }
        );
    }
}
