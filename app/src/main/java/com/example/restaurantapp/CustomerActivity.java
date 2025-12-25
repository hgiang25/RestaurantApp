package com.example.restaurantapp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.restaurantapp.api.FirebaseService;
import com.example.restaurantapp.authentication.LoginActivity;
import com.example.restaurantapp.fragments.customer.CustomerMenuFragment;
import com.example.restaurantapp.fragments.customer.CustomerChatFragment;
import com.example.restaurantapp.fragments.customer.CustomerNotificationsFragment;
import com.example.restaurantapp.fragments.customer.CustomerOrdersFragment;
import com.example.restaurantapp.fragments.customer.CustomerProfileFragment;
import com.example.restaurantapp.fragments.customer.CustomerReservationFragment;
import com.example.restaurantapp.utils.LocaleHelper;
import com.example.restaurantapp.utils.PreferenceManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationBarView;

public class CustomerActivity extends AppCompatActivity {

    BottomNavigationView bottomNav;
    FloatingActionButton fabChat;
    private boolean isChatOpen = false;
    private float dX, dY;
    private int lastAction;
    private boolean isDragging = false;


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
        fabChat = findViewById(R.id.fabChat);

        fabChat.setOnTouchListener((view, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    dX = view.getX() - event.getRawX();
                    dY = view.getY() - event.getRawY();
                    lastAction = MotionEvent.ACTION_DOWN;
                    isDragging = false;
                    return true;

                case MotionEvent.ACTION_MOVE:
                    view.setX(event.getRawX() + dX);
                    view.setY(event.getRawY() + dY);
                    lastAction = MotionEvent.ACTION_MOVE;
                    isDragging = true;
                    return true;

                case MotionEvent.ACTION_UP:
                    if (!isDragging && lastAction == MotionEvent.ACTION_DOWN) {
                        // Nếu KHÔNG kéo → coi là click
                        view.performClick();
                    }
                    return true;

                default:
                    return false;
            }
        });


        // Load fragment mặc định
        if (savedInstanceState == null) {
            loadFragment(new CustomerMenuFragment());
        }

        // FAB Chat click listener
        fabChat.setOnClickListener(v -> {
            if (!isChatOpen) {
                // Mở Chat
                loadFragment(new CustomerChatFragment());
                fabChat.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
                isChatOpen = true;
                bottomNav.setVisibility(android.view.View.GONE);
            } else {
                // Đóng Chat, quay lại Menu
                loadFragment(new CustomerMenuFragment());
                fabChat.setImageResource(R.drawable.ic_chat_bot);
                isChatOpen = false;
                bottomNav.setVisibility(android.view.View.VISIBLE);
                bottomNav.setSelectedItemId(R.id.nav_menu);
            }
        });

        bottomNav.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                // Reset chat state khi chọn tab khác
                if (isChatOpen) {
                    fabChat.setImageResource(R.drawable.ic_chat_bot);
                    isChatOpen = false;
                }
                
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

    @Override
    public void onBackPressed() {
        if (isChatOpen) {
            // Đóng chat khi nhấn back
            fabChat.performClick();
        } else {
            super.onBackPressed();
        }
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }
}
