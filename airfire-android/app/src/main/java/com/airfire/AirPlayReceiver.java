package com.airfire;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;
import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/**
 * AirPlay Receiver for Fire TV
 * Makes the Fire TV appear as an AirPlay receiver in iOS Control Center
 */
public class AirPlayReceiver {
    private static final String TAG = "AirFireAirPlay";
    private static final int AIRPLAY_PORT = 7000;
    private static final String SERVICE_TYPE = "_airplay._tcp";
    private static final String SERVICE_NAME = "AirFire";
    
    private Context context;
    private StatusCallback statusCallback;
    private NsdManager nsdManager;
    private ServerSocket airplayServer;
    private boolean isRunning = false;
    
    public interface StatusCallback {
        void onStatusUpdate(String message);
        void onAirPlayConnection(String clientAddress);
        void onVideoData(byte[] data);
    }
    
    public AirPlayReceiver(Context context, StatusCallback callback) {
        this.context = context;
        this.statusCallback = callback;
        this.nsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
    }
    
    public void start() {
        if (isRunning) return;
        
        isRunning = true;
        statusCallback.onStatusUpdate("🍎 Starting AirPlay receiver...");
        
        // Start AirPlay HTTP server
        startAirPlayServer();
        
        // Advertise AirPlay service via Bonjour
        advertiseAirPlayService();
    }
    
    public void stop() {
        isRunning = false;
        
        // Stop service advertisement
        if (nsdManager != null) {
            try {
                nsdManager.unregisterService(registrationListener);
            } catch (Exception e) {
                Log.w(TAG, "Error unregistering service", e);
            }
        }
        
        // Stop AirPlay server
        if (airplayServer != null) {
            try {
                airplayServer.close();
            } catch (IOException e) {
                Log.w(TAG, "Error closing AirPlay server", e);
            }
        }
        
        statusCallback.onStatusUpdate("🍎 AirPlay receiver stopped");
    }
    
    private void startAirPlayServer() {
        new Thread(() -> {
            try {
                airplayServer = new ServerSocket(AIRPLAY_PORT);
                statusCallback.onStatusUpdate("🍎 AirPlay server ready on port " + AIRPLAY_PORT);
                
                while (isRunning) {
                    try {
                        Socket clientSocket = airplayServer.accept();
                        statusCallback.onStatusUpdate("🍎 AirPlay client connected: " + clientSocket.getRemoteSocketAddress());
                        statusCallback.onAirPlayConnection(clientSocket.getRemoteSocketAddress().toString());
                        
                        // Handle AirPlay client in separate thread
                        new Thread(() -> handleAirPlayClient(clientSocket)).start();
                        
                    } catch (IOException e) {
                        if (isRunning) {
                            Log.e(TAG, "AirPlay server error", e);
                            statusCallback.onStatusUpdate("❌ AirPlay server error: " + e.getMessage());
                        }
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Failed to start AirPlay server", e);
                statusCallback.onStatusUpdate("❌ Failed to start AirPlay server: " + e.getMessage());
            }
        }).start();
    }
    
    private void handleAirPlayClient(Socket clientSocket) {
        try {
            AirPlayHTTPHandler handler = new AirPlayHTTPHandler(clientSocket, statusCallback);
            handler.handleConnection();
        } catch (Exception e) {
            Log.e(TAG, "Error handling AirPlay client", e);
            statusCallback.onStatusUpdate("❌ AirPlay client error: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                Log.w(TAG, "Error closing AirPlay client socket", e);
            }
        }
    }
    
    private void advertiseAirPlayService() {
        try {
            NsdServiceInfo serviceInfo = new NsdServiceInfo();
            serviceInfo.setServiceName(SERVICE_NAME);
            serviceInfo.setServiceType(SERVICE_TYPE);
            serviceInfo.setPort(AIRPLAY_PORT);
            
            // Add AirPlay TXT records
            Map<String, String> attributes = new HashMap<>();
            attributes.put("deviceid", getMacAddress());
            attributes.put("features", "0x445F8A00,0x1C340"); // AirPlay features
            attributes.put("model", "AirFire");
            attributes.put("srcvers", "366.0"); // AirPlay source version
            attributes.put("vv", "2"); // Version
            
            // Note: Android NSD doesn't directly support TXT records in older versions
            // For production, consider using jmDNS library for better control
            
            nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener);
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to advertise AirPlay service", e);
            statusCallback.onStatusUpdate("❌ Failed to advertise AirPlay service: " + e.getMessage());
        }
    }
    
    private final NsdManager.RegistrationListener registrationListener = new NsdManager.RegistrationListener() {
        @Override
        public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
            Log.e(TAG, "Service registration failed: " + errorCode);
            statusCallback.onStatusUpdate("❌ AirPlay service registration failed: " + errorCode);
        }
        
        @Override
        public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
            Log.w(TAG, "Service unregistration failed: " + errorCode);
        }
        
        @Override
        public void onServiceRegistered(NsdServiceInfo serviceInfo) {
            Log.i(TAG, "AirPlay service registered: " + serviceInfo.getServiceName());
            statusCallback.onStatusUpdate("🍎 AirPlay service advertised - visible in iOS Control Center");
        }
        
        @Override
        public void onServiceUnregistered(NsdServiceInfo serviceInfo) {
            Log.i(TAG, "AirPlay service unregistered");
        }
    };
    
    private String getMacAddress() {
        // Generate a pseudo MAC address for AirPlay device ID
        // In production, you'd want to use a consistent device identifier
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        String ip = Formatter.formatIpAddress(wifiManager.getConnectionInfo().getIpAddress());
        
        // Create pseudo MAC from IP for simplicity
        String[] parts = ip.split("\\.");
        return String.format("AA:BB:CC:%02X:%02X:%02X", 
            Integer.parseInt(parts[1]) % 256,
            Integer.parseInt(parts[2]) % 256, 
            Integer.parseInt(parts[3]) % 256);
    }
}