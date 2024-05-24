package com.example.ai_app;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Objects;


@Entity(tableName = "messages")
public class MessageEntity {

    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "chat_id")
    private int chat_id;

    @ColumnInfo(name = "content")
    private String content;

    @ColumnInfo(name = "is_user_message")
    private boolean isUserMessage;


    public MessageEntity() {
    }

    public MessageEntity(String content, boolean isUserMessage, String chatName) {
        this.content = content;
        this.isUserMessage = isUserMessage;
        if(Objects.equals(chatName, "Основной")){this.chat_id = 0;}
        else if(Objects.equals(chatName, "Психолог")){this.chat_id = 1;}
        else if(Objects.equals(chatName, "Юрист")){this.chat_id = 2;}
        else if(Objects.equals(chatName, "Программист")){this.chat_id = 3;}
        else{this.chat_id = 0;}
    }

    public boolean isUserMessage() {
        return isUserMessage;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getChat_id(){
        return chat_id;
    }

    public void setChat_id(int name){
        this.chat_id = name;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setUserMessage(boolean userMessage) {
        isUserMessage = userMessage;
    }
}



