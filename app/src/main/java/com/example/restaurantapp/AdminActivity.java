package com.example.restaurantapp;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;

import com.example.restaurantapp.api.FirebaseService;
import com.example.restaurantapp.authentication.LoginActivity;
import com.example.restaurantapp.authentication.RegisterStaffActivity;
import com.google.android.material.button.MaterialButton;

public class AdminActivity extends AppCompatActivity {

    MaterialButton btnCreateStaff, btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        btnCreateStaff = findViewById(R.id.btnCreateStaff);
        btnLogout = findViewById(R.id.btnLogout);

        btnCreateStaff.setOnClickListener(v ->
                startActivity(new Intent(AdminActivity.this, RegisterStaffActivity.class)));

        btnLogout.setOnClickListener(v -> {
            FirebaseService.getInstance().logout();
            startActivity(new Intent(AdminActivity.this, LoginActivity.class));
            finish();
        });
    }
}
