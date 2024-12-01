package com.example.a2do;
import android.view.ViewGroup;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TaskListActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private TaskAdapter taskAdapter;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_list);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.task_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        taskAdapter = new TaskAdapter(this::showTaskOptions);
        recyclerView.setAdapter(taskAdapter);

        // Load tasks
        loadTasks();

        // Start timer to update task times
        startTimeUpdateTimer();
    }

    private void showTaskOptions(Task task) {
        String[] options = task.isCompleted ?
                new String[]{"Update", "Mark as Incomplete", "Delete"} :
                new String[]{"Update", "Mark as Completed", "Delete"};

        new AlertDialog.Builder(this)
                .setTitle("Task Options")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            showUpdateDialog(task);
                            break;
                        case 1:
                            toggleTaskCompletion(task);
                            break;
                        case 2:
                            deleteTask(task);
                            break;
                    }
                })
                .show();
    }

    private void showUpdateDialog(Task task) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_update_task, null);

        EditText taskNameEdit = dialogView.findViewById(R.id.taskNameEdit);
        TextView dateText = dialogView.findViewById(R.id.dateText);
        TextView timeText = dialogView.findViewById(R.id.timeText);

        // Populate current task details
        taskNameEdit.setText(task.name);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        dateText.setText(dateFormat.format(task.timestamp));
        timeText.setText(timeFormat.format(task.timestamp));

        // Date picker dialog
        dateText.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            cal.setTime(task.timestamp);
            new DatePickerDialog(TaskListActivity.this, (view, year, month, day) -> {
                String date = String.format(Locale.getDefault(), "%d-%02d-%02d", year, month + 1, day);
                dateText.setText(date);
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        // Time picker dialog
        timeText.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            cal.setTime(task.timestamp);
            new TimePickerDialog(TaskListActivity.this, (view, hour, minute) -> {
                String time = String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
                timeText.setText(time);
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show();
        });

        builder.setView(dialogView)
                .setTitle("Update Task")
                .setPositiveButton("Update", (dialog, which) -> {
                    String newName = taskNameEdit.getText().toString();
                    String newDate = dateText.getText().toString();
                    String newTime = timeText.getText().toString();

                    try {
                        Date updatedTimestamp = dateTimeFormat.parse(newDate + " " + newTime);
                        updateTask(task, newName, updatedTimestamp);
                    } catch (Exception e) {
                        Toast.makeText(this, "Invalid date/time format", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateTask(Task task, String newName, Date newTimestamp) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please log in", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", newName);
        updates.put("timestamp", newTimestamp);

        db.collection("tasks").document(task.id)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Task updated successfully", Toast.LENGTH_SHORT).show();
                    loadTasks();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to update task", Toast.LENGTH_SHORT).show());
    }

    private void toggleTaskCompletion(Task task) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please log in", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("completed", !task.isCompleted);

        db.collection("tasks").document(task.id)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    String message = task.isCompleted ?
                            "Task marked as incomplete" :
                            "Task marked as completed";
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                    loadTasks();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to update task", Toast.LENGTH_SHORT).show());
    }

    private void deleteTask(Task task) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Task")
                .setMessage("Are you sure you want to delete this task?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    db.collection("tasks").document(task.id)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Task deleted", Toast.LENGTH_SHORT).show();
                                loadTasks();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Failed to delete task", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadTasks() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please log in", Toast.LENGTH_SHORT).show();
            return;
        }

        Query query = db.collection("tasks")
                .whereEqualTo("userId", currentUser.getUid())
                .orderBy("timestamp", Query.Direction.ASCENDING);

        query.addSnapshotListener((value, error) -> {
            if (error != null) {
                Toast.makeText(TaskListActivity.this,
                        "Error loading tasks: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
                return;
            }

            if (value != null) {
                List<Task> tasks = new ArrayList<>();
                for (QueryDocumentSnapshot document : value) {
                    Task task = new Task(
                            document.getId(),
                            document.getString("name"),
                            document.getDate("timestamp"),
                            document.getDouble("latitude"),
                            document.getDouble("longitude"),
                            Boolean.TRUE.equals(document.getBoolean("completed"))
                    );
                    tasks.add(task);
                }
                taskAdapter.setTasks(tasks);

                if (tasks.isEmpty()) {
                    Toast.makeText(TaskListActivity.this,
                            "No tasks found", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void startTimeUpdateTimer() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                loadTasks();  // Reload tasks to update remaining time
                handler.postDelayed(this, 60000);  // Update every minute
            }
        }, 60000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }

    // Task data class
    private static class Task {
        String id;
        String name;
        Date timestamp;
        double latitude;
        double longitude;
        boolean isCompleted;

        Task(String id, String name, Date timestamp, double latitude, double longitude, boolean isCompleted) {
            this.id = id;
            this.name = name;
            this.timestamp = timestamp;
            this.latitude = latitude;
            this.longitude = longitude;
            this.isCompleted = isCompleted;
        }
    }

    // RecyclerView Adapter
    private class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {
        private List<Task> tasks = new ArrayList<>();
        private final OnTaskClickListener clickListener;

        interface OnTaskClickListener {
            void onTaskClick(Task task);
        }

        TaskAdapter(OnTaskClickListener clickListener) {
            this.clickListener = clickListener;
        }

        @NonNull
        @Override
        public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.task_item, parent, false);
            return new TaskViewHolder(view, clickListener);
        }

        @Override
        public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
            Task task = tasks.get(position);
            holder.bind(task);
        }

        @Override
        public int getItemCount() {
            return tasks.size();
        }

        void setTasks(List<Task> tasks) {
            this.tasks = tasks;
            notifyDataSetChanged();
        }

        class TaskViewHolder extends RecyclerView.ViewHolder {
            private final TextView nameTextView;
            private final TextView dateTimeTextView;
            private final TextView locationTextView;
            private final TextView timeRemainingTextView;

            TaskViewHolder(@NonNull View itemView, OnTaskClickListener listener) {
                super(itemView);
                nameTextView = itemView.findViewById(R.id.task_name);
                dateTimeTextView = itemView.findViewById(R.id.task_date_time);
                locationTextView = itemView.findViewById(R.id.task_location);
                timeRemainingTextView = itemView.findViewById(R.id.time_remaining);

                // Set click listener for the entire item
                itemView.setOnClickListener(v -> {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION && listener != null) {
                        listener.onTaskClick((Task) itemView.getTag());
                    }
                });
            }

            void bind(Task task) {
                itemView.setTag(task);

                // Apply completed task styling
                if (task.isCompleted) {
                    nameTextView.setPaintFlags(nameTextView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    itemView.setAlpha(0.7f);
                    timeRemainingTextView.setText("Completed");
                    timeRemainingTextView.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                } else {
                    nameTextView.setPaintFlags(nameTextView.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                    itemView.setAlpha(1.0f);

                    // Calculate remaining time
                    long remaining = task.timestamp.getTime() - System.currentTimeMillis();
                    if (remaining > 0) {
                        long hours = remaining / (60 * 60 * 1000);
                        long minutes = (remaining % (60 * 60 * 1000)) / (60 * 1000);
                        timeRemainingTextView.setText(String.format(Locale.getDefault(),
                                "%dh %dm remaining", hours, minutes));
                        timeRemainingTextView.setTextColor(getResources().getColor(android.R.color.darker_gray));
                    } else {
                        timeRemainingTextView.setText("Overdue!");
                        timeRemainingTextView.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                    }
                }

                nameTextView.setText(task.name);

                SimpleDateFormat dateFormat = new SimpleDateFormat(
                        "dd MMM yyyy HH:mm", Locale.getDefault());
                dateTimeTextView.setText(dateFormat.format(task.timestamp));

                locationTextView.setText(String.format(Locale.getDefault(),
                        "Location: %.4f, %.4f",
                        task.latitude, task.longitude));
            }
        }
    }
}