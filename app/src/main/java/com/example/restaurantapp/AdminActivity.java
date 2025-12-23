package com.example.restaurantapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import com.example.restaurantapp.api.FirebaseService;
import com.example.restaurantapp.authentication.LoginActivity;
import com.example.restaurantapp.fragments.admin.AdminDashboardFragment;
import com.example.restaurantapp.fragments.admin.AdminMenuFragment;
import com.example.restaurantapp.fragments.admin.AdminMoreFragment;
import com.example.restaurantapp.fragments.admin.AdminStaffFragment;
import com.example.restaurantapp.fragments.admin.AdminTablesFragment;
import com.example.restaurantapp.utils.LocaleHelper;
import com.example.restaurantapp.utils.PreferenceManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class AdminActivity extends AppCompatActivity {

    BottomNavigationView bottomNav;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        PreferenceManager prefs = new PreferenceManager(this);
        prefs.applySavedSettings();
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        bottomNav = findViewById(R.id.bottomNav);

        // Load fragment mặc định
        if (savedInstanceState == null) {
            loadFragment(new AdminDashboardFragment());
        }

        bottomNav.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment fragment = null;
                int itemId = item.getItemId();

                if (itemId == R.id.nav_dashboard) {
                    fragment = new AdminDashboardFragment();
                } else if (itemId == R.id.nav_staff) {
                    fragment = new AdminStaffFragment();
                } else if (itemId == R.id.nav_tables) {
                    fragment = new AdminTablesFragment();
                } else if (itemId == R.id.nav_menu) {
                    fragment = new AdminMenuFragment();
                } else if (itemId == R.id.nav_more) {
                    fragment = new AdminMoreFragment();
                }

                if (fragment != null) {
                    loadFragment(fragment);
                    return true;
                }
                return false;
            }
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }
}
