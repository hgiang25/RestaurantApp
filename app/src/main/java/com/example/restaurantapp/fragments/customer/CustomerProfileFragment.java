package com.example.restaurantapp.fragments.customer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.restaurantapp.R;
import com.example.restaurantapp.activities.OrderHistoryActivity;
import com.example.restaurantapp.api.FirebaseService;
import com.example.restaurantapp.authentication.LoginActivity;
import com.example.restaurantapp.utils.LocaleHelper;
import com.example.restaurantapp.utils.PreferenceManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import de.hdodenhof.circleimageview.CircleImageView;

import java.util.HashMap;
import java.util.Map;

public class CustomerProfileFragment extends Fragment {

    private TextView txtUsername, txtEmail, txtLoyaltyPoints;
    private CircleImageView imgAvatar;
    private LinearLayout btnEditProfile, btnOrderHistory, btnSettings;
    private MaterialButton btnLogout;
    private PreferenceManager preferenceManager;
    
    // Current user data
    private String currentUsername = "";
    private String currentPhone = "";
    private String currentAddress = "";
    private String currentAvatarUrl = "";
    
    // For avatar selection in dialog
    private CircleImageView dialogAvatarView;
    private Uri selectedAvatarUri = null;
    
    // Activity result launcher for image picker
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize image picker launcher
        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedAvatarUri = result.getData().getData();
                    if (selectedAvatarUri != null && dialogAvatarView != null) {
                        Glide.with(this)
                                .load(selectedAvatarUri)
                                .placeholder(R.drawable.ic_profile)
                                .into(dialogAvatarView);
                    }
                }
            }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_customer_profile, container, false);

        preferenceManager = new PreferenceManager(requireContext());

        imgAvatar = view.findViewById(R.id.imgAvatar);
        txtUsername = view.findViewById(R.id.txtUsername);
        txtEmail = view.findViewById(R.id.txtEmail);
        txtLoyaltyPoints = view.findViewById(R.id.txtLoyaltyPoints);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        btnOrderHistory = view.findViewById(R.id.btnOrderHistory);
        btnSettings = view.findViewById(R.id.btnSettings);
        btnLogout = view.findViewById(R.id.btnLogout);

        loadProfile();
        setupClickListeners();

        return view;
    }

    private void setupClickListeners() {
        btnEditProfile.setOnClickListener(v -> showEditProfileDialog());
        
        btnOrderHistory.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), OrderHistoryActivity.class));
        });
        
        btnSettings.setOnClickListener(v -> showSettingsDialog());

        btnLogout.setOnClickListener(v -> {
            FirebaseService.getInstance().logout();
            startActivity(new Intent(getActivity(), LoginActivity.class));
            getActivity().finish();
        });
    }

    private void loadProfile() {
        String userId = FirebaseService.getInstance().getCurrentUserId();
        if (userId == null) return;

        FirebaseService.getInstance().listenUserRealtime(userId, (snapshot, error) -> {
            if (error != null || snapshot == null) return;

            String username = snapshot.getString("username");
            String email = snapshot.getString("email");
            Long points = snapshot.getLong("loyaltyPoints");
            String phone = snapshot.getString("phone");
            String address = snapshot.getString("address");
            String avatarUrl = snapshot.getString("avatarUrl");

            currentUsername = username != null ? username : "";
            currentPhone = phone != null ? phone : "";
            currentAddress = address != null ? address : "";
            currentAvatarUrl = avatarUrl != null ? avatarUrl : "";

            txtUsername.setText(username != null ? username : "N/A");
            txtEmail.setText(email != null ? email : "N/A");
            txtLoyaltyPoints.setText(points != null ? points + " điểm" : "0 điểm");
            
            // Load avatar
            if (currentAvatarUrl != null && !currentAvatarUrl.isEmpty() && imgAvatar != null) {
                Glide.with(requireContext())
                        .load(currentAvatarUrl)
                        .placeholder(R.drawable.ic_profile)
                        .error(R.drawable.ic_profile)
                        .into(imgAvatar);
            }
        });
    }

    private void showEditProfileDialog() {
        View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_edit_profile, null);

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.bg_dialog);
        }

        dialogAvatarView = dialogView.findViewById(R.id.imgAvatar);
        MaterialButton btnChangeAvatar = dialogView.findViewById(R.id.btnChangeAvatar);
        TextInputEditText edtDisplayName = dialogView.findViewById(R.id.edtDisplayName);
        TextInputEditText edtPhone = dialogView.findViewById(R.id.edtPhone);
        TextInputEditText edtAddress = dialogView.findViewById(R.id.edtAddress);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);
        MaterialButton btnSave = dialogView.findViewById(R.id.btnSave);

        // Load current avatar
        selectedAvatarUri = null;
        if (currentAvatarUrl != null && !currentAvatarUrl.isEmpty()) {
            Glide.with(this)
                    .load(currentAvatarUrl)
                    .placeholder(R.drawable.ic_profile)
                    .into(dialogAvatarView);
        }

        // Fill current data
        edtDisplayName.setText(currentUsername);
        edtPhone.setText(currentPhone);
        edtAddress.setText(currentAddress);

        // Change avatar button
        btnChangeAvatar.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });

        btnCancel.setOnClickListener(v -> {
            dialogAvatarView = null;
            dialog.dismiss();
        });

        btnSave.setOnClickListener(v -> {
            String newUsername = edtDisplayName.getText().toString().trim();
            String newPhone = edtPhone.getText().toString().trim();
            String newAddress = edtAddress.getText().toString().trim();

            if (newUsername.isEmpty()) {
                edtDisplayName.setError("Vui lòng nhập tên");
                return;
            }

            // If avatar changed, upload first then save profile
            if (selectedAvatarUri != null) {
                uploadAvatarAndSaveProfile(newUsername, newPhone, newAddress, dialog);
            } else {
                saveProfile(newUsername, newPhone, newAddress, null);
                dialogAvatarView = null;
                dialog.dismiss();
            }
        });

        dialog.show();
    }
    
    private void uploadAvatarAndSaveProfile(String username, String phone, String address, AlertDialog dialog) {
        String userId = FirebaseService.getInstance().getCurrentUserId();
        if (userId == null || selectedAvatarUri == null) return;

        Toast.makeText(getContext(), "Đang tải ảnh lên...", Toast.LENGTH_SHORT).show();

        StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                .child("avatars")
                .child(userId + ".jpg");

        storageRef.putFile(selectedAvatarUri)
                .addOnSuccessListener(taskSnapshot -> {
                    storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String avatarUrl = uri.toString();
                        saveProfile(username, phone, address, avatarUrl);
                        dialogAvatarView = null;
                        dialog.dismiss();
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Lỗi tải ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveProfile(String username, String phone, String address, String avatarUrl) {
        String userId = FirebaseService.getInstance().getCurrentUserId();
        if (userId == null) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("username", username);
        updates.put("phone", phone);
        updates.put("address", address);
        if (avatarUrl != null) {
            updates.put("avatarUrl", avatarUrl);
        }

        FirebaseService.getInstance().getDb()
                .collection("users")
                .document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showSettingsDialog() {
        View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_settings, null);

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        SwitchMaterial switchDarkMode = dialogView.findViewById(R.id.switchDarkMode);
        Spinner spinnerLanguage = dialogView.findViewById(R.id.spinnerLanguage);
        TextView txtCurrentLanguage = dialogView.findViewById(R.id.txtCurrentLanguage);
        MaterialButton btnClose = dialogView.findViewById(R.id.btnClose);

        // Setup Dark Mode
        switchDarkMode.setChecked(preferenceManager.isDarkMode());
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferenceManager.setDarkMode(isChecked);
        });

        // Setup Language Spinner
        String[] languages = {"Tiếng Việt", "English"};
        String[] langCodes = {"vi", "en"};
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                languages
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLanguage.setAdapter(adapter);

        // Set current language
        String currentLang = preferenceManager.getLanguage();
        int langIndex = currentLang.equals("en") ? 1 : 0;
        spinnerLanguage.setSelection(langIndex);
        txtCurrentLanguage.setText(languages[langIndex]);

        spinnerLanguage.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String selectedLang = langCodes[position];
                if (!selectedLang.equals(preferenceManager.getLanguage())) {
                    preferenceManager.setLanguage(selectedLang);
                    txtCurrentLanguage.setText(languages[position]);
                    
                    // Recreate activity to apply language change
                    if (getActivity() != null) {
                        dialog.dismiss();
                        getActivity().recreate();
                    }
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}
