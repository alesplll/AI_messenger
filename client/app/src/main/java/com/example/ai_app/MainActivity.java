package com.example.ai_app;

import android.content.Intent;
import android.os.AsyncTask;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import android.widget.EditText;
import android.widget.Button;
import java.util.ArrayList;
import android.view.View;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import javax.net.ssl.X509TrustManager;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import android.widget.ImageView;
import android.widget.TextView;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;


public class MainActivity extends AppCompatActivity implements ChatContract.View {

    private String name_character = "AI";
    private RecyclerView recyclerView;
    private EditText messageEditText;
    private Button sendButton;
    private ChatAdapter chatAdapter;
    private ChatContract.Presenter presenter;
    private TextView chatTitleTextView;
    private String chatName;
    private int chatId;
    private String chatPrompt;
    private ImageView backArrow;


    private AppDatabase appDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        chatTitleTextView = findViewById(R.id.chatTitleTextView);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("chatName")) {
            chatName = intent.getStringExtra("chatName");
            chatPrompt = getChatPrompt(chatName);
            chatTitleTextView.setText(chatName);
        }else {
            chatName = "Default";
            chatPrompt = getChatPrompt(chatName);
            chatTitleTextView.setText(chatName);
        }

        backArrow = findViewById(R.id.back_arrow);
        // Обработка нажатия на стрелку "Назад"
        backArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Возвращение к предыдущей активности
            }
        });


        recyclerView = findViewById(R.id.recycler_view_chat);
        messageEditText = findViewById(R.id.edit_text_message);
        sendButton = findViewById(R.id.button_send);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        chatAdapter = new ChatAdapter(new ArrayList<>());
        recyclerView.setAdapter(chatAdapter);

        presenter = new ChatPresenter(this);

        appDatabase = AppDatabase.getInstance(this);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage();
            }
        });

        // Загрузите сообщения из базы данных при запуске
        loadMessagesFromDatabase();
    }


    // not in main thread
    private Executor executor = Executors.newSingleThreadExecutor();

    private void sendMessage() {
        String userMessage =  messageEditText.getText().toString().trim();
        presenter.sendMessage(userMessage);
        messageEditText.getText().clear();

        saveMessageToDatabase(userMessage, true, chatName);   // сообщение пользователя в базу данных
        displayMessage(userMessage, true, chatName);          // Отображение сообщение пользователя в списке

        // Асинхронно запускаем получение ответа от ИИ
        executor.execute(() -> {
            String aiResponse_not_corrected = getAIResponse(userMessage);
            String aiResponse = aiResponse_not_corrected.substring(1, aiResponse_not_corrected.length()-1);
            saveMessageToDatabase(aiResponse, false, chatName);     // сообщение ИИ в базу данныx
            displayMessage(aiResponse, false, chatName);            // Отображение ответ ИИ в списке
        });
    }

    private String getAIResponse(String userMessage) {

        try {
            /// здеся обходим сертификат - не в прод
            int certResourceId = R.raw.cert;

            InputStream certInputStream = getResources().openRawResource(certResourceId);

            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            X509Certificate certificate = (X509Certificate) certificateFactory.generateCertificate(certInputStream);

            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            keyStore.setCertificateEntry("alias", certificate);

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagerFactory.getTrustManagers(), new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());


            TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
                public void checkClientTrusted(
                        java.security.cert.X509Certificate[] certs, String authType) {
                }
                public void checkServerTrusted(
                        java.security.cert.X509Certificate[] certs, String authType) {
                }
            }};

            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            HostnameVerifier allHostsValid = new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };

            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

            /// конец всяких обходов




            // URL сервера
            //String url = "https://3.137.41.104:4000/process_message";
            String url = "https://ec2-3-16-180-70.us-east-2.compute.amazonaws.com:443/process_message";

            // Строковый параметр для отправки
            chatPrompt = getChatPrompt(chatName);
            String requestBody = "{\"prompt\": \"" + chatPrompt + "\", \"message\": \"" + userMessage + "\"}";
            //String requestBody = "{\"message\": \"" + userMessage + "\"}";

            // Создаем объект URL
            URL apiUrl = new URL(url);

            // Открываем соединение
            HttpURLConnection connection = (HttpURLConnection) apiUrl.openConnection();

            // Устанавливаем метод запроса и свойства
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            // Получаем поток для записи тела запроса
            try (OutputStream os = connection.getOutputStream()) {
                os.write(requestBody.getBytes());
                os.flush();
            }
            catch (Exception e) {
                e.printStackTrace();
                return " error_client " + e;
            }

            // Получаем код ответа от сервера
            int responseCode = connection.getResponseCode();

            // Чтение ответа от сервера
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                return response.toString();
            }
            catch (Exception e) {
                e.printStackTrace();
                return " error_client " + e;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return " error_client " + e;
        }


        //return "I am here, I am " + chatName + " but you do not connected";
    }


    private void saveMessageToDatabase(final String message, final boolean isUserMessage, String chatName) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                if(isUserMessage == true) {
                    appDatabase.messageDao().insertMessage(new MessageEntity(message, isUserMessage, chatName));
                }else{
                    appDatabase.messageDao().insertMessage(new MessageEntity(message.substring(12), isUserMessage, chatName));
                }
            }
        });
    }


    private void loadMessagesFromDatabase() {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                if(Objects.equals(chatName, "Основной")){chatId = 0;}
                else if(Objects.equals(chatName, "Психолог")){chatId = 1;}
                else if(Objects.equals(chatName, "Юрист")){chatId = 2;}
                else if(Objects.equals(chatName, "Программист")){chatId = 3;}
                else {chatId = 0;}
                List<MessageEntity> messages = appDatabase.messageDao().getAllMessagesForChat(chatId);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        for (MessageEntity messageEntity : messages) {
                            boolean isUserMessage = messageEntity.isUserMessage();
                            String displayMessage = isUserMessage ? "You: " + messageEntity.getContent() : "AI: " + messageEntity.getContent();
                            chatAdapter.addMessage(displayMessage, isUserMessage, chatName);
                        }
                        recyclerView.scrollToPosition(chatAdapter.getItemCount() - 1);
                    }
                });
            }
        });
    }



    public void displayMessage(String message, boolean isUserMessage, String chatName_i) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (Objects.equals(chatName_i, chatName)){
                    String displayMessage;
                    if (isUserMessage) {
                        displayMessage = "You: " + message;
                    } else {
                        displayMessage = name_character + ": " + message.substring(12);
                    }
                    chatAdapter.addMessage(displayMessage, isUserMessage, chatName);
                    recyclerView.scrollToPosition(chatAdapter.getItemCount() - 1);
                }
            }
        });
    }

    private String getChatPrompt(String chatName) {
        switch (chatName) {
            case "Основной":
                return "Основной помощник: Ты — надежный гид в мире знаний. Ты обладаешь широким кругозором и всегда готов помочь в любой ситуации. У тебя дружелюбный и уверенный в себе характер, и ты готов общаться с собеседником на любом языке, чтобы обеспечить максимально комфортное взаимодействие.";
            case "Психолог":
                return "Психолог: Ты — эмоциональный компаньон. Ты внимателен и заботлив, всегда готов выслушать и поддержать в трудные моменты. У тебя спокойный и дружелюбный характер, и ты обладаешь умением находить общий язык с каждым, кто обращается к тебе.";
            case "Юрист":
                return "Юрист: Ты — надежный защитник в мире права. Ты обладаешь аналитическим мышлением и глубокими знаниями в области юриспруденции. У тебя строгий и ответственный характер, и ты всегда готов помочь разобраться в законодательстве и правовых вопросах.";
            case "Программист":
                return "Программист: Ты — технологический гуру. Ты обладаешь острым умом и широкими знаниями в области программирования и компьютерных технологий. У тебя логичный и аналитический характер, и ты всегда готов помочь с написанием кодов и решением технических проблем.";
            default:
                return "Основной помощник: Ты — надежный гид в мире знаний. Ты обладаешь широким кругозором и всегда готов помочь в любой ситуации. У тебя дружелюбный и уверенный в себе характер, и ты готов общаться с собеседником на любом языке, чтобы обеспечить максимально комфортное взаимодействие.";
        }
    }



}
