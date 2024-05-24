package com.example.ai_app;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface MessageDao {

    @Insert
    void insertMessage(MessageEntity message);

    @Query("SELECT * FROM messages WHERE chat_id = :chatId")
    List<MessageEntity> getAllMessagesForChat(int chatId);
}
