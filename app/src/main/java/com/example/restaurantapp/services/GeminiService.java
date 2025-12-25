package com.example.restaurantapp.services;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.restaurantapp.api.FirebaseService;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
    private static final String GEMINI_API_KEY = "YOUR_GEMINI_API_KEY";
    
    private static final String GEMINI_API_URL = 
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=";
    
    private static GeminiService instance;
    private OkHttpClient client;
    private Gson gson;
    private Handler mainHandler;
    private String systemPrompt;
    
    // Cache dữ liệu từ app để đưa vào context
    private StringBuilder menuInfo = new StringBuilder();
    private StringBuilder promoInfo = new StringBuilder();
    private StringBuilder tableInfo = new StringBuilder();
    private StringBuilder categoryInfo = new StringBuilder();
    private int totalMenuItems = 0;
    private int availableTables = 0;
    private boolean dataLoaded = false;
    
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
        AtomicInteger loadCounter = new AtomicInteger(4); // Số lượng collection cần load
        
        // Load menu để đưa vào system prompt (không filter, load tất cả rồi check available)
        FirebaseService.getInstance().getDb().collection("menu")
            .get()
            .addOnSuccessListener(querySnapshot -> {
                menuInfo = new StringBuilder();
                categoryInfo = new StringBuilder();
                menuInfo.append("THỰC ĐƠN NHÀ HÀNG (Dữ liệu thực từ hệ thống):\n");
                
                // Nhóm theo category
                java.util.Map<String, java.util.List<String>> menuByCategory = new java.util.HashMap<>();
                int availableCount = 0;
                
                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    // Check available field (có thể là "available" hoặc "isAvailable")
                    Boolean isAvailable = doc.getBoolean("available");
                    if (isAvailable == null) {
                        isAvailable = doc.getBoolean("isAvailable");
                    }
                    // Nếu không có field hoặc là true thì coi như available
                    if (isAvailable != null && !isAvailable) {
                        continue; // Skip món không available
                    }
                    
                    String name = doc.getString("name");
                    Double price = doc.getDouble("price");
                    String category = doc.getString("category");
                    String desc = doc.getString("description");
                    Long rating = doc.getLong("rating");
                    
                    if (name != null && price != null) {
                        availableCount++;
                        String menuItem = String.format(Locale.getDefault(), 
                            "  + %s: %,.0fđ", name, price);
                        if (desc != null && !desc.isEmpty()) {
                            menuItem += " (" + desc + ")";
                        }
                        if (rating != null && rating > 0) {
                            menuItem += " ⭐" + rating;
                        }
                        
                        String cat = category != null ? category : "Khác";
                        menuByCategory.computeIfAbsent(cat, k -> new java.util.ArrayList<>()).add(menuItem);
                    }
                }
                
                // Format theo category
                for (java.util.Map.Entry<String, java.util.List<String>> entry : menuByCategory.entrySet()) {
                    menuInfo.append("📌 ").append(entry.getKey().toUpperCase()).append(":\n");
                    for (String item : entry.getValue()) {
                        menuInfo.append(item).append("\n");
                    }
                    menuInfo.append("\n");
                }
                
                totalMenuItems = availableCount;
                if (!menuByCategory.isEmpty()) {
                    categoryInfo.append("Danh mục: ").append(String.join(", ", menuByCategory.keySet()));
                }
                
                Log.d(TAG, "Loaded " + availableCount + " available menu items from " + querySnapshot.size() + " total");
                checkAndBuildPrompt(loadCounter.decrementAndGet());
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to load menu: " + e.getMessage());
                checkAndBuildPrompt(loadCounter.decrementAndGet());
            });
        
        // Load promotions
        FirebaseService.getInstance().getDb().collection("promotions")
            .whereEqualTo("active", true)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                promoInfo = new StringBuilder();
                if (!querySnapshot.isEmpty()) {
                    promoInfo.append("KHUYẾN MÃI ĐANG ÁP DỤNG (Dữ liệu thực từ hệ thống):\n");
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String name = doc.getString("name");
                        String desc = doc.getString("description");
                        Double discount = doc.getDouble("discountPercent");
                        Double maxDiscount = doc.getDouble("maxDiscount");
                        Double minOrder = doc.getDouble("minOrderAmount");
                        String code = doc.getString("code");
                        Timestamp validUntil = doc.getTimestamp("validUntil");
                        
                        if (name != null) {
                            promoInfo.append("🎁 ").append(name);
                            if (discount != null && discount > 0) {
                                promoInfo.append(String.format(Locale.getDefault(), 
                                    " - Giảm %.0f%%", discount));
                            }
                            if (maxDiscount != null && maxDiscount > 0) {
                                promoInfo.append(String.format(Locale.getDefault(), 
                                    " (tối đa %,.0fđ)", maxDiscount));
                            }
                            promoInfo.append("\n");
                            if (code != null && !code.isEmpty()) {
                                promoInfo.append("   Mã khuyến mãi: ").append(code).append("\n");
                            }
                            if (minOrder != null && minOrder > 0) {
                                promoInfo.append(String.format(Locale.getDefault(),
                                    "   Đơn tối thiểu: %,.0fđ\n", minOrder));
                            }
                            if (validUntil != null) {
                                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                                promoInfo.append("   Hết hạn: ").append(sdf.format(validUntil.toDate())).append("\n");
                            }
                            if (desc != null && !desc.isEmpty()) {
                                promoInfo.append("   ").append(desc).append("\n");
                            }
                        }
                    }
                } else {
                    promoInfo.append("KHUYẾN MÃI: Hiện tại không có chương trình khuyến mãi nào.\n");
                }
                checkAndBuildPrompt(loadCounter.decrementAndGet());
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to load promotions: " + e.getMessage());
                checkAndBuildPrompt(loadCounter.decrementAndGet());
            });
        
        // Load thông tin bàn
        FirebaseService.getInstance().getDb().collection("tables")
            .get()
            .addOnSuccessListener(querySnapshot -> {
                tableInfo = new StringBuilder();
                tableInfo.append("THÔNG TIN BÀN (Dữ liệu thực từ hệ thống):\n");
                
                int free = 0, occupied = 0, reserved = 0;
                StringBuilder tableDetails = new StringBuilder();
                
                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    String name = doc.getString("name");
                    Long capacity = doc.getLong("capacity");
                    String status = doc.getString("status");
                    
                    if (name != null) {
                        String statusVN = "Trống";
                        if ("occupied".equals(status)) {
                            statusVN = "Đang sử dụng";
                            occupied++;
                        } else if ("reserved".equals(status)) {
                            statusVN = "Đã đặt trước";
                            reserved++;
                        } else {
                            free++;
                        }
                        
                        tableDetails.append(String.format("- %s: %d chỗ - %s\n", 
                            name, capacity != null ? capacity.intValue() : 0, statusVN));
                    }
                }
                
                availableTables = free;
                tableInfo.append(String.format("Tổng cộng: %d bàn (Trống: %d, Đang dùng: %d, Đã đặt: %d)\n",
                    querySnapshot.size(), free, occupied, reserved));
                tableInfo.append(tableDetails);
                
                Log.d(TAG, "Loaded " + querySnapshot.size() + " tables");
                checkAndBuildPrompt(loadCounter.decrementAndGet());
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to load tables: " + e.getMessage());
                checkAndBuildPrompt(loadCounter.decrementAndGet());
            });
            
        // Load đặt bàn hôm nay (đơn giản hóa query để tránh lỗi)
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        FirebaseService.getInstance().getDb().collection("reservations")
            .whereEqualTo("date", today)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                StringBuilder reservationInfo = new StringBuilder();
                int count = 0;
                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    String status = doc.getString("status");
                    // Chỉ lấy pending hoặc confirmed
                    if ("pending".equals(status) || "confirmed".equals(status)) {
                        String tableName = doc.getString("tableName");
                        String time = doc.getString("time");
                        Long guests = doc.getLong("guests");
                        
                        String statusVN = "pending".equals(status) ? "Chờ xác nhận" : "Đã xác nhận";
                        reservationInfo.append(String.format("- %s lúc %s (%d khách) - %s\n",
                            tableName != null ? tableName : "N/A", 
                            time != null ? time : "N/A", 
                            guests != null ? guests.intValue() : 0, 
                            statusVN));
                        count++;
                    }
                }
                if (count > 0) {
                    tableInfo.append("\n📅 ĐẶT BÀN HÔM NAY (").append(today).append("):\n");
                    tableInfo.append(reservationInfo);
                }
                Log.d(TAG, "Loaded " + count + " reservations for today");
                checkAndBuildPrompt(loadCounter.decrementAndGet());
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to load reservations: " + e.getMessage());
                checkAndBuildPrompt(loadCounter.decrementAndGet());
            });
    }
    
    private void checkAndBuildPrompt(int remaining) {
        Log.d(TAG, "checkAndBuildPrompt: remaining=" + remaining + 
            ", menuItems=" + totalMenuItems + 
            ", tables=" + availableTables);
        if (remaining <= 0) {
            dataLoaded = true;
            buildSystemPrompt();
            Log.d(TAG, "System prompt built with " + totalMenuItems + " menu items");
        }
    }
    
    private void buildSystemPrompt() {
        String currentDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
        String currentTime = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        
        // Đảm bảo có thông tin mặc định nếu chưa load được
        String menuContent = menuInfo.length() > 50 ? menuInfo.toString() : "Đang cập nhật thực đơn...\n";
        String promoContent = promoInfo.length() > 0 ? promoInfo.toString() : "Đang cập nhật khuyến mãi...\n";
        String tableContent = tableInfo.length() > 0 ? tableInfo.toString() : "Đang cập nhật thông tin bàn...\n";
        
        systemPrompt = "Bạn là trợ lý AI của nhà hàng. QUAN TRỌNG: Bạn CHỈ được trả lời dựa trên dữ liệu thực được cung cấp bên dưới.\n" +
            "KHÔNG ĐƯỢC bịa ra thông tin về món ăn, giá cả, khuyến mãi hay bàn nếu không có trong dữ liệu.\n" +
            "Nếu khách hỏi về thứ không có trong dữ liệu, hãy nói rõ là không có thông tin và hướng dẫn liên hệ hotline.\n\n" +
            
            "═══════════════════════════════════════\n" +
            "📍 THÔNG TIN NHÀ HÀNG\n" +
            "═══════════════════════════════════════\n" +
            "- Tên: Nhà hàng Việt Nam\n" +
            "- Giờ mở cửa: 8:00 - 22:00 (Thứ 2 - Chủ nhật)\n" +
            "- Địa chỉ: 123 Đường ABC, Quận 1, TP.HCM\n" +
            "- Hotline: 1900-1234\n" +
            "- Thanh toán: Tiền mặt, Chuyển khoản, MoMo, ZaloPay\n" +
            "- Thời gian hiện tại: " + currentDate + " " + currentTime + "\n\n" +
            
            "═══════════════════════════════════════\n" +
            "🍽️ " + menuContent + 
            "(Tổng: " + totalMenuItems + " món)\n" +
            categoryInfo.toString() + "\n\n" +
            
            "═══════════════════════════════════════\n" +
            "🎉 " + promoContent + "\n" +
            
            "═══════════════════════════════════════\n" +
            "🪑 " + tableContent + "\n" +
            
            "═══════════════════════════════════════\n" +
            "📋 QUY TẮC TRẢ LỜI (BẮT BUỘC TUÂN THỦ):\n" +
            "═══════════════════════════════════════\n" +
            "1. CHỈ trả lời dựa trên dữ liệu thực ở trên - KHÔNG bịa thêm món, giá hay thông tin\n" +
            "2. Nếu khách hỏi món KHÔNG có trong menu → Nói 'Xin lỗi, món này không có trong thực đơn của chúng tôi' và gợi ý món tương tự nếu có\n" +
            "3. Nếu không có thông tin → Hướng dẫn khách gọi hotline 1900-1234\n" +
            "4. Luôn nói giá chính xác như trong dữ liệu, format: X.XXXđ\n" +
            "5. Khi nói về bàn trống, dựa vào thông tin bàn thực tế ở trên\n" +
            "6. Với đặt bàn: Khuyến khích đặt qua app hoặc gọi hotline\n" +
            "7. Trả lời bằng tiếng Việt, thân thiện, ngắn gọn (2-4 câu)\n" +
            "8. Sử dụng emoji phù hợp 🍜🍲🥗🍝🎁\n" +
            "9. Hiện có " + availableTables + " bàn trống\n";
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
        dataLoaded = false;
        loadRestaurantData();
    }
    
    /**
     * Kiểm tra xem dữ liệu đã được load chưa
     */
    public boolean isDataLoaded() {
        return dataLoaded;
    }
    
    /**
     * Lấy thông tin tóm tắt về dữ liệu đã load
     */
    public String getDataSummary() {
        return String.format("Menu: %d món | Bàn trống: %d", totalMenuItems, availableTables);
    }
    
    /**
     * Kiểm tra xem Gemini đã được cấu hình chưa
     */
    public boolean isConfigured() {
        return !GEMINI_API_KEY.equals("YOUR_GEMINI_API_KEY");
    }
}
