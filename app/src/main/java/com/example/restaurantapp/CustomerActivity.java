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
import com.example.restaurantapp.fragments.customer.CustomerMenuFragment;
import com.example.restaurantapp.fragments.customer.CustomerNotificationsFragment;
import com.example.restaurantapp.fragments.customer.CustomerOrdersFragment;
import com.example.restaurantapp.fragments.customer.CustomerProfileFragment;
import com.example.restaurantapp.fragments.customer.CustomerReservationFragment;
import com.example.restaurantapp.utils.LocaleHelper;
import com.example.restaurantapp.utils.PreferenceManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class CustomerActivity extends AppCompatActivity {

    BottomNavigationView bottomNav;

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
        setContentView(R.layout.activity_customer);

        bottomNav = findViewById(R.id.bottomNav);

        // Load fragment mặc định
        if (savedInstanceState == null) {
            loadFragment(new CustomerMenuFragment());
        }

        bottomNav.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment fragment = null;
                int itemId = item.getItemId();

                if (itemId == R.id.nav_menu) {
                    fragment = new CustomerMenuFragment();
                } else if (itemId == R.id.nav_reservation) {
                    fragment = new CustomerReservationFragment();
                } else if (itemId == R.id.nav_orders) {
                    fragment = new CustomerOrdersFragment();
                } else if (itemId == R.id.nav_notifications) {
                    fragment = new CustomerNotificationsFragment();
                } else if (itemId == R.id.nav_profile) {
                    fragment = new CustomerProfileFragment();
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
