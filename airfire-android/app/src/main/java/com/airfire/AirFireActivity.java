package com.airfire;

import android.app.Activity;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;
import android.widget.Toast;

import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;

import com.limelight.binding.video.MediaCodecDecoderRenderer;
import com.limelight.binding.video.CrashListener;
import com.limelight.binding.video.PerfOverlayListener;
import com.limelight.preferences.PreferenceConfiguration;

/**
 * AirFire - Simplified Moonlight for iPhone screen mirroring
 * Removes game-specific features, keeps video decoding excellence
 */
public class AirFireActivity extends Activity implements SurfaceHolder.Callback, CrashListener, PerfOverlayListener {
    
    private static final int AIRFIRE_PORT = 5000;
    
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private TextView statusText;
    private MediaCodecDecoderRenderer decoder;
    private ServerSocket serverSocket;
    private boolean isStreaming = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Create simple UI
        setupUI();
        
        // Start listening for iPhone connections
        startServer();
    }
    
    private void setupUI() {
        setContentView(R.layout.activity_airfire);
        
        surfaceView = findViewById(R.id.surface_view);
        statusText = findViewById(R.id.status_text);
        
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        
        updateStatus("Waiting for iPhone connection...");
    }
    
    private void startServer() {
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(AIRFIRE_PORT);
                updateStatus("AirFire ready on port " + AIRFIRE_PORT);
                
                while (!isFinishing()) {
                    try {
                        // Wait for iPhone connection
                        Socket clientSocket = serverSocket.accept();
                        
                        runOnUiThread(() -> {
                            updateStatus("iPhone connected! Starting stream...");
                            startVideoDecoding(clientSocket);
                        });
                        
                    } catch (IOException e) {
                        if (!isFinishing()) {
                            runOnUiThread(() -> updateStatus("Connection error: " + e.getMessage()));
                        }
                    }
                }
            } catch (IOException e) {
                runOnUiThread(() -> updateStatus("Server error: " + e.getMessage()));
            }
        }).start();
    }
    
    private void startVideoDecoding(Socket clientSocket) {
        if (decoder != null) {
            decoder.cleanup();
        }
        
        // Create simplified preferences for video decoding
        PreferenceConfiguration prefs = new PreferenceConfiguration();
        prefs.width = 1920;  // Default to 1080p
        prefs.height = 1080;
        prefs.fps = 60;
        prefs.bitrate = 10000; // 10 Mbps
        
        // Initialize the same professional decoder Moonlight uses
        decoder = new MediaCodecDecoderRenderer(
            this,           // activity
            prefs,          // preferences  
            this,           // crash listener
            0,              // consecutive crash count
            false,          // metered data
            false,          // HDR
            "Unknown",      // GL renderer
            this            // perf overlay listener
        );
        
        decoder.setRenderTarget(surfaceHolder);
        
        // Setup decoder
        int result = decoder.setup(
            0x01,           // H.264 format
            prefs.width,
            prefs.height, 
            prefs.fps
        );
        
        if (result != 0) {
            updateStatus("Decoder setup failed: " + result);
            return;
        }
        
        decoder.start();
        isStreaming = true;
        updateStatus("Streaming from iPhone...");
        
        // Start receiving data from iPhone
        handleIncomingStream(clientSocket);
    }
    
    private void handleIncomingStream(Socket clientSocket) {
        // This will use Moonlight's existing network protocol
        // to receive H.264 frames from iPhone and feed to decoder
        new Thread(() -> {
            try {
                byte[] buffer = new byte[100000];
                while (isStreaming && !clientSocket.isClosed()) {
                    int bytesRead = clientSocket.getInputStream().read(buffer);
                    if (bytesRead > 0) {
                        // Feed H.264 data to decoder
                        decoder.submitDecodeUnit(
                            buffer,           // decode unit data
                            bytesRead,        // length
                            0,                // decode unit type
                            0,                // frame number
                            0,                // frame type
                            (char)0,          // host processing latency
                            System.currentTimeMillis(), // receive time
                            System.currentTimeMillis()  // enqueue time
                        );
                    }
                }
            } catch (Exception e) {
                runOnUiThread(() -> updateStatus("Stream error: " + e.getMessage()));
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
        // Surface is ready for video rendering
    }
    
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // Handle surface changes
    }
    
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (decoder != null) {
            decoder.stop();
        }
    }
    
    // CrashListener implementation
    @Override
    public void notifyCrash(Exception e) {
        runOnUiThread(() -> {
            updateStatus("Decoder crashed: " + e.getMessage());
            Toast.makeText(this, "Video decoder error", Toast.LENGTH_LONG).show();
        });
    }
    
    // PerfOverlayListener implementation
    @Override
    public void onPerfUpdate(String text) {
        // Could show performance stats if needed
        // For now, just ignore
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        isStreaming = false;
        
        if (decoder != null) {
            decoder.stop();
            decoder.cleanup();
        }
        
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }
}