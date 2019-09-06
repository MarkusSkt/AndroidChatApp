package com.example.socketiochatapplication;

import android.app.Application;
import android.util.Log;

import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import java.net.URISyntaxException;

public class Global extends Application {

    private static final String CONNECT_URI = "SET IP";

    private Socket mSocket;

    public void connectSocket() {
        try {
            mSocket = IO.socket(CONNECT_URI);
        } catch (URISyntaxException e) {
            Log.e("Connect Error:", e.getMessage());
        }
    }

    public Socket getSocket() {
        return mSocket;
    }
}
