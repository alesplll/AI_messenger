/*
package com.example.ai_app;
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


import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import javax.net.ssl.X509TrustManager;


import java.io.OutputStream;
import java.net.HttpURLConnection;


import android.os.Bundle;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;



public class _old_MainActivity extends AppCompatActivity implements ChatContract.View {

    private RecyclerView recyclerView;
    private EditText messageEditText;
    private Button sendButton;
    private ChatAdapter chatAdapter;
    private ChatContract.Presenter presenter;

    private AppDatabase appDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
        String userMessage = messageEditText.getText().toString().trim();
        presenter.sendMessage(userMessage);
        messageEditText.getText().clear();

        // сообщение пользователя в базе данных
        saveMessageToDatabase(userMessage, true);
        // Оторазите сообщение пользователя в списке
        displayMessage(userMessage, true);

        // Асинхронно запускаем получение ответа от ИИ
        executor.execute(() -> {
            String aiResponse_not_corrected = getAIResponse(userMessage);
            String aiResponse = aiResponse_not_corrected.substring(1, aiResponse_not_corrected.length()-1);
            // сообщение ИИ в базе данных
            saveMessageToDatabase(aiResponse, false);
            // Отобразите ответ ИИ в списке
            displayMessage(aiResponse, false);
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
            String url = "https://3.137.41.104:4000/process_message";

            // Строковый параметр для отправки
            String requestBody = "{\"message\": \"" + userMessage + "\"}";

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
        } catch (Exception e) {
            e.printStackTrace();
            return " error_client " + e;
        }
    }


    private void saveMessageToDatabase(final String message, final boolean isUserMessage) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                appDatabase.messageDao().insertMessage(new MessageEntity(message, isUserMessage));
            }
        });
    }


    private void loadMessagesFromDatabase() {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                List<MessageEntity> messages = appDatabase.messageDao().getAllMessages();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        for (MessageEntity messageEntity : messages) {
                            boolean isUserMessage = messageEntity.isUserMessage();
                            String displayMessage = isUserMessage ? "You: " + messageEntity.getContent() : "AI: " + messageEntity.getContent();
                            chatAdapter.addMessage(displayMessage, isUserMessage);
                        }
                        recyclerView.scrollToPosition(chatAdapter.getItemCount() - 1);
                    }
                });
            }
        });
    }



    public void displayMessage(String message, boolean isUserMessage) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String displayMessage;
                if (isUserMessage) {
                    displayMessage = "You: " + message;
                } else {
                    displayMessage = "AI: " + message;
                }
                chatAdapter.addMessage(displayMessage, isUserMessage);
                recyclerView.scrollToPosition(chatAdapter.getItemCount() - 1);
            }
        });
    }


    //// Новая часть - менюшка и работа с ней!!!!!!
    ////
    ///
    ///
    ///
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle actionBarDrawerToggle;

    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (actionBarDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void openChat(String username) {
        // Открываем выбранный чат
    }

    private void createNewChat() {
        // Создаем новый чат
    }


}
*/