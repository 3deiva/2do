package com.example.a2do;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class OTPVerificationActivity extends AppCompatActivity {
    private EditText otpEditText;
    private Button verifyButton;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp_verification);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        otpEditText = findViewById(R.id.otp_edit_text);
        verifyButton = findViewById(R.id.verify_button);

        String email = getIntent().getStringExtra("email");
        String password = getIntent().getStringExtra("password");
        String name = getIntent().getStringExtra("name");
        String correctOTP = getIntent().getStringExtra("otp");

        verifyButton.setOnClickListener(v -> {
            String enteredOTP = otpEditText.getText().toString().trim();

            if (enteredOTP.equals(correctOTP)) {
                createUserAccount(email, password, name);
            } else {
                Toast.makeText(this, "Invalid OTP", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createUserAccount(String email, String password, String name) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Store additional user details in Firestore
                        Map<String, Object> user = new HashMap<>();
                        user.put("name", name);
                        user.put("email", email);

                        db.collection("users")
                                .document(mAuth.getCurrentUser().getUid())
                                .set(user)
                                .addOnSuccessListener(documentReference -> {
                                    Toast.makeText(this, "Registration Successful", Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(this, MainActivity.class));
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Failed to store user details", Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        Toast.makeText(this, "Authentication failed: "
                                + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
