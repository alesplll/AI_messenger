package com.example.ai_app;

public class ChatPresenter implements ChatContract.Presenter {

    private ChatContract.View view;

    public ChatPresenter(ChatContract.View view) {
        this.view = view;
    }

    @Override
    public void sendMessage(String message) {
        // Здесь вы можете добавить логику для отправки сообщения на сервер и получения ответа от ИИ
        // После получения ответа от ИИ, вызовите метод view.displayMessage(responseFromAI, false);
    }
}
