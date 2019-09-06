package com.example.socketiochatapplication;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.socketiochatapplication.data.ImageUtil;
import com.example.socketiochatapplication.data.Message;
import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Activity for ChatBox events (connection, messages...)
 */
public class ChatBoxActivity extends AppCompatActivity {

    private static final String TAG = "ChatBoxActivity";

    /**
     * Keys for message data send from Server
     */
    private static final String NICKNAME_KEY = "nickname";
    private static final String ONLINE_KEY = "online";
    private static final String NICKNAME_KEY_SENDER = "senderNickname";
    private static final String MESSAGE_KEY = "message";

    /**
     * Events
     */
    private static final String JOINED_EVENT = "joined_room";
    private static final String USER_JOINED_EVENT = "user_joined_room";
    private static final String LEFT_EVENT = "left_room";
    private static final String USER_LEFT_EVENT = "user_left_room";

    private static final String MESSAGE_EVENT = "message";
    private static final String MESSAGE_DETECTION_EVENT = "message_detection_room";

    private Socket mSocket;

    private String mNickname;
    private String mRoomName;
    private Bitmap mRoomBitmap;
    private EditText mMessageText;

    private RecyclerView mMsgRecyclerView;

    private ArrayList<Message> mMessages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_box);

        Global global = ((Global) getApplicationContext());
        mSocket = global.getSocket();
        mMessageText = findViewById(R.id.messageText);

        getExtras();
        initMessageListView();

        initSendButton();

        initJoinedEvent();
        initLeftEvent();
        initMessageEvent();

        initToolbar();
        refreshToolbarData(mRoomName, 0);

        if (savedInstanceState == null) {
            mSocket.emit(JOINED_EVENT, mNickname, mRoomName);
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void getExtras() {
        try {
            Bundle extras = getIntent().getExtras();

            mNickname = extras.getString(MainActivity.NICKNAME_KEY);
            mRoomName = extras.getString(RoomActivity.ROOM_NAME_KEY);

            if (getIntent().hasExtra(RoomActivity.ROOM_IMAGE_URI_KEY)) {
                Uri imageUri = extras.getParcelable(RoomActivity.ROOM_IMAGE_URI_KEY);
                Log.i("getExtras", imageUri.toString());
                mRoomBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                mRoomBitmap = ImageUtil.getCroppedBitmap(mRoomBitmap);
            } else if (getIntent().hasExtra(RoomActivity.ROOM_IMAGE_URL_KEY)) {
                URL imageURL = new URL(extras.getString(RoomActivity.ROOM_IMAGE_URL_KEY));
                Log.i("getExtras", imageURL.toString());
                ConvertImageAsyncTask asyncTask = new ConvertImageAsyncTask(this);
                asyncTask.execute(imageURL);
            }

        } catch (Exception e) {
            Log.e(TAG, "Extras  not found");
        }
    }

    private void initMessageListView() {
        mMsgRecyclerView = findViewById(R.id.message_list);

        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setOrientation(LinearLayoutManager.VERTICAL);
        mMsgRecyclerView.setLayoutManager(manager);
        //set the adapter for the recycler view
        mMsgRecyclerView.setAdapter(new ChatBoxAdapter(new ArrayList<Message>()));
    }

    private void initToolbar() {
        Toolbar myToolbar = findViewById(R.id.my_toolbar);
        myToolbar.setTitle("");

        setSupportActionBar(myToolbar);
    }

    private void refreshToolbarData(String roomName, int online) {
        Toolbar myToolbar = findViewById(R.id.my_toolbar);
        TextView nameText = myToolbar.findViewById(R.id.toolbar_info);

        String text = roomName + " (" + online + ")";
        nameText.setText(text);

        refreshToolbarImage();
    }

    public void refreshToolbarImage() {
        Toolbar myToolbar = findViewById(R.id.my_toolbar);
        ImageView roomImage = myToolbar.findViewById(R.id.toolbar_image);
        roomImage.setVisibility(View.VISIBLE);

        if (mRoomBitmap != null) {
            roomImage.setImageBitmap(mRoomBitmap);
        }
    }

    /**
     * Listens for message event from server, handles sending
     * messages to the recycler view
     */
    private void initJoinedEvent() {
        mSocket.off(USER_JOINED_EVENT);
        mSocket.on(USER_JOINED_EVENT, new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        JSONObject data = (JSONObject) args[0];
                        try {
                            Log.i("User Joined", "USER JOINED");

                            /* extract data from fired event */
                            String nickname = data.getString(NICKNAME_KEY);
                            int usersOnline = data.getInt(ONLINE_KEY);

                            Message joinedMsg = new Message(
                                    "Server",
                                    "Welcome " + nickname,
                                    getTimeStamp());

                            AddMessage(joinedMsg);

                            refreshToolbarData(mRoomName, usersOnline);

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }

    /**
     * Listens for message event from server, handles sending
     * messages to the recycler view
     */
    private void initLeftEvent() {
        mSocket.off(USER_LEFT_EVENT);
        mSocket.on(USER_LEFT_EVENT, new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        JSONObject data = (JSONObject) args[0];
                        try {
                            /* extract data from fired event */
                            String nickname = data.getString(NICKNAME_KEY);
                            int usersOnline = data.getInt(ONLINE_KEY);

                            Message joinedMsg = new Message(
                                    "Server",
                                    nickname + " left the chat!",
                                    getTimeStamp());

                            AddMessage(joinedMsg);

                            refreshToolbarData(mRoomName, usersOnline);

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }

    /**
     * Listens for message event from server, handles sending
     * messages to the recycler view
     */
    private void initMessageEvent() {
        mSocket.off(MESSAGE_EVENT);
        mSocket.on(MESSAGE_EVENT, new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        JSONObject data = (JSONObject) args[0];
                        try {
                            /* extract data from fired event */
                            String nickname = data.getString(NICKNAME_KEY_SENDER);
                            String message = data.getString(MESSAGE_KEY);

                            Message msg = new Message(nickname, message, getTimeStamp());
                            AddMessage(msg);

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }

    private void AddMessage(Message message) {
        /* add the message to the messageList */
        if (mMessages == null) {
            mMessages = new ArrayList<>();
        }

        mMessages.add(message);
        refreshRecyclerView();
    }

    /**
     * Refreshes the Recycler View which shows all messages
     */
    private void refreshRecyclerView() {
        ChatBoxAdapter adapter = new ChatBoxAdapter(mMessages);
        mMsgRecyclerView.setAdapter(adapter);

        /* notify the adapter to update the recycler view */
        adapter.notifyDataSetChanged();
    }

    /**
     * Handles sending message
     */
    private void initSendButton() {
        Button sendButton = findViewById(R.id.sendButton);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String messageText = mMessageText.getText().toString();

                if (messageText.isEmpty()) {
                    return;
                }

                Log.i(TAG, messageText);

                /* Tell the server that we want to send a message */
                sendMessage(messageText);
                mMessageText.setText("");

                Toast.makeText(ChatBoxActivity.this, "Message Sent", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Emits a message
     */
    private void sendMessage(String message) {
        if (message.isEmpty()) {
            return;
        }

        mSocket.emit(MESSAGE_DETECTION_EVENT, mNickname, message, mRoomName);
    }

    private String getTimeStamp() {
        @SuppressLint("SimpleDateFormat") DateFormat dateFormat = new SimpleDateFormat("HH:mm");
        return dateFormat.format(new Date());
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setMessage("Are you sure you want to leave the room?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mSocket.emit(LEFT_EVENT, mNickname, mRoomName);

                        Intent intent = new Intent(ChatBoxActivity.this, RoomActivity.class);
                        intent.putExtra(MainActivity.NICKNAME_KEY, mNickname);

                        finish();

                        startActivity(intent);
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

    private static class ConvertImageAsyncTask extends AsyncTask<URL, Void, Bitmap> {

        private WeakReference<Context> mWeakContext;

        private ConvertImageAsyncTask(Context context) {
            mWeakContext = new WeakReference<>(context);
        }

        @Override
        protected Bitmap doInBackground(URL... url) {
            Bitmap bitmap = null;

            try {
                bitmap = ImageUtil.getBitmapFromStream(url[0]);
            } catch (IOException e) {
                Log.e("Error Getting Bitmap", e.getMessage());
            }

            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            // update the UI (this is executed on UI thread)
            super.onPostExecute(bitmap);

            ChatBoxActivity activity = (ChatBoxActivity) mWeakContext.get();
            activity.mRoomBitmap = bitmap;
            activity.refreshToolbarImage();
        }
    }
}