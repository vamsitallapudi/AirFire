package com.airfire.receiver;

import android.app.Activity;
import android.os.Bundle;
import android.view.SurfaceView;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.view.Surface;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.DataInputStream;
import java.nio.ByteBuffer;

public class MainActivity extends Activity {
    private MediaCodec decoder;
    private Surface surface;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SurfaceView surfaceView = new SurfaceView(this);
        setContentView(surfaceView);
        surface = surfaceView.getHolder().getSurface();
        
        new Thread(this::startReceiver).start();
    }
    
    private void startReceiver() {
        try {
            // Setup decoder
            MediaFormat format = MediaFormat.createVideoFormat("video/avc", 1920, 1080);
            decoder = MediaCodec.createDecoderByType("video/avc");
            decoder.configure(format, surface, null, 0);
            decoder.start();
            
            // Start TCP server
            ServerSocket server = new ServerSocket(5000);
            Socket client = server.accept();
            DataInputStream input = new DataInputStream(client.getInputStream());
            
            // Receive and decode
            byte[] buffer = new byte[100000];
            while (true) {
                int length = input.readInt();
                input.readFully(buffer, 0, length);
                
                int inputIndex = decoder.dequeueInputBuffer(10000);
                if (inputIndex >= 0) {
                    ByteBuffer inputBuffer = decoder.getInputBuffer(inputIndex);
                    inputBuffer.clear();
                    inputBuffer.put(buffer, 0, length);
                    decoder.queueInputBuffer(inputIndex, 0, length, 0, 0);
                }
                
                MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                int outputIndex = decoder.dequeueOutputBuffer(info, 10000);
                if (outputIndex >= 0) {
                    decoder.releaseOutputBuffer(outputIndex, true);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}