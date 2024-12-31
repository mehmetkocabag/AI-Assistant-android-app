package com.example.chatbotapp;

import com.example.chatbotapp.network.OpenAiRequest;

public class ChatInfo {
    private String chatId;
    private OpenAiRequest.Message firstMessage;

    public ChatInfo(String chatId, OpenAiRequest.Message firstMessage) {
        this.chatId = chatId;
        this.firstMessage = firstMessage;
    }

    public String getChatId() {
        return chatId;
    }

    public OpenAiRequest.Message getFirstMessage() {
        return firstMessage;
    }
}
