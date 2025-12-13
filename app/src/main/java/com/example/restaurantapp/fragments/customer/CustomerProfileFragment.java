package com.example.restaurantapp.fragments.customer;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.restaurantapp.R;
import com.example.restaurantapp.api.FirebaseService;
import com.example.restaurantapp.authentication.LoginActivity;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;

public class CustomerProfileFragment extends Fragment {

    private TextView txtUsername, txtEmail, txtLoyaltyPoints;
    private MaterialButton btnLogout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_customer_profile, container, false);

        txtUsername = view.findViewById(R.id.txtUsername);
        txtEmail = view.findViewById(R.id.txtEmail);
        txtLoyaltyPoints = view.findViewById(R.id.txtLoyaltyPoints);
        btnLogout = view.findViewById(R.id.btnLogout);

        loadProfile();

        btnLogout.setOnClickListener(v -> {
            FirebaseService.getInstance().logout();
            startActivity(new Intent(getActivity(), LoginActivity.class));
            getActivity().finish();
        });

        return view;
    }

    private void loadProfile() {
        String userId = FirebaseService.getInstance().getCurrentUserId();
        if (userId == null) return;

        FirebaseService.getInstance().listenUserRealtime(userId, (snapshot, error) -> {
            if (error != null || snapshot == null) return;

            String username = snapshot.getString("username");
            String email = snapshot.getString("email");
            Long points = snapshot.getLong("loyaltyPoints");

            txtUsername.setText(username != null ? username : "N/A");
            txtEmail.setText(email != null ? email : "N/A");
            txtLoyaltyPoints.setText(points != null ? points + " điểm" : "0 điểm");
        });
    }
}
