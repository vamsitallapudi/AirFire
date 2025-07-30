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
 * Enhanced AirFire Activity with dual protocol support
 * Supports both custom TCP protocol and AirPlay receiver
 */
public class SimpleAirFireActivity extends Activity implements SurfaceHolder.Callback, AirPlayReceiver.StatusCallback {
    
    private static final int AIRFIRE_PORT = 5000;
    
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private TextView statusText;
    private TextView ipText;
    private TextView protocolText;
    
    // TCP Server (original functionality)
    private ServerSocket serverSocket;
    private boolean isRunning = false;
    
    // AirPlay Receiver (new functionality)
    private AirPlayReceiver airPlayReceiver;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Create enhanced UI
        setupUI();
        
        // Show IP address
        showIPAddress();
        
        // Start both servers
        startTCPServer();
        startAirPlayReceiver();
    }
    
    private void setupUI() {
        setContentView(R.layout.activity_simple_airfire);
        
        surfaceView = findViewById(R.id.surface_view);
        statusText = findViewById(R.id.status_text);
        ipText = findViewById(R.id.ip_text);
        protocolText = findViewById(R.id.protocol_text); // New text view for protocol info
        
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        
        updateStatus("🔥 AirFire ready - Dual protocol support");
    }
    
    private void showIPAddress() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        String ip = Formatter.formatIpAddress(wifiManager.getConnectionInfo().getIpAddress());
        
        runOnUiThread(() -> {
            if (ipText != null) {
                ipText.setText("Fire TV IP: " + ip);
            }
            if (protocolText != null) {
                protocolText.setText("📱 Custom TCP: " + ip + ":5000\\n🍎 AirPlay: " + ip + ":7000");
            }
        });
    }
    
    private void startTCPServer() {
        isRunning = true;
        
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(AIRFIRE_PORT);
                updateStatus("📱 TCP server ready on port " + AIRFIRE_PORT);
                
                while (isRunning && !isFinishing()) {
                    try {
                        // Wait for custom protocol connection
                        Socket clientSocket = serverSocket.accept();
                        
                        runOnUiThread(() -> {
                            updateStatus("📱 Custom client connected: " + clientSocket.getRemoteSocketAddress());
                            handleTCPConnection(clientSocket);
                        });
                        
                    } catch (IOException e) {
                        if (isRunning && !isFinishing()) {
                            runOnUiThread(() -> updateStatus("❌ TCP connection error: " + e.getMessage()));
                        }
                    }
                }
            } catch (IOException e) {
                runOnUiThread(() -> updateStatus("❌ TCP server error: " + e.getMessage()));
            }
        }).start();
    }
    
    private void startAirPlayReceiver() {
        airPlayReceiver = new AirPlayReceiver(this, this);
        airPlayReceiver.start();
    }
    
    private void handleTCPConnection(Socket clientSocket) {
        updateStatus("📱 Ready to receive custom TCP stream...");
        
        new Thread(() -> {
            try {
                byte[] buffer = new byte[1024];
                while (isRunning && !clientSocket.isClosed()) {
                    int bytesRead = clientSocket.getInputStream().read(buffer);
                    if (bytesRead > 0) {
                        runOnUiThread(() -> 
                            updateStatus("📱 TCP data: " + bytesRead + " bytes")
                        );
                    }
                    
                    Thread.sleep(100);
                }
            } catch (Exception e) {
                runOnUiThread(() -> updateStatus("📱 TCP client disconnected"));
            }
        }).start();
    }
    
    // AirPlayReceiver.StatusCallback implementation
    @Override
    public void onStatusUpdate(String message) {
        runOnUiThread(() -> updateStatus(message));
    }
    
    @Override
    public void onAirPlayConnection(String clientAddress) {
        runOnUiThread(() -> updateStatus("🍎 iOS device connected via AirPlay: " + clientAddress));
    }
    
    @Override
    public void onVideoData(byte[] data) {
        runOnUiThread(() -> updateStatus("🍎 AirPlay video data: " + data.length + " bytes"));
        
        // TODO: Process AirPlay video data (H.264 stream)
        // This is where you'd decode and display the video
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
        
        // Stop TCP server
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                // Ignore
            }
        }
        
        // Stop AirPlay receiver
        if (airPlayReceiver != null) {
            airPlayReceiver.stop();
        }
    }
}