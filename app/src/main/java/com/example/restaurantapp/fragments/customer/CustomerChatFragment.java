package com.example.restaurantapp.fragments.customer;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.restaurantapp.R;
import com.example.restaurantapp.adapters.ChatAdapter;
import com.example.restaurantapp.models.ChatMessage;
import com.example.restaurantapp.services.ChatbotService;
import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class CustomerChatFragment extends Fragment {

    private RecyclerView recyclerMessages;
    private EditText edtMessage;
    private FloatingActionButton btnSend;
    private ImageButton btnClearChat;
    private LinearLayout typingIndicator;
    private HorizontalScrollView quickActionsContainer;
    
    private Chip chipMenu, chipPromotion, chipReservation, chipOrderStatus, chipContact;

    private ChatAdapter chatAdapter;
    private List<ChatMessage> messageList = new ArrayList<>();
    private ChatbotService chatbotService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_customer_chat, container, false);

        initViews(view);
        setupRecyclerView();
        setupListeners();
        
        // Initialize chatbot service
        chatbotService = new ChatbotService(requireContext());
        
        // Welcome message
        addBotMessage("Xin chào! 👋 Tôi là trợ lý ảo của nhà hàng.\n\nTôi có thể giúp bạn về thực đơn, đặt bàn, khuyến mãi, và nhiều thứ khác. Hãy hỏi tôi bất cứ điều gì!");
        
        // Show quick actions after welcome
        quickActionsContainer.setVisibility(View.VISIBLE);

        return view;
    }

    private void initViews(View view) {
        recyclerMessages = view.findViewById(R.id.recyclerMessages);
        edtMessage = view.findViewById(R.id.edtMessage);
        btnSend = view.findViewById(R.id.btnSend);
        btnClearChat = view.findViewById(R.id.btnClearChat);
        typingIndicator = view.findViewById(R.id.typingIndicator);
        quickActionsContainer = view.findViewById(R.id.quickActionsContainer);
        
        chipMenu = view.findViewById(R.id.chipMenu);
        chipPromotion = view.findViewById(R.id.chipPromotion);
        chipReservation = view.findViewById(R.id.chipReservation);
        chipOrderStatus = view.findViewById(R.id.chipOrderStatus);
        chipContact = view.findViewById(R.id.chipContact);
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setStackFromEnd(true);
        recyclerMessages.setLayoutManager(layoutManager);
        
        chatAdapter = new ChatAdapter(messageList);
        recyclerMessages.setAdapter(chatAdapter);
    }

    private void setupListeners() {
        // Send button
        btnSend.setOnClickListener(v -> sendMessage());
        
        // Enter key to send
        edtMessage.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });
        
        // Clear chat
        btnClearChat.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Xóa cuộc trò chuyện")
                    .setMessage("Bạn có muốn xóa tất cả tin nhắn?")
                    .setPositiveButton("Xóa", (dialog, which) -> {
                        messageList.clear();
                        chatAdapter.clearMessages();
                        addBotMessage("Cuộc trò chuyện đã được xóa. Tôi có thể giúp gì cho bạn?");
                        quickActionsContainer.setVisibility(View.VISIBLE);
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });
        
        // Quick action chips
        chipMenu.setOnClickListener(v -> sendQuickMessage("Cho tôi xem thực đơn"));
        chipPromotion.setOnClickListener(v -> sendQuickMessage("Có khuyến mãi gì không?"));
        chipReservation.setOnClickListener(v -> sendQuickMessage("Tôi muốn đặt bàn"));
        chipOrderStatus.setOnClickListener(v -> sendQuickMessage("Xem đơn hàng của tôi"));
        chipContact.setOnClickListener(v -> sendQuickMessage("Thông tin liên hệ"));
    }

    private void sendMessage() {
        String message = edtMessage.getText().toString().trim();
        if (message.isEmpty()) {
            return;
        }
        
        // Add user message
        addUserMessage(message);
        edtMessage.setText("");
        
        // Hide quick actions after first message
        quickActionsContainer.setVisibility(View.GONE);
        
        // Show typing indicator
        showTypingIndicator();
        
        // Get bot response
        chatbotService.processMessage(message, new ChatbotService.ChatCallback() {
            @Override
            public void onResponse(String response) {
                hideTypingIndicator();
                addBotMessage(response);
            }

            @Override
            public void onError(String error) {
                hideTypingIndicator();
                addBotMessage("Xin lỗi, có lỗi xảy ra. Vui lòng thử lại! 😅");
            }
        });
    }

    private void sendQuickMessage(String message) {
        edtMessage.setText(message);
        sendMessage();
    }

    private void addUserMessage(String text) {
        ChatMessage message = new ChatMessage(text, ChatMessage.TYPE_USER);
        messageList.add(message);
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        scrollToBottom();
    }

    private void addBotMessage(String text) {
        ChatMessage message = new ChatMessage(text, ChatMessage.TYPE_BOT);
        messageList.add(message);
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        scrollToBottom();
    }

    private void showTypingIndicator() {
        typingIndicator.setVisibility(View.VISIBLE);
    }

    private void hideTypingIndicator() {
        typingIndicator.setVisibility(View.GONE);
    }

    private void scrollToBottom() {
        if (messageList.size() > 0) {
            recyclerMessages.smoothScrollToPosition(messageList.size() - 1);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh chatbot data
        if (chatbotService != null) {
            chatbotService.refreshData();
        }
    }
}
