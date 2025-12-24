package com.example.restaurantapp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.restaurantapp.api.FirebaseService;
import com.example.restaurantapp.authentication.LoginActivity;
import com.example.restaurantapp.fragments.staff.StaffNotificationsFragment;
import com.example.restaurantapp.fragments.staff.StaffOrdersFragment;
import com.example.restaurantapp.fragments.staff.StaffReservationsFragment;
import com.example.restaurantapp.fragments.staff.StaffTablesFragment;
import com.example.restaurantapp.utils.LocaleHelper;
import com.example.restaurantapp.utils.PreferenceManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.example.restaurantapp.fragments.staff.StaffTimesheetFragment;

public class StaffActivity extends AppCompatActivity {

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
        setContentView(R.layout.activity_staff);

        bottomNav = findViewById(R.id.bottomNav);

        // Load fragment mặc định
        if (savedInstanceState == null) {
            loadFragment(new StaffTablesFragment());
        }

        bottomNav.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment fragment = null;
                int itemId = item.getItemId();

                if (itemId == R.id.nav_tables) {
                    fragment = new StaffTablesFragment();

                } else if (itemId == R.id.nav_timesheet) {
                    fragment = new StaffTimesheetFragment(); // 🔥 CHẤM CÔNG

                } else if (itemId == R.id.nav_reservations) {
                    fragment = new StaffReservationsFragment();

                } else if (itemId == R.id.nav_orders) {
                    fragment = new StaffOrdersFragment();

                } else if (itemId == R.id.nav_notifications) {
                    fragment = new StaffNotificationsFragment();

                } else if (itemId == R.id.nav_logout) {
                    FirebaseService.getInstance().logout();
                    startActivity(new Intent(StaffActivity.this, LoginActivity.class));
                    finish();
                    return true;
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
