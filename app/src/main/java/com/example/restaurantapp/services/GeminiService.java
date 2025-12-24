package com.example.restaurantapp.services;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.restaurantapp.api.FirebaseService;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Service để tích hợp Google Gemini AI qua REST API
 */
public class GeminiService {
    private static final String TAG = "GeminiService";
    
    // ⚠️ QUAN TRỌNG: Thay API Key của bạn vào đây
    // Lấy API Key tại: https://aistudio.google.com/app/apikey
    private static final String GEMINI_API_KEY = "AIzaSyDXBX6JztgIpdit-P-Rq9KAE4nd9zkg9gc";
    
    private static final String GEMINI_API_URL = 
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=";
    
    private static GeminiService instance;
    private OkHttpClient client;
    private Gson gson;
    private Handler mainHandler;
    private String systemPrompt;
    
    // Cache menu data để đưa vào context
    private StringBuilder menuInfo = new StringBuilder();
    private StringBuilder promoInfo = new StringBuilder();
    
    public interface GeminiCallback {
        void onSuccess(String response);
        void onError(String error);
    }
    
    private GeminiService() {
        client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
        gson = new Gson();
        mainHandler = new Handler(Looper.getMainLooper());
        buildSystemPrompt(); // Build default prompt first
        loadRestaurantData(); // Then load data async
    }
    
    public static synchronized GeminiService getInstance() {
        if (instance == null) {
            instance = new GeminiService();
        }
        return instance;
    }
    
