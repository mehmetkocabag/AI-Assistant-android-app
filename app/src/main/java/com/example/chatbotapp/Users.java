package com.example.chatbotapp;

public class Users {
    public String name;
    public String email;

    public Users() {
    }

    public Users(String name, String email) {
        this.name = name;
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

}
