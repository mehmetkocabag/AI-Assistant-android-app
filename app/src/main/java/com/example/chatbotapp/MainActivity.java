package com.example.chatbotapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;

import com.example.chatbotapp.network.ApiClient;
import com.example.chatbotapp.network.OpenAiRequest;
import com.example.chatbotapp.network.OpenAiResponse;
import com.example.chatbotapp.network.OpenAiService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class MainActivity extends AppCompatActivity {
    private EditText textUserInput;
    private TextView textViewChat;
    private OpenAiService service;
    private static final String API_KEY = "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";

    FirebaseAuth auth;
    FirebaseUser user;
    String ChatId;
    String userName;
    String email;
    String model = "gpt-4o";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome);
        auth = FirebaseAuth.getInstance();
        TextView textViewLogout,textViewUserDetails;
        textViewLogout = findViewById(R.id.logout);
        textViewUserDetails = findViewById(R.id.user_details); //Updated in loadUserNameFromFirebase().Left here for future implementation.
        Button startChatButton = findViewById(R.id.startChattingButton);
        Button accountButton = findViewById(R.id.accountButton);

        user = auth.getCurrentUser();
        if (user == null){
            Intent intent = new Intent(getApplicationContext(), Login.class);
            startActivity(intent);
            finish();
        }
        else{
            loadUserNameFromFirebase();
            email = user.getEmail();
        }

        textViewLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(getApplicationContext(), Login.class);
            startActivity(intent);
            finish();
        });
        startChatButton.setOnClickListener(v -> chatHistoryGrid());
        accountButton.setOnClickListener(v -> account());
    }
    private void startChat(List<OpenAiRequest.Message> messages) {
        setContentView(R.layout.activity_main);
        ProgressBar progressBar = findViewById(R.id.progressBar);
        TextView aiTypingView = findViewById(R.id.aiTypingText);
        textUserInput = findViewById(R.id.textUserInput);
        textViewChat = findViewById(R.id.textViewChat);
        textViewChat.setMovementMethod(new ScrollingMovementMethod());

        displayRetrievedMessages(messages);

        Retrofit retrofit = ApiClient.getClient(API_KEY); // ApiClient, Retrofit instance
        service = retrofit.create(OpenAiService.class);

        ImageButton sendButton = findViewById(R.id.imageButton); //Api communication
        sendButton.setOnClickListener(v -> {
            aiTypingView.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.VISIBLE);
            String userMessage = textUserInput.getText().toString();
            if (!userMessage.isEmpty()) {

                messages.add(new OpenAiRequest.Message("user", userMessage));
                displayMessage("> "+ userName + "\n" + userMessage);
                textUserInput.setText("");
                OpenAiRequest request = new OpenAiRequest(model, messages);       // OpenAIRequest

                service.createCompletion(request).enqueue(new Callback<OpenAiResponse>() {
                    @Override
                    public void onResponse(Call<OpenAiResponse> call, Response<OpenAiResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            String aiContent = response.body().getChoices().get(0).getMessage().getContent();
                            aiContent = aiContent.trim();
                            displayMessage("> AI" + "\n" + aiContent);

                            messages.add(new OpenAiRequest.Message("system", aiContent));
                            progressBar.setVisibility(View.GONE);
                            aiTypingView.setVisibility(View.GONE);
                        }
                    }
                    @Override
                    public void onFailure(Call<OpenAiResponse> call, Throwable t) {
                        Log.e("API Failure", t.getMessage(), t);
                        displayMessage("Error: " + t.getMessage());
                        progressBar.setVisibility(View.GONE);
                        aiTypingView.setVisibility(View.GONE);
                    }
                });
            }
        });

        ImageButton backToStartButton = findViewById(R.id.backToStartButton);
        backToStartButton.setOnClickListener(v -> {
            saveChat(ChatId, messages);
            chatHistoryGrid();
        });

        ImageButton deleteChatButton = findViewById(R.id.deleteChatButton);
        deleteChatButton.setOnClickListener(v -> {
            deleteChat(ChatId);
        });
    }
    public void newChatToFirebase(List<OpenAiRequest.Message> messages) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference chatRef = FirebaseDatabase.getInstance().getReference("ChatHistories").child(userId);

        String chatId = chatRef.push().getKey();
        ChatId = chatId;

        if (chatId != null) {
            chatRef.child(chatId).setValue(messages).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(getApplicationContext(), "Chat created", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getApplicationContext(), "Failed to create chat", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
    public void saveChat(String chatId, List<OpenAiRequest.Message> messages) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference chatRef = FirebaseDatabase.getInstance().getReference("ChatHistories").child(userId).child(chatId);

        chatRef.setValue(messages).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(getApplicationContext(), "Chat Saved", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getApplicationContext(), "Failed to update chat", Toast.LENGTH_SHORT).show();
                    }
                });
    }
    public void chatHistoryGrid() {
        setContentView(R.layout.grid);

        List<OpenAiRequest.Message> messages = new ArrayList<>();
        List<ChatInfo> chatInfo = new ArrayList<>();

        GridView gridView = findViewById(R.id.gridViewChats);
        ChatAdapter chatAdapter = new ChatAdapter(this, chatInfo);
        gridView.setAdapter(chatAdapter);

        loadChatsInfoFromFirebase(chatAdapter);

        gridView.setOnItemClickListener((parent, view, position, id) -> {
            ChatInfo clickedChatInfo = (ChatInfo) chatAdapter.getItem(position);
            ChatId = clickedChatInfo.getChatId();
            loadChatMessagesFromFirebase(ChatId);
            //debug Toast.makeText(this, "Chat ID: " + ChatId, Toast.LENGTH_SHORT).show();
        });

        Button buttonNewChat = findViewById(R.id.buttonNewChat);
        buttonNewChat.setOnClickListener(v -> {
            newChatToFirebase(messages);
            startChat(messages);
        });

        ImageButton goBack = findViewById(R.id.goBack);
        goBack.setOnClickListener(v -> {
            Intent intent= new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }
    public void loadChatsInfoFromFirebase(ChatAdapter chatAdapter) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference chatRef = FirebaseDatabase.getInstance().getReference("ChatHistories").child(userId);

        chatRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<ChatInfo> chatInfo = new ArrayList<>();
                for (DataSnapshot chatSnapshot : snapshot.getChildren()) {
                    String chatId = chatSnapshot.getKey();
                    DataSnapshot firstMessageSnapshot = chatSnapshot.getChildren().iterator().next();
                    OpenAiRequest.Message firstMessage = firstMessageSnapshot.getValue(OpenAiRequest.Message.class);
                    if (firstMessage != null && firstMessage.getContent() != null) {
                        String content = firstMessage.getContent();
                        if (content.length() > 30) {
                            String topic = content.substring(0, 27) + "...";
                            firstMessage.setContent(topic);
                        }else {
                            String zeroWidthSpace = "\u200B";
                            String topic = String.format("%-30s", content) + zeroWidthSpace;
                            firstMessage.setContent(topic);
                        }
                        chatInfo.add(new ChatInfo(chatId, firstMessage));
                    }
                }
                chatAdapter.updateChatInfo(chatInfo);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getApplicationContext(), "Failed to load chats", Toast.LENGTH_SHORT).show();
                Intent intent= new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }
    public void loadChatMessagesFromFirebase(String chatId) {
        List<OpenAiRequest.Message> messages = new ArrayList<>();

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference chatRef = FirebaseDatabase.getInstance().getReference("ChatHistories").child(userId).child(chatId);

        chatRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot messageSnapshot : snapshot.getChildren()) {
                    OpenAiRequest.Message message = messageSnapshot.getValue(OpenAiRequest.Message.class);
                    if (message != null){
                    messages.add(message);}
                }
                startChat(messages);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getApplicationContext(), "Failed to load chat messages", Toast.LENGTH_SHORT).show();
            }
        });

    }
    public void deleteChat(String chatId) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference chatRef = FirebaseDatabase.getInstance().getReference("ChatHistories").child(userId).child(chatId);

        new AlertDialog.Builder(this).setTitle("Delete Chat").setMessage("Are you sure you want to delete this chat?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    chatRef.removeValue().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(getApplicationContext(), "Chat deleted", Toast.LENGTH_SHORT).show();
                            chatHistoryGrid();
                        } else {
                            Toast.makeText(getApplicationContext(), "Failed to delete chat", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void account() {
        setContentView(R.layout.account);

        TextView textViewUserMail = findViewById(R.id.user_email);
        textViewUserMail.setText(email);
        TextView textViewBackToMenu = findViewById(R.id.backToMenu);
        Button changeNameButton = findViewById(R.id.changeName);
        Button changePassButton = findViewById(R.id.changePass);

        textViewBackToMenu.setOnClickListener(v -> {
            Intent intent= new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        });
        changeNameButton.setOnClickListener(v -> changeNickName());
        changePassButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, ChangePassword.class);
            startActivity(intent);
        });

    }
    public void changeNickName() {
        setContentView(R.layout.change_name);

        TextView textCurrentName = findViewById(R.id.currentName);
        textCurrentName.setText(userName);

        TextView textBackToAcc = findViewById(R.id.goBackName);
        textBackToAcc.setOnClickListener(v -> account());

        EditText newName = findViewById(R.id.newName);
        Button changeNickNameButton = findViewById(R.id.changeNameButton);

        changeNickNameButton.setOnClickListener(v -> {
            String newNameS = newName.getText().toString();
            textCurrentName.setText(newNameS);
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(userId);
            Users user = new Users(newNameS,email);
            userRef.setValue(user).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(getApplicationContext(), "User updated.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Failed to update user.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
    private void loadUserNameFromFirebase() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(userId);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Users user = snapshot.getValue(Users.class);
                if (user != null) {
                    String userName = user.getName();
                    getUserName(userName);
                    TextView textView = findViewById(R.id.user_details);
                    textView.setText(userName);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getApplicationContext(), "Failed to load user.", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void getUserName(String name) {
        userName = name;
    }
    private void displayMessage(String message) {
        textViewChat.append(message + "\n\n");

        ScrollView scrollView = findViewById(R.id.scrollView);
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }
    private void displayRetrievedMessages(List<OpenAiRequest.Message> messages) {
        for (OpenAiRequest.Message message : messages) {
            String role = message.getRole();
            String content = message.getContent();
            String text;

            if ("user".equals(role)) {
                text = "> " + userName + "\n" + content;
            } else {
                text = "> AI" + "\n" + content;
            }
            displayMessage(text);
        }
    }
}