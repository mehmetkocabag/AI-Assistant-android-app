package com.example.chatbotapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChangePassword extends AppCompatActivity {

    private EditText currentPassword, newPassword, confirmNewPassword;
    private Button changePasswordButton;
    private FirebaseAuth mAuth;
    TextView textBackToAcc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.change_password);

        currentPassword = findViewById(R.id.currentPassword);
        newPassword = findViewById(R.id.newPassword);
        confirmNewPassword = findViewById(R.id.confirmNewPassword);
        changePasswordButton = findViewById(R.id.changePasswordButton);
        textBackToAcc = findViewById(R.id.goBack);
        textBackToAcc.setOnClickListener(v -> {
            finish();
        });
        mAuth = FirebaseAuth.getInstance();

        changePasswordButton.setOnClickListener(v -> {
            String currentPass = currentPassword.getText().toString();
            String newPass = newPassword.getText().toString();
            String confirmPass = confirmNewPassword.getText().toString();

            if (TextUtils.isEmpty(currentPass) || TextUtils.isEmpty(newPass) || TextUtils.isEmpty(confirmPass)) {
                Toast.makeText(ChangePassword.this, "All fields must be filled.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPass.equals(confirmPass)) {
                Toast.makeText(ChangePassword.this, "Passwords do not match.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (newPass.length() < 6) {
                Toast.makeText(ChangePassword.this, "Password must be at least 6 characters long.", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) {
                reauthenticateAndChangePassword(user, currentPass, newPass);
            } else {
                Toast.makeText(ChangePassword.this, "User not authenticated", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void reauthenticateAndChangePassword(FirebaseUser user, String currentPass, String newPass) {

        String email = user.getEmail();

        if (email == null) {
            Toast.makeText(this, "Failed to retrieve user email", Toast.LENGTH_SHORT).show();
            return;
        }

        AuthCredential credential = EmailAuthProvider.getCredential(email, currentPass);
        user.reauthenticate(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                user.updatePassword(newPass).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(ChangePassword.this, "Password updated.", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(ChangePassword.this, "Password update failed.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            } else {
                Toast.makeText(ChangePassword.this, "Re-authentication failed", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
