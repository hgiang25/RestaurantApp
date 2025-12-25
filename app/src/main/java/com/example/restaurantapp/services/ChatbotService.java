package com.example.restaurantapp.services;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.example.restaurantapp.api.FirebaseService;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * AI Assistant Service - Xử lý các câu hỏi của khách hàng
 * Tích hợp Gemini AI với fallback về FAQ nếu chưa cấu hình API Key
 */
public class ChatbotService {

    public interface ChatCallback {
        void onResponse(String response);
        void onError(String error);
    }

    private Context context;
    private Handler mainHandler;
    private Random random = new Random();
    private GeminiService geminiService;
    private boolean useGemini = true;  // Set false để chỉ dùng FAQ

    // Cache data từ Firestore
    private List<Map<String, Object>> menuCache = new ArrayList<>();
    private List<Map<String, Object>> promotionCache = new ArrayList<>();
    private String restaurantInfo = "";

    public ChatbotService(Context context) {
        this.context = context;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.geminiService = GeminiService.getInstance();
        loadDataFromFirestore();
    }

    private void loadDataFromFirestore() {
        // Load menu (không filter, load tất cả rồi check available)
        FirebaseService.getInstance().getDb().collection("menu")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    menuCache.clear();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        // Check available field
                        Boolean isAvailable = doc.getBoolean("available");
                        if (isAvailable == null) {
                            isAvailable = doc.getBoolean("isAvailable");
                        }
                        if (isAvailable != null && !isAvailable) {
                            continue; // Skip món không available
                        }
                        
                        Map<String, Object> item = new HashMap<>();
                        item.put("name", doc.getString("name"));
                        item.put("price", doc.getDouble("price"));
                        item.put("category", doc.getString("category"));
                        item.put("description", doc.getString("description"));
                        menuCache.add(item);
                    }
                });

        // Load promotions (không filter, load tất cả rồi check active)
        FirebaseService.getInstance().getDb().collection("promotions")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    promotionCache.clear();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        // Check active field
                        Boolean isActive = doc.getBoolean("active");
                        if (isActive == null) {
                            isActive = doc.getBoolean("isActive");
                        }
                        if (isActive != null && !isActive) {
                            continue; // Skip khuyến mãi không active
                        }
                        
                        Map<String, Object> promo = new HashMap<>();
                        promo.put("name", doc.getString("name"));
                        promo.put("description", doc.getString("description"));
                        promo.put("discountPercent", doc.getDouble("discountPercent"));
                        promo.put("code", doc.getString("code"));
                        promotionCache.add(promo);
                    }
                });
    }

    /**
     * Xử lý tin nhắn từ người dùng và trả về phản hồi
     * Ưu tiên sử dụng Gemini AI, fallback về FAQ nếu chưa cấu hình
     */
    public void processMessage(String userMessage, ChatCallback callback) {
        // Kiểm tra xem có sử dụng Gemini không
        if (useGemini && geminiService.isConfigured()) {
            // Sử dụng Gemini AI
            geminiService.sendMessage(userMessage, new GeminiService.GeminiCallback() {
                @Override
                public void onSuccess(String response) {
                    mainHandler.post(() -> callback.onResponse(response));
                }

                @Override
                public void onError(String error) {
                    // Fallback về FAQ nếu Gemini lỗi
                    mainHandler.post(() -> {
                        String fallbackResponse = generateResponse(userMessage.toLowerCase().trim());
                        callback.onResponse(fallbackResponse + "\n\n_(Trả lời từ hệ thống FAQ)_");
                    });
                }
            });
        } else {
            // Sử dụng FAQ (rule-based)
            int delay = 300 + random.nextInt(500);
            mainHandler.postDelayed(() -> {
                String response = generateResponse(userMessage.toLowerCase().trim());
                callback.onResponse(response);
            }, delay);
        }
    }

    private String generateResponse(String message) {
        // Greeting patterns
        if (matchesAny(message, "xin chào", "hello", "hi", "chào", "hey", "alo")) {
            return getRandomGreeting();
        }

        // Menu related
        if (matchesAny(message, "thực đơn", "menu", "món ăn", "có gì", "bán gì", "món gì")) {
            return getMenuResponse();
        }

        // Price related
        if (matchesAny(message, "giá", "bao nhiêu", "price", "tiền")) {
            return getPriceResponse(message);
        }

        // Promotion related
        if (matchesAny(message, "khuyến mãi", "giảm giá", "ưu đãi", "voucher", "mã giảm", "promotion", "sale")) {
            return getPromotionResponse();
        }

        // Reservation related
        if (matchesAny(message, "đặt bàn", "book", "reservation", "đặt chỗ", "giữ bàn")) {
            return getReservationResponse();
        }

        // Order status
        if (matchesAny(message, "đơn hàng", "order", "trạng thái", "status", "đã đặt")) {
            return getOrderStatusResponse();
        }

        // Hours/Location
        if (matchesAny(message, "giờ mở cửa", "mở cửa", "đóng cửa", "thời gian", "hours", "khi nào")) {
            return getHoursResponse();
        }

        if (matchesAny(message, "địa chỉ", "ở đâu", "location", "chỗ nào", "vị trí")) {
            return getLocationResponse();
        }

        // Contact
        if (matchesAny(message, "liên hệ", "contact", "hotline", "số điện thoại", "gọi", "phone")) {
            return getContactResponse();
        }

        // Recommend
        if (matchesAny(message, "gợi ý", "recommend", "nên ăn", "ngon", "best", "đề xuất", "món nào")) {
            return getRecommendationResponse();
        }

        // Thanks
        if (matchesAny(message, "cảm ơn", "thanks", "thank you", "cám ơn")) {
            return getRandomThanks();
        }

        // Help
        if (matchesAny(message, "help", "giúp", "hướng dẫn", "hỗ trợ", "làm sao")) {
            return getHelpResponse();
        }

        // Payment
        if (matchesAny(message, "thanh toán", "payment", "trả tiền", "pay", "chuyển khoản")) {
            return getPaymentResponse();
        }

        // Loyalty/Points
        if (matchesAny(message, "điểm", "point", "loyalty", "tích điểm", "thẻ thành viên")) {
            return getLoyaltyResponse();
        }

        // Default response
        return getDefaultResponse();
    }

    private boolean matchesAny(String message, String... keywords) {
        for (String keyword : keywords) {
            if (message.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String getRandomGreeting() {
        String[] greetings = {
            "Xin chào! 👋 Tôi là trợ lý ảo của nhà hàng. Tôi có thể giúp gì cho bạn hôm nay?",
            "Chào bạn! 😊 Rất vui được hỗ trợ bạn. Bạn muốn xem thực đơn, đặt bàn hay có câu hỏi gì không?",
            "Hello! 🤖 Tôi sẵn sàng giúp bạn. Hãy hỏi tôi về thực đơn, khuyến mãi, hoặc đặt bàn nhé!",
            "Xin chào quý khách! 🎉 Hôm nay bạn muốn thưởng thức món gì?"
        };
        return greetings[random.nextInt(greetings.length)];
    }

    private String getMenuResponse() {
        StringBuilder sb = new StringBuilder();
        sb.append("📋 **THỰC ĐƠN NHÀ HÀNG**\n\n");

        if (menuCache.isEmpty()) {
            sb.append("Xin lỗi, tôi đang tải thực đơn. Vui lòng thử lại sau hoặc vào tab Menu để xem chi tiết!");
            return sb.toString();
        }

        // Group by category
        Map<String, List<Map<String, Object>>> grouped = new HashMap<>();
        for (Map<String, Object> item : menuCache) {
            String category = (String) item.get("category");
            if (category == null) category = "Khác";
            grouped.computeIfAbsent(category, k -> new ArrayList<>()).add(item);
        }

        int count = 0;
        for (Map.Entry<String, List<Map<String, Object>>> entry : grouped.entrySet()) {
            if (count >= 3) {
                sb.append("\n...và nhiều món khác nữa!");
                break;
            }
            sb.append("**").append(entry.getKey()).append(":**\n");
            for (Map<String, Object> item : entry.getValue()) {
                String name = (String) item.get("name");
                Double price = (Double) item.get("price");
                if (name != null && price != null) {
                    sb.append("• ").append(name).append(" - ")
                      .append(String.format(Locale.getDefault(), "%,.0fđ", price)).append("\n");
                }
            }
            sb.append("\n");
            count++;
        }

        sb.append("\n👉 Vào tab **Menu** để xem đầy đủ và đặt món nhé!");
        return sb.toString();
    }

    private String getPriceResponse(String message) {
        // Try to find specific item
        for (Map<String, Object> item : menuCache) {
            String name = (String) item.get("name");
            if (name != null && message.contains(name.toLowerCase())) {
                Double price = (Double) item.get("price");
                String desc = (String) item.get("description");
                return String.format("💰 **%s**: %,.0fđ\n\n%s\n\nBạn muốn đặt món này không?", 
                        name, price, desc != null ? desc : "Món ăn ngon tuyệt!");
            }
        }

        return "💰 Để biết giá cụ thể, bạn hãy cho tôi biết tên món bạn quan tâm, hoặc vào tab Menu để xem đầy đủ thực đơn và giá nhé!";
    }

    private String getPromotionResponse() {
        StringBuilder sb = new StringBuilder();
        sb.append("🎁 **KHUYẾN MÃI HIỆN TẠI**\n\n");

        if (promotionCache.isEmpty()) {
            sb.append("Hiện tại chưa có chương trình khuyến mãi nào.\n\nHãy theo dõi nhà hàng để cập nhật ưu đãi mới nhất nhé! 💝");
            return sb.toString();
        }

        for (Map<String, Object> promo : promotionCache) {
            String name = (String) promo.get("name");
            String desc = (String) promo.get("description");
            Double discount = (Double) promo.get("discountPercent");
            String code = (String) promo.get("code");

            sb.append("🏷️ **").append(name).append("**\n");
            if (discount != null && discount > 0) {
                sb.append("   Giảm ").append(String.format("%.0f", discount)).append("%\n");
            }
            if (desc != null) {
                sb.append("   ").append(desc).append("\n");
            }
            if (code != null) {
                sb.append("   Mã: `").append(code).append("`\n");
            }
            sb.append("\n");
        }

        sb.append("👉 Nhập mã khi thanh toán để được giảm giá!");
        return sb.toString();
    }

    private String getReservationResponse() {
        return "📅 **ĐẶT BÀN**\n\n" +
               "Để đặt bàn, bạn có thể:\n\n" +
               "1️⃣ Vào tab **Đặt bàn** trong app\n" +
               "2️⃣ Chọn ngày, giờ và số người\n" +
               "3️⃣ Xác nhận đặt bàn\n\n" +
               "Hoặc gọi hotline: **1900-xxxx** để được hỗ trợ trực tiếp!\n\n" +
               "💡 Nên đặt trước 2-3 tiếng để đảm bảo có bàn nhé!";
    }

    private String getOrderStatusResponse() {
        return "📦 **THEO DÕI ĐƠN HÀNG**\n\n" +
               "Để xem trạng thái đơn hàng:\n\n" +
               "1️⃣ Vào tab **Đơn hàng** trong app\n" +
               "2️⃣ Xem danh sách các đơn đã đặt\n" +
               "3️⃣ Theo dõi trạng thái realtime\n\n" +
               "Các trạng thái:\n" +
               "• ⏳ Chờ xác nhận\n" +
               "• ✅ Đã xác nhận\n" +
               "• 🍳 Đang chuẩn bị\n" +
               "• 🍽️ Đã phục vụ\n" +
               "• 💰 Đã thanh toán";
    }

    private String getHoursResponse() {
        return "🕐 **GIỜ MỞ CỬA**\n\n" +
               "• Thứ 2 - Thứ 6: 10:00 - 22:00\n" +
               "• Thứ 7 - Chủ nhật: 09:00 - 23:00\n" +
               "• Ngày lễ: 09:00 - 23:00\n\n" +
               "🍽️ Last order: trước 21:30\n\n" +
               "Chúng tôi luôn sẵn sàng phục vụ bạn! 😊";
    }

    private String getLocationResponse() {
        return "📍 **ĐỊA CHỈ NHÀ HÀNG**\n\n" +
               "🏠 123 Đường ABC, Quận XYZ, TP.HCM\n\n" +
               "🚗 Có bãi đỗ xe miễn phí\n" +
               "🚌 Gần trạm bus tuyến 01, 54, 152\n\n" +
               "👉 Mở Google Maps để dẫn đường!";
    }

    private String getContactResponse() {
        return "📞 **LIÊN HỆ**\n\n" +
               "• Hotline: **1900-xxxx**\n" +
               "• Điện thoại: 028-xxxx-xxxx\n" +
               "• Email: info@restaurant.com\n" +
               "• Facebook: /restaurantapp\n" +
               "• Zalo: 09xx-xxx-xxx\n\n" +
               "Chúng tôi sẵn sàng hỗ trợ 24/7! 💬";
    }

    private String getRecommendationResponse() {
        StringBuilder sb = new StringBuilder();
        sb.append("⭐ **MÓN ĂN GỢI Ý HÔM NAY**\n\n");

        if (menuCache.isEmpty()) {
            sb.append("Hôm nay bạn nên thử các món signature của nhà hàng!\n\n");
            sb.append("Vào tab Menu để khám phá nhé! 🍴");
            return sb.toString();
        }

        // Random 3 items
        List<Map<String, Object>> shuffled = new ArrayList<>(menuCache);
        java.util.Collections.shuffle(shuffled);
        
        int count = Math.min(3, shuffled.size());
        for (int i = 0; i < count; i++) {
            Map<String, Object> item = shuffled.get(i);
            String name = (String) item.get("name");
            Double price = (Double) item.get("price");
            if (name != null) {
                sb.append("🍴 **").append(name).append("**");
                if (price != null) {
                    sb.append(" - ").append(String.format(Locale.getDefault(), "%,.0fđ", price));
                }
                sb.append("\n");
            }
        }

        sb.append("\n💡 Đây là các món được yêu thích! Bạn muốn đặt món nào?");
        return sb.toString();
    }

    private String getRandomThanks() {
        String[] responses = {
            "Không có gì! 😊 Rất vui được giúp bạn. Nếu cần gì thêm cứ hỏi tôi nhé!",
            "Cảm ơn bạn đã ghé thăm! 💝 Chúc bạn ngon miệng!",
            "Rất vui vì đã hỗ trợ được bạn! 🎉 Hẹn gặp lại!",
            "Không có chi! Chúc bạn có một bữa ăn tuyệt vời! 🍽️"
        };
        return responses[random.nextInt(responses.length)];
    }

    private String getHelpResponse() {
        return "🆘 **TÔI CÓ THỂ GIÚP GÌ?**\n\n" +
               "Hỏi tôi về:\n\n" +
               "📋 **Thực đơn** - Xem các món ăn\n" +
               "💰 **Giá cả** - Hỏi giá món cụ thể\n" +
               "🎁 **Khuyến mãi** - Ưu đãi hiện có\n" +
               "📅 **Đặt bàn** - Hướng dẫn đặt bàn\n" +
               "📦 **Đơn hàng** - Theo dõi đơn\n" +
               "🕐 **Giờ mở cửa** - Thời gian phục vụ\n" +
               "📍 **Địa chỉ** - Vị trí nhà hàng\n" +
               "📞 **Liên hệ** - Hotline, email\n" +
               "⭐ **Gợi ý món** - Món ngon hôm nay\n\n" +
               "Hãy thử hỏi đi nào! 😊";
    }

    private String getPaymentResponse() {
        return "💳 **THANH TOÁN**\n\n" +
               "Chúng tôi chấp nhận:\n\n" +
               "💵 Tiền mặt\n" +
               "💳 Thẻ Visa/Mastercard\n" +
               "🏦 Chuyển khoản ngân hàng\n" +
               "📱 Ví MoMo, ZaloPay, VNPay\n\n" +
               "💡 Thanh toán online: Nhấn nút **Thanh toán** trong đơn hàng và chọn phương thức!";
    }

    private String getLoyaltyResponse() {
        return "🏆 **CHƯƠNG TRÌNH THÀNH VIÊN**\n\n" +
               "Tích điểm với mỗi đơn hàng:\n\n" +
               "• 10.000đ = 1 điểm\n" +
               "• 100 điểm = Voucher 50.000đ\n" +
               "• 500 điểm = Voucher 300.000đ\n\n" +
               "🎁 Thành viên VIP còn được:\n" +
               "• Giảm 10% tất cả đơn hàng\n" +
               "• Ưu tiên đặt bàn\n" +
               "• Quà sinh nhật đặc biệt\n\n" +
               "👉 Xem điểm của bạn trong tab **Hồ sơ**!";
    }

    private String getDefaultResponse() {
        String[] responses = {
            "Xin lỗi, tôi chưa hiểu câu hỏi của bạn. 🤔\n\nBạn có thể hỏi về thực đơn, đặt bàn, khuyến mãi, hoặc gõ \"help\" để xem các chủ đề tôi hỗ trợ!",
            "Hmm, tôi cần thêm thông tin. 😊\n\nThử hỏi cụ thể hơn như: \"Thực đơn có gì?\", \"Đặt bàn như thế nào?\", hoặc \"Khuyến mãi gì?\"",
            "Tôi chưa được lập trình để trả lời câu này. 🤖\n\nNhưng đừng lo, bạn có thể liên hệ hotline **1900-xxxx** để được hỗ trợ trực tiếp!",
            "Câu hỏi thú vị! 🧐\n\nTiếc là tôi chưa có câu trả lời. Hãy thử hỏi về menu, giá cả, hoặc đặt bàn nhé!"
        };
        return responses[random.nextInt(responses.length)];
    }

    /**
     * Refresh cache data
     */
    public void refreshData() {
        loadDataFromFirestore();
    }
}
