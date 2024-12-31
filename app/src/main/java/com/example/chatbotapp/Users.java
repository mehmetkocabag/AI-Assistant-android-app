package com.example.chatbotapp;

import com.example.chatbotapp.network.OpenAiRequest;

import java.util.ArrayList;
import java.util.List;

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

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

}
