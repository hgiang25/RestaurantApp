package com.example.restaurantapp.authentication;

import androidx.appcompat.app.AppCompatActivity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.widget.Toast;

import com.example.restaurantapp.R;
import com.example.restaurantapp.api.FirebaseService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class RegisterStaffActivity extends AppCompatActivity {

    TextInputEditText edtEmail, edtUsername, edtPassword, edtRePassword;
    MaterialButton btnRegister;
    ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_staff);

        edtEmail = findViewById(R.id.edtEmail);
        edtUsername = findViewById(R.id.edtUsername);
        edtPassword = findViewById(R.id.edtPassword);
        edtRePassword = findViewById(R.id.edtRePassword);
        btnRegister = findViewById(R.id.btnRegister);

        dialog = new ProgressDialog(this);
        dialog.setMessage("Đang tạo nhân viên...");

        btnRegister.setOnClickListener(v -> handleRegister());
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

        FirebaseService.getInstance().register(email, username, pass, "staff",
                unused -> {
                    dialog.dismiss();
                    Toast.makeText(this, "Tạo nhân viên thành công! Kiểm tra email để xác thực.", Toast.LENGTH_LONG).show();
                    finish();
                },
                e -> {
                    dialog.dismiss();
                    edtEmail.setError(e.getMessage());
                });
    }
}
