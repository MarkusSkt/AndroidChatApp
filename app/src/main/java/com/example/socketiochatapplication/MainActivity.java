package com.example.socketiochatapplication;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Activity for entering the chat lobby
 */
public class MainActivity extends AppCompatActivity {

    public static final String NICKNAME_KEY = "nickname";

    private Button mEnterChatButton;
    private EditText mNickNameText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mEnterChatButton = findViewById(R.id.enterchatButton);
        mNickNameText = findViewById(R.id.nicknameText);

        connectSocket();
        setEnterChatButtonListener();
    }

    private void connectSocket() {
        Global global = ((Global) getApplicationContext());
        global.connectSocket();
    }

    /**
     * Adds the @mEnterChatButton as the listener
     * for entering chat room
     */
    private void setEnterChatButtonListener() {
        if (mEnterChatButton == null) {
            mEnterChatButton = findViewById(R.id.enterchatButton);
        }

        mEnterChatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String nickname = mNickNameText.getText().toString();

                if (nickname.isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Nickname is too short!", Toast.LENGTH_SHORT).show();
                    return;
                }

                Intent intent = new Intent(MainActivity.this, RoomActivity.class);
                intent.putExtra(NICKNAME_KEY, nickname);
                startActivity(intent);
            }
        });
    }
}
