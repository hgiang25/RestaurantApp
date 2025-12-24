package com.example.restaurantapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.restaurantapp.R;
import com.example.restaurantapp.models.ChatMessage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private List<ChatMessage> messageList;
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public ChatAdapter(List<ChatMessage> messageList) {
        this.messageList = messageList;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_message, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessage message = messageList.get(position);
        holder.bind(message);
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    public void addMessage(ChatMessage message) {
        messageList.add(message);
        notifyItemInserted(messageList.size() - 1);
    }

    public void updateLastBotMessage(String newText) {
        for (int i = messageList.size() - 1; i >= 0; i--) {
            if (messageList.get(i).isBotMessage()) {
                messageList.get(i).setMessage(newText);
                messageList.get(i).setTyping(false);
                notifyItemChanged(i);
                break;
            }
        }
    }

    public void clearMessages() {
        messageList.clear();
        notifyDataSetChanged();
    }

    class ChatViewHolder extends RecyclerView.ViewHolder {
        LinearLayout layoutBotMessage, layoutUserMessage;
        TextView tvBotMessageText, tvBotMessageTime;
        TextView tvUserMessageText, tvUserMessageTime;

        ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutBotMessage = itemView.findViewById(R.id.layoutBotMessage);
            layoutUserMessage = itemView.findViewById(R.id.layoutUserMessage);
            tvBotMessageText = itemView.findViewById(R.id.tvBotMessageText);
            tvBotMessageTime = itemView.findViewById(R.id.tvBotMessageTime);
            tvUserMessageText = itemView.findViewById(R.id.tvUserMessageText);
            tvUserMessageTime = itemView.findViewById(R.id.tvUserMessageTime);
        }

        void bind(ChatMessage message) {
            String timeStr = timeFormat.format(new Date(message.getTimestamp()));

            if (message.isUserMessage()) {
                // User message - Right side
                layoutUserMessage.setVisibility(View.VISIBLE);
                layoutBotMessage.setVisibility(View.GONE);
                
                tvUserMessageText.setText(message.getMessage());
                tvUserMessageTime.setText(timeStr);
            } else {
                // Bot message - Left side
                layoutBotMessage.setVisibility(View.VISIBLE);
                layoutUserMessage.setVisibility(View.GONE);
                
                if (message.isTyping()) {
                    tvBotMessageText.setText("Đang suy nghĩ...");
                } else {
                    tvBotMessageText.setText(message.getMessage());
                }
                tvBotMessageTime.setText(timeStr);
            }
        }
    }
}
