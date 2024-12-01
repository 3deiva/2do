package com.example.a2do;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {
    private TextView welcomeTextView;
    private Button logoutButton, addTaskButton, viewTasksButton, dailySchedulerButton;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize UI components
        initializeViews();

        // Check if user is logged in and update welcome message
        updateWelcomeMessage();

        // Set up button click listeners
        setupClickListeners();
    }

    private void initializeViews() {
        welcomeTextView = findViewById(R.id.welcome_text);
        logoutButton = findViewById(R.id.logout_button);
        addTaskButton = findViewById(R.id.add_task_button);
        viewTasksButton = findViewById(R.id.view_tasks_button);
        dailySchedulerButton = findViewById(R.id.daily_scheduler_button);
    }

    private void updateWelcomeMessage() {
        if (mAuth.getCurrentUser() != null) {
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(mAuth.getCurrentUser().getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        String name = documentSnapshot.getString("name");
                        welcomeTextView.setText("Hello, " + (name != null ? name : "User") + "!");
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(MainActivity.this,
                                "Error loading user data: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void setupClickListeners() {
        // Logout button
        logoutButton.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        });

        // Add task button
        addTaskButton.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, AddTaskActivity.class));
        });

        // View tasks button
        viewTasksButton.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, TaskListActivity.class));
        });

        // Daily Scheduler button
        dailySchedulerButton.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, DailySchedulerActivity.class));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check if user is still logged in
        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        }
    }
}