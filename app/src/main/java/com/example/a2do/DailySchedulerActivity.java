package com.example.a2do;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

public class DailySchedulerActivity extends AppCompatActivity {
    private EditText totalWorkHoursInput, urgentTasksInput, importantTasksInput;
    private Button calculateScheduleButton, saveScheduleButton;
    private TextView scheduleResultText;
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily_scheduler);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Initialize Views
        initializeViews();

        // Set up Listeners
        setupListeners();
    }

    private void initializeViews() {
        totalWorkHoursInput = findViewById(R.id.total_work_hours_input);
        urgentTasksInput = findViewById(R.id.urgent_tasks_input);
        importantTasksInput = findViewById(R.id.important_tasks_input);
        calculateScheduleButton = findViewById(R.id.calculate_schedule_button);
        saveScheduleButton = findViewById(R.id.save_schedule_button);
        scheduleResultText = findViewById(R.id.schedule_result_text);
    }

    private void setupListeners() {
        calculateScheduleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calculateDailySchedule();
            }
        });

        saveScheduleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveScheduleToFirestore();
            }
        });
    }

    private boolean validateInputs() {
        String totalHoursStr = totalWorkHoursInput.getText().toString();
        String urgentTasksStr = urgentTasksInput.getText().toString();
        String importantTasksStr = importantTasksInput.getText().toString();

        if (totalHoursStr.isEmpty() || urgentTasksStr.isEmpty() || importantTasksStr.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return false;
        }

        try {
            double totalHours = Double.parseDouble(totalHoursStr);
            int urgentTasks = Integer.parseInt(urgentTasksStr);
            int importantTasks = Integer.parseInt(importantTasksStr);

            if (totalHours <= 0 || urgentTasks < 0 || importantTasks < 0) {
                Toast.makeText(this, "Please enter valid positive numbers", Toast.LENGTH_SHORT).show();
                return false;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid number format", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void calculateDailySchedule() {
        if (!validateInputs()) return;

        // Parse inputs
        double totalWorkHours = Double.parseDouble(totalWorkHoursInput.getText().toString());
        int urgentTasks = Integer.parseInt(urgentTasksInput.getText().toString());
        int importantTasks = Integer.parseInt(importantTasksInput.getText().toString());

        // Calculate schedule
        ScheduleAnalysis scheduleAnalysis = optimizeSchedule(totalWorkHours, urgentTasks, importantTasks);

        // Display results
        displayScheduleResults(scheduleAnalysis);
    }

    private ScheduleAnalysis optimizeSchedule(double totalWorkHours, int urgentTasks, int importantTasks) {
        // Basic time allocation strategy
        double urgentTaskTime = 0.4 * totalWorkHours; // 40% of time for urgent tasks
        double importantTaskTime = 0.3 * totalWorkHours; // 30% for important tasks
        double breakTime = 0.2 * totalWorkHours; // 20% buffer/break time
        double flexTime = 0.1 * totalWorkHours; // 10% flexible time

        // Allocate time per task
        double timePerUrgentTask = urgentTasks > 0 ? urgentTaskTime / urgentTasks : 0;
        double timePerImportantTask = importantTasks > 0 ? importantTaskTime / importantTasks : 0;

        return new ScheduleAnalysis(
                totalWorkHours,
                urgentTasks,
                importantTasks,
                urgentTaskTime,
                importantTaskTime,
                breakTime,
                flexTime,
                timePerUrgentTask,
                timePerImportantTask
        );
    }

    private void displayScheduleResults(ScheduleAnalysis analysis) {
        DecimalFormat df = new DecimalFormat("#.##");

        StringBuilder resultBuilder = new StringBuilder();
        resultBuilder.append("Daily Schedule Optimization:\n\n")
                .append("Total Work Hours: ").append(df.format(analysis.totalWorkHours)).append(" hrs\n")
                .append("Urgent Tasks: ").append(analysis.urgentTasks)
                .append(" (").append(df.format(analysis.urgentTaskTime)).append(" hrs)\n")
                .append("Important Tasks: ").append(analysis.importantTasks)
                .append(" (").append(df.format(analysis.importantTaskTime)).append(" hrs)\n")
                .append("Break Time: ").append(df.format(analysis.breakTime)).append(" hrs\n")
                .append("Flexible Time: ").append(df.format(analysis.flexTime)).append(" hrs\n\n")
                .append("Recommended Time Allocation:\n")
                .append("• Per Urgent Task: ").append(df.format(analysis.timePerUrgentTask)).append(" hrs\n")
                .append("• Per Important Task: ").append(df.format(analysis.timePerImportantTask)).append(" hrs");

        scheduleResultText.setText(resultBuilder.toString());
    }

    private void saveScheduleToFirestore() {
        // Ensure a schedule has been calculated
        if (scheduleResultText.getText().toString().isEmpty()) {
            Toast.makeText(this, "Please calculate a schedule first", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check user is logged in
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please log in to save your schedule", Toast.LENGTH_SHORT).show();
            return;
        }

        // Prepare schedule data
        Map<String, Object> scheduleData = new HashMap<>();
        scheduleData.put("userId", mAuth.getCurrentUser().getUid());
        scheduleData.put("totalWorkHours", totalWorkHoursInput.getText().toString());
        scheduleData.put("urgentTasks", urgentTasksInput.getText().toString());
        scheduleData.put("importantTasks", importantTasksInput.getText().toString());
        scheduleData.put("scheduleDetails", scheduleResultText.getText().toString());
        scheduleData.put("timestamp", System.currentTimeMillis());

        // Save to Firestore
        firestore.collection("daily_schedules")
                .add(scheduleData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Schedule saved successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to save schedule: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Inner class to hold schedule analysis data
    private static class ScheduleAnalysis {
        double totalWorkHours;
        int urgentTasks;
        int importantTasks;
        double urgentTaskTime;
        double importantTaskTime;
        double breakTime;
        double flexTime;
        double timePerUrgentTask;
        double timePerImportantTask;

        public ScheduleAnalysis(double totalWorkHours, int urgentTasks, int importantTasks,
                                double urgentTaskTime, double importantTaskTime,
                                double breakTime, double flexTime,
                                double timePerUrgentTask, double timePerImportantTask) {
            this.totalWorkHours = totalWorkHours;
            this.urgentTasks = urgentTasks;
            this.importantTasks = importantTasks;
            this.urgentTaskTime = urgentTaskTime;
            this.importantTaskTime = importantTaskTime;
            this.breakTime = breakTime;
            this.flexTime = flexTime;
            this.timePerUrgentTask = timePerUrgentTask;
            this.timePerImportantTask = timePerImportantTask;
        }
    }
}
