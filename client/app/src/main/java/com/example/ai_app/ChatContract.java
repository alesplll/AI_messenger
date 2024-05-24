package com.example.ai_app;

public interface ChatContract {

    interface View {
        void displayMessage(String message, boolean isUserMessage, String chatName);
    }

    interface Presenter {
        void sendMessage(String message);
    }
}
