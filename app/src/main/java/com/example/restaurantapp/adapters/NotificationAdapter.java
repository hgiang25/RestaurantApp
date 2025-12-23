package com.example.restaurantapp.adapters;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.restaurantapp.R;
import com.example.restaurantapp.models.NotificationModel;

import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private List<NotificationModel> notificationList;
    private Set<String> readNotificationIds = new HashSet<>();
    private OnNotificationClickListener listener;

    public interface OnNotificationClickListener {
        void onNotificationClick(NotificationModel notification, int position);
    }

    public NotificationAdapter(List<NotificationModel> notificationList, OnNotificationClickListener listener) {
        this.notificationList = notificationList;
        this.listener = listener;
    }

    public void setReadNotificationIds(Set<String> readIds) {
        this.readNotificationIds = readIds;
        notifyDataSetChanged();
    }

    public void markAsRead(String notificationId) {
        readNotificationIds.add(notificationId);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        NotificationModel notification = notificationList.get(position);
        boolean isRead = readNotificationIds.contains(notification.getId());
        holder.bind(notification, isRead);
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView txtTitle, txtTime;
        View viewUnreadIndicator;

        NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTitle = itemView.findViewById(R.id.txtTitle);
            txtTime = itemView.findViewById(R.id.txtTime);
            viewUnreadIndicator = itemView.findViewById(R.id.viewUnreadIndicator);
        }

        void bind(NotificationModel notification, boolean isRead) {
            // Hiển thị title
            String title = notification.getTitle();
            if (title == null || title.isEmpty()) {
                // Nếu không có title, lấy 50 ký tự đầu của message làm title
                String msg = notification.getMessage();
                title = (msg != null && msg.length() > 50) ? msg.substring(0, 50) + "..." : msg;
            }
            txtTitle.setText(title);

            // Hiển thị thời gian
            if (notification.getCreatedAt() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault());
                txtTime.setText(sdf.format(notification.getCreatedAt().toDate()));
            }

            // Hiển thị indicator chưa đọc
            if (isRead) {
                viewUnreadIndicator.setVisibility(View.GONE);
                txtTitle.setTypeface(null, Typeface.NORMAL);
                txtTitle.setAlpha(0.7f);
            } else {
                viewUnreadIndicator.setVisibility(View.VISIBLE);
                txtTitle.setTypeface(null, Typeface.BOLD);
                txtTitle.setAlpha(1.0f);
            }

            // Click để xem chi tiết
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onNotificationClick(notification, getAdapterPosition());
                }
            });
        }
    }
}
