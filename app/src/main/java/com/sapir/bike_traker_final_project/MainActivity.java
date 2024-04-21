package com.sapir.bike_traker_final_project;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private Fragment_Map fragment_map;
    private Button startStopButton;
    private TextView mainTimer;
    private boolean isStarted = false;
    private Timer mct5;
    private Handler handler;
    private long startTimeMillis = 0;
    private Runnable updateTimerRunnable;
    private Boolean timerOn ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        timerOn=false;
        findViews();
        initializeMCT5();
        setupButtons();
    }

    private void findViews() {
        fragment_map = new Fragment_Map();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.main_map, fragment_map)
                .commit();
        startStopButton = findViewById(R.id.start_stop_button);
        mainTimer = findViewById(R.id.main_timer);
    }

    private void initializeMCT5() {
        mct5 = Timer.initHelper();
    }

    private void setupButtons() {
        startStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isStarted) {
                    // Start button clicked
                    startStopButton.setText("Stop");
                    startTimer();
                } else {
                    // Stop button clicked
                    startStopButton.setText("Start");
                    stopTimer();
                }
                isStarted = !isStarted; // Toggle the state
            }
        });
    }

    private void startTimer() {
        timerOn=true;
        startTimeMillis = System.currentTimeMillis();

        // Create a new handler on the main thread
        handler = new Handler(Looper.getMainLooper());

        // Create a runnable to update the timer every second
        updateTimerRunnable = new Runnable() {
            @Override
            public void run() {
                long elapsedTimeMillis = System.currentTimeMillis() - startTimeMillis;

                int minutes = (int) ((elapsedTimeMillis / (1000 * 60)) % 60);
                int seconds = (int) ((elapsedTimeMillis / 1000) % 60);

                String formattedTime = String.format("%02d:%02d", minutes, seconds);
                mainTimer.setText(formattedTime);

                // Post the next update after 1 second
                handler.postDelayed(this, 1000);
            }
        };

        // Start the handler to update the timer
        handler.postDelayed(updateTimerRunnable, 1000);
    }

    private void stopTimer() {
        timerOn=false;        // Remove the update timer runnable from the handler
        if (handler != null && updateTimerRunnable != null) {
            handler.removeCallbacks(updateTimerRunnable);

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (handler != null && updateTimerRunnable != null) {
            handler.removeCallbacks(updateTimerRunnable);
        }
        mct5.removeAll();
    }

    public boolean isTimerOn( ) {
        return this.timerOn;
    }
}