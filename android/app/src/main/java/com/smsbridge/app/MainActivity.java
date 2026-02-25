package com.smsbridge.app;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    // ── CONFIG ─────────────────────────────────────────────────────────────────
    private static final String SERVER_URL = "localhost:5000";  // ← Your server IP
    private static final String SECRET_TOKEN = "my-secret-token-123";       // ← Must match server.py
    private static final int POLL_INTERVAL_MS = 5000; // poll every 5 seconds
    // ──────────────────────────────────────────────────────────────────────────

    private static final String TAG = "SMSBridge";
    private static final int SMS_PERMISSION_CODE = 101;

    private Handler handler = new Handler();
    private TextView statusView, logView;
    private int sent = 0, failed = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusView = findViewById(R.id.statusText);
        logView    = findViewById(R.id.logText);

        requestSmsPermission();
    }

    private void requestSmsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION_CODE);
        } else {
            startPolling();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        if (requestCode == SMS_PERMISSION_CODE && results.length > 0
                && results[0] == PackageManager.PERMISSION_GRANTED) {
            startPolling();
        } else {
            statusView.setText("❌ SMS permission denied — cannot send messages");
        }
    }

    private void startPolling() {
        log("✅ SMS Bridge active. Polling server every " + POLL_INTERVAL_MS / 1000 + "s…");
        statusView.setText("🟢 Running — connected to " + SERVER_URL);
        handler.post(pollRunnable);
    }

    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            new Thread(() -> {
                try {
                    // 1. Poll server for pending messages
                    URL url = new URL(SERVER_URL + "/poll");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestProperty("X-Token", SECRET_TOKEN);
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);

                    int code = conn.getResponseCode();
                    if (code != 200) {
                        runOnUiThread(() -> statusView.setText("⚠️ Server returned " + code));
                        return;
                    }

                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                    br.close();

                    JSONObject json = new JSONObject(sb.toString());
                    JSONArray messages = json.getJSONArray("messages");

                    // 2. Send each message via SMS
                    for (int i = 0; i < messages.length(); i++) {
                        JSONObject msg = messages.getJSONObject(i);
                        String id   = msg.getString("id");
                        String to   = msg.getString("to");
                        String body = msg.getString("body");

                        boolean success = sendSMS(to, body);

                        if (success) {
                            confirm(id);
                            sent++;
                            runOnUiThread(() -> {
                                log("✉️ Sent → " + to + ": " + body);
                                statusView.setText("🟢 Running | Sent: " + sent + " | Failed: " + failed);
                            });
                        } else {
                            failed++;
                            runOnUiThread(() -> {
                                log("❌ Failed → " + to);
                                statusView.setText("🟢 Running | Sent: " + sent + " | Failed: " + failed);
                            });
                        }
                    }

                    if (messages.length() == 0) {
                        runOnUiThread(() -> statusView.setText("🟢 Running | Sent: " + sent + " | Waiting…"));
                    }

                } catch (Exception e) {
                    Log.e(TAG, "Poll error", e);
                    runOnUiThread(() -> statusView.setText("🔴 Server unreachable: " + e.getMessage()));
                }
            }).start();

            // Schedule next poll
            handler.postDelayed(pollRunnable, POLL_INTERVAL_MS);
        }
    };

    private boolean sendSMS(String to, String body) {
        try {
            SmsManager sm = SmsManager.getDefault();
            if (body.length() > 160) {
                sm.sendMultipartTextMessage(to, null, sm.divideMessage(body), null, null);
            } else {
                sm.sendTextMessage(to, null, body, null, null);
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "SMS send error", e);
            return false;
        }
    }

    private void confirm(String id) {
        try {
            URL url = new URL(SERVER_URL + "/confirm/" + id);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("X-Token", SECRET_TOKEN);
            conn.setDoOutput(true);
            conn.getOutputStream().write("{}".getBytes());
            conn.getResponseCode();
            conn.disconnect();
        } catch (Exception e) {
            Log.e(TAG, "Confirm error", e);
        }
    }

    private void log(String msg) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        String time = sdf.format(new Date());
        runOnUiThread(() -> {
            String current = logView.getText().toString();
            logView.setText("[" + time + "] " + msg + "\n" + current);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(pollRunnable);
    }
}