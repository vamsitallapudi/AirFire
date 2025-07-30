package com.airfire;

import android.app.Activity;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;
import android.net.wifi.WifiManager;
import android.content.Context;
import android.text.format.Formatter;

import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;

/**
 * Simple AirFire test activity - Basic Fire TV receiver
 * This is a minimal version to test basic functionality
 */
public class SimpleAirFireActivity extends Activity implements SurfaceHolder.Callback {
    
    private static final int AIRFIRE_PORT = 5000;
    
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private TextView statusText;
    private TextView ipText;
    private ServerSocket serverSocket;
    private boolean isRunning = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Create simple UI
        setupUI();
        
        // Show IP address
        showIPAddress();
        
        // Start listening for iPhone connections
        startServer();
    }
    
    private void setupUI() {
        setContentView(R.layout.activity_simple_airfire);
        
        surfaceView = findViewById(R.id.surface_view);
        statusText = findViewById(R.id.status_text);
        ipText = findViewById(R.id.ip_text);
        
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        
        updateStatus("AirFire ready - Waiting for iPhone...");
    }
    
    private void showIPAddress() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        String ip = Formatter.formatIpAddress(wifiManager.getConnectionInfo().getIpAddress());
        
        runOnUiThread(() -> {
            if (ipText != null) {
                ipText.setText("Fire TV IP: " + ip + ":" + AIRFIRE_PORT);
            }
        });
    }
    
    private void startServer() {
        isRunning = true;
        
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(AIRFIRE_PORT);
                updateStatus("🔥 AirFire ready on port " + AIRFIRE_PORT);
                
                while (isRunning && !isFinishing()) {
                    try {
                        // Wait for iPhone connection
                        Socket clientSocket = serverSocket.accept();
                        
                        runOnUiThread(() -> {
                            updateStatus("📱 iPhone connected from " + clientSocket.getRemoteSocketAddress());
                            handleConnection(clientSocket);
                        });
                        
                    } catch (IOException e) {
                        if (isRunning && !isFinishing()) {
                            runOnUiThread(() -> updateStatus("❌ Connection error: " + e.getMessage()));
                        }
                    }
                }
            } catch (IOException e) {
                runOnUiThread(() -> updateStatus("❌ Server error: " + e.getMessage()));
            }
        }).start();
    }
    
    private void handleConnection(Socket clientSocket) {
        updateStatus("🎬 Ready to receive video stream...");
        
        // For now, just keep the connection alive and show status
        new Thread(() -> {
            try {
                byte[] buffer = new byte[1024];
                while (isRunning && !clientSocket.isClosed()) {
                    int bytesRead = clientSocket.getInputStream().read(buffer);
                    if (bytesRead > 0) {
                        runOnUiThread(() -> 
                            updateStatus("📡 Receiving data: " + bytesRead + " bytes")
                        );
                    }
                    
                    Thread.sleep(100); // Don't spam updates
                }
            } catch (Exception e) {
                runOnUiThread(() -> updateStatus("📱 iPhone disconnected"));
            }
        }).start();
    }
    
    private void updateStatus(String message) {
        if (statusText != null) {
            statusText.setText(message);
        }
    }
    
    // SurfaceHolder.Callback implementation
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        updateStatus("📺 Display surface ready");
    }
    
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        updateStatus("📺 Display: " + width + "x" + height);
    }
    
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        updateStatus("📺 Display surface destroyed");
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        isRunning = false;
        
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }
}