    private void loadRestaurantData() {
        // Load menu để đưa vào system prompt
        FirebaseService.getInstance().getDb().collection("menu")
            .whereEqualTo("isAvailable", true)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                menuInfo = new StringBuilder();
                menuInfo.append("THỰC ĐƠN NHÀ HÀNG:\n");
                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    String name = doc.getString("name");
                    Double price = doc.getDouble("price");
                    String category = doc.getString("category");
                    String desc = doc.getString("description");
                    
                    if (name != null && price != null) {
                        menuInfo.append(String.format(Locale.getDefault(), 
                            "- %s (%s): %,.0fđ", name, category, price));
                        if (desc != null && !desc.isEmpty()) {
                            menuInfo.append(" - ").append(desc);
                        }
                        menuInfo.append("\n");
                    }
                }
                Log.d(TAG, "Loaded " + querySnapshot.size() + " menu items");
                buildSystemPrompt();
            })
            .addOnFailureListener(e -> Log.e(TAG, "Failed to load menu: " + e.getMessage()));
        
        // Load promotions
        FirebaseService.getInstance().getDb().collection("promotions")
            .whereEqualTo("isActive", true)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                promoInfo = new StringBuilder();
                if (!querySnapshot.isEmpty()) {
                    promoInfo.append("\nKHUYẾN MÃI HIỆN TẠI:\n");
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String name = doc.getString("name");
                        String desc = doc.getString("description");
                        Double discount = doc.getDouble("discountPercent");
                        String code = doc.getString("code");
                        
                        if (name != null) {
                            promoInfo.append("- ").append(name);
                            if (discount != null) {
                                promoInfo.append(String.format(Locale.getDefault(), 
                                    " (Giảm %.0f%%)", discount));
                            }
                            if (code != null) {
                                promoInfo.append(" - Mã: ").append(code);
                            }
                            if (desc != null) {
                                promoInfo.append(" - ").append(desc);
                            }
                            promoInfo.append("\n");
                        }
                    }
                }
                buildSystemPrompt();
            });
    }
    
    private void buildSystemPrompt() {
        systemPrompt = "Bạn là trợ lý AI thân thiện của nhà hàng Việt Nam. " +
            "Nhiệm vụ của bạn là hỗ trợ khách hàng về thực đơn, giá cả, khuyến mãi, đặt bàn và các thắc mắc khác.\n\n" +
            
            "THÔNG TIN NHÀ HÀNG:\n" +
            "- Tên: Nhà hàng Việt Nam\n" +
            "- Giờ mở cửa: 8:00 - 22:00 (Thứ 2 - Chủ nhật)\n" +
            "- Địa chỉ: 123 Đường ABC, Quận 1, TP.HCM\n" +
            "- Hotline: 1900-1234\n" +
            "- Thanh toán: Tiền mặt, Chuyển khoản, MoMo, ZaloPay\n\n" +
            
            menuInfo.toString() + "\n" +
            promoInfo.toString() + "\n" +
            
            "HƯỚNG DẪN TRẢ LỜI:\n" +
            "1. Luôn trả lời bằng tiếng Việt, thân thiện và lịch sự\n" +
            "2. Sử dụng emoji phù hợp để tạo không khí vui vẻ\n" +
            "3. Nếu khách hỏi về món không có trong menu, gợi ý món tương tự\n" +
            "4. Nếu không biết câu trả lời, hướng dẫn khách liên hệ hotline\n" +
            "5. Giữ câu trả lời ngắn gọn, dễ hiểu (tối đa 3-4 câu nếu có thể)\n" +
            "6. Khi nói về giá, luôn format số tiền có dấu chấm ngăn cách hàng nghìn và đơn vị 'đ'\n" +
            "7. Khuyến khích khách đặt món qua app hoặc đặt bàn trước";
    }
    
    /**
     * Gửi tin nhắn đến Gemini và nhận phản hồi
     */
    public void sendMessage(String userMessage, GeminiCallback callback) {
        if (GEMINI_API_KEY.equals("YOUR_GEMINI_API_KEY")) {
            callback.onError("Vui lòng cấu hình API Key của Gemini trong GeminiService.java");
            return;
        }
        
        try {
            // Tạo request body theo format Gemini API
            JsonObject requestBody = new JsonObject();
            
            // System instruction
            JsonObject systemInstruction = new JsonObject();
            JsonArray systemParts = new JsonArray();
            JsonObject systemText = new JsonObject();
            systemText.addProperty("text", systemPrompt);
            systemParts.add(systemText);
            systemInstruction.add("parts", systemParts);
            requestBody.add("system_instruction", systemInstruction);
            
            // Contents (user message)
            JsonArray contents = new JsonArray();
            JsonObject content = new JsonObject();
            content.addProperty("role", "user");
            JsonArray parts = new JsonArray();
            JsonObject textPart = new JsonObject();
            textPart.addProperty("text", userMessage);
            parts.add(textPart);
            content.add("parts", parts);
            contents.add(content);
            requestBody.add("contents", contents);
            
            // Generation config
            JsonObject generationConfig = new JsonObject();
            generationConfig.addProperty("temperature", 0.7);
            generationConfig.addProperty("maxOutputTokens", 500);
            requestBody.add("generationConfig", generationConfig);
            
            String jsonBody = gson.toJson(requestBody);
            Log.d(TAG, "Request: " + jsonBody);
            
            RequestBody body = RequestBody.create(
                jsonBody, 
                MediaType.parse("application/json")
            );
            
            Request request = new Request.Builder()
                .url(GEMINI_API_URL + GEMINI_API_KEY)
                .post(body)
                .build();
            
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e(TAG, "API call failed: " + e.getMessage());
                    mainHandler.post(() -> callback.onError("Lỗi kết nối: " + e.getMessage()));
                }
                
                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    Log.d(TAG, "Response: " + responseBody);
                    
                    if (response.isSuccessful()) {
                        try {
                            String text = parseGeminiResponse(responseBody);
                            mainHandler.post(() -> callback.onSuccess(text));
                        } catch (Exception e) {
                            Log.e(TAG, "Parse error: " + e.getMessage());
                            mainHandler.post(() -> callback.onError("Lỗi xử lý phản hồi"));
                        }
                    } else {
                        Log.e(TAG, "API error: " + response.code() + " - " + responseBody);
                        mainHandler.post(() -> callback.onError("Lỗi API: " + response.code()));
                    }
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error sending message: " + e.getMessage());
            callback.onError("Lỗi: " + e.getMessage());
        }
    }
    
    private String parseGeminiResponse(String responseBody) {
        JsonObject response = gson.fromJson(responseBody, JsonObject.class);
        
        if (response.has("candidates")) {
            JsonArray candidates = response.getAsJsonArray("candidates");
            if (candidates.size() > 0) {
                JsonObject candidate = candidates.get(0).getAsJsonObject();
                if (candidate.has("content")) {
                    JsonObject content = candidate.getAsJsonObject("content");
                    if (content.has("parts")) {
                        JsonArray parts = content.getAsJsonArray("parts");
                        if (parts.size() > 0) {
                            JsonObject part = parts.get(0).getAsJsonObject();
                            if (part.has("text")) {
                                return part.get("text").getAsString().trim();
                            }
                        }
                    }
                }
            }
        }
        
        // Check for error
        if (response.has("error")) {
            JsonObject error = response.getAsJsonObject("error");
            String message = error.has("message") ? error.get("message").getAsString() : "Unknown error";
            throw new RuntimeException(message);
        }
        
        return "Xin lỗi, tôi không thể trả lời lúc này.";
    }
    
    /**
     * Làm mới dữ liệu nhà hàng (gọi khi menu hoặc khuyến mãi thay đổi)
     */
    public void refreshData() {
        loadRestaurantData();
    }
    
    /**
     * Kiểm tra xem Gemini đã được cấu hình chưa
     */
    public boolean isConfigured() {
        return !GEMINI_API_KEY.equals("YOUR_GEMINI_API_KEY");
    }
}
