package com.airfire;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles AirPlay HTTP protocol requests
 * Implements basic AirPlay server functionality for iOS screen mirroring
 */
public class AirPlayHTTPHandler {
    private static final String TAG = "AirPlayHTTP";
    
    private Socket clientSocket;
    private AirPlayReceiver.StatusCallback statusCallback;
    private BufferedReader reader;
    private OutputStream outputStream;
    
    public AirPlayHTTPHandler(Socket clientSocket, AirPlayReceiver.StatusCallback callback) {
        this.clientSocket = clientSocket;
        this.statusCallback = callback;
    }
    
    public void handleConnection() throws IOException {
        reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        outputStream = clientSocket.getOutputStream();
        
        String requestLine;
        while ((requestLine = reader.readLine()) != null && !requestLine.isEmpty()) {
            Log.d(TAG, "AirPlay request: " + requestLine);
            
            if (requestLine.startsWith("GET") || requestLine.startsWith("POST")) {
                handleHTTPRequest(requestLine);
                break; // Handle one request per connection for simplicity
            }
        }
    }
    
    private void handleHTTPRequest(String requestLine) throws IOException {
        String[] parts = requestLine.split(" ");
        if (parts.length < 2) return;
        
        String method = parts[0];
        String path = parts[1];
        
        Log.d(TAG, "Handling " + method + " " + path);
        statusCallback.onStatusUpdate("🍎 AirPlay: " + method + " " + path);
        
        // Read headers
        Map<String, String> headers = new HashMap<>();
        String headerLine;
        while ((headerLine = reader.readLine()) != null && !headerLine.isEmpty()) {
            String[] headerParts = headerLine.split(": ", 2);
            if (headerParts.length == 2) {
                headers.put(headerParts[0], headerParts[1]);
            }
        }
        
        // Handle different AirPlay endpoints
        switch (path) {
            case "/server-info":
                handleServerInfo();
                break;
            case "/play":
                handlePlay(headers);
                break;
            case "/scrub":
                handleScrub();
                break;
            case "/stop":
                handleStop();
                break;
            case "/photo":
                handlePhoto(headers);
                break;
            default:
                handleNotFound();
                break;
        }
    }
    
    private void handleServerInfo() throws IOException {
        statusCallback.onStatusUpdate("🍎 Providing server info to iOS device");
        
        String response = 
            "HTTP/1.1 200 OK\r\n" +
            "Date: " + new java.util.Date() + "\r\n" +
            "Content-Type: text/x-apple-plist+xml\r\n" +
            "Content-Length: 500\r\n" +
            "\r\n" +
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n" +
            "<plist version=\"1.0\">\n" +
            "<dict>\n" +
            "    <key>deviceid</key>\n" +
            "    <string>AA:BB:CC:DD:EE:FF</string>\n" +
            "    <key>features</key>\n" +
            "    <integer>0x445F8A00</integer>\n" +
            "    <key>model</key>\n" +
            "    <string>AirFire1,1</string>\n" +
            "    <key>protovers</key>\n" +
            "    <string>1.0</string>\n" +
            "    <key>srcvers</key>\n" +
            "    <string>366.0</string>\n" +
            "</dict>\n" +
            "</plist>";
        
        outputStream.write(response.getBytes());
        outputStream.flush();
    }
    
    private void handlePlay(Map<String, String> headers) throws IOException {
        statusCallback.onStatusUpdate("🍎 Starting AirPlay video stream");
        
        String contentLength = headers.get("Content-Length");
        if (contentLength != null) {
            int length = Integer.parseInt(contentLength);
            byte[] content = new byte[length];
            
            // Read the video stream content
            int bytesRead = 0;
            while (bytesRead < length) {
                int read = clientSocket.getInputStream().read(content, bytesRead, length - bytesRead);
                if (read == -1) break;
                bytesRead += read;
            }
            
            statusCallback.onStatusUpdate("🍎 Received " + bytesRead + " bytes of video data");
            statusCallback.onVideoData(content);
        }
        
        // Send OK response
        String response = "HTTP/1.1 200 OK\r\n\r\n";
        outputStream.write(response.getBytes());
        outputStream.flush();
    }
    
    private void handleScrub() throws IOException {
        statusCallback.onStatusUpdate("🍎 AirPlay scrub request");
        
        String response = "HTTP/1.1 200 OK\r\n\r\n";
        outputStream.write(response.getBytes());
        outputStream.flush();
    }
    
    private void handleStop() throws IOException {
        statusCallback.onStatusUpdate("🍎 AirPlay stream stopped");
        
        String response = "HTTP/1.1 200 OK\r\n\r\n";
        outputStream.write(response.getBytes());
        outputStream.flush();
    }
    
    private void handlePhoto(Map<String, String> headers) throws IOException {
        statusCallback.onStatusUpdate("🍎 AirPlay photo received");
        
        String response = "HTTP/1.1 200 OK\r\n\r\n";
        outputStream.write(response.getBytes());
        outputStream.flush();
    }
    
    private void handleNotFound() throws IOException {
        statusCallback.onStatusUpdate("🍎 Unknown AirPlay request");
        
        String response = "HTTP/1.1 404 Not Found\r\n\r\n";
        outputStream.write(response.getBytes());
        outputStream.flush();
    }
}