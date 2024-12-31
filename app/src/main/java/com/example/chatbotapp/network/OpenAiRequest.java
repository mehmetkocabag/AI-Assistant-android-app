package com.example.chatbotapp.network;

import java.util.List;

public class OpenAiRequest {
    private String model;
    private List<Message> messages;

    public OpenAiRequest(String model, List<Message> messages) {
        this.model = model;
        this.messages = messages;
    }

    public static class Message {
        private String role;
        private String content;

        public Message() {
        }
        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
        public String getRole() {
            return role;
        }

        public String getContent() {
            return content;
        }
        public void setContent(String content) {
            this.content = content;
        }
    }
}