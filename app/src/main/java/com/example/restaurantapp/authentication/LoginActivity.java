package com.example.restaurantapp.authentication;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ActivityOptions;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
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

public class LoginActivity extends AppCompatActivity {

    TextInputEditText edtEmail, edtPassword;
    MaterialButton btnLogin;
    TextView txtRegister;

    ImageView imgLogo;
    TextView txtLoginTitle;
    CardView cardForm;
    LottieAnimationView lottieBackground; // hoặc View nếu bạn dùng View


    ProgressDialog dialog;
    FirebaseService api;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply saved settings
        PreferenceManager prefs = new PreferenceManager(this);
        prefs.applySavedSettings();
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Ánh xạ
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);
        txtRegister = findViewById(R.id.txtRegister);

        // Ánh xạ view animation
//        imgLogo = findViewById(R.id.imgLogo);
//        txtLoginTitle = findViewById(R.id.txtLoginTitle);
//        cardForm = findViewById(R.id.cardForm);
//        txtRegister = findViewById(R.id.txtRegister);
//        lottieBackground = findViewById(R.id.lottie_background);




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

    @Override
    protected void onResume() {
        super.onResume();

//        Animation slideFromBottom = AnimationUtils.loadAnimation(this, R.anim.slide_from_bottom);
//        Animation slideFromBottomDelayed = AnimationUtils.loadAnimation(this, R.anim.slide_from_bottom_delayed);
//
//        imgLogo.startAnimation(slideFromBottom);
//        txtLoginTitle.startAnimation(slideFromBottomDelayed);
//        cardForm.startAnimation(slideFromBottom);
//        txtRegister.startAnimation(slideFromBottomDelayed);
//        btnLogin.startAnimation(slideFromBottom);
//        lottieBackground.startAnimation(slideFromBottom);
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
