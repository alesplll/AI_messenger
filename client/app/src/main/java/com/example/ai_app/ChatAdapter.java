package com.example.ai_app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import androidx.annotation.NonNull;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.MessageViewHolder> {

    private List<MessageEntity> messages;

    public ChatAdapter(List<MessageEntity> messages) {
        this.messages = messages;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        holder.bind(messages.get(position));
    }

    public class MessageViewHolder extends RecyclerView.ViewHolder {

        private TextView messageText;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.text_message);
        }

        public void bind(MessageEntity message) {
            messageText.setText(message.getContent());
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public void addMessage(String message, boolean isUserMessage, String chatName) {
        messages.add(new MessageEntity(message, isUserMessage, chatName));
        notifyItemInserted(messages.size() - 1);
    }


    public void setMessages(List<MessageEntity> messages) {
        this.messages = messages;
        notifyDataSetChanged();
    }
}
