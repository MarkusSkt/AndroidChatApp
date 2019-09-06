package com.example.socketiochatapplication;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.socketiochatapplication.data.AWSUtil;
import com.example.socketiochatapplication.data.ImageUtil;
import com.example.socketiochatapplication.data.Room;
import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONObject;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Activity for listing all current Rooms and creating new ones
 */
public class RoomActivity extends AppCompatActivity
        implements RoomCreationFragment.OnCreateRoomListener, OnJoinRoomListener {

    private static final String TAG = "RoomActivity";
    public static final String ROOM_NAME_KEY = "room_name";
    public static final String ROOM_IMAGE_URI_KEY = "room_image_uri";
    public static final String ROOM_IMAGE_URL_KEY = "room_image_url";
    private static final String CREATE_ROOM_EVENT = "create_room";
    private static final String ROOM_CREATED_CALLBACK = "room_created";
    private static final String GET_ROOMS_EVENT = "get_rooms";
    private static final String ROOMS_CALLBACK = "rooms";

    private Fragment mHelpFragment;
    private Fragment mRoomCreationFragment;

    private Socket mSocket;
    private String mNickname;

    private TextView mRoomInfoText;
    private RecyclerView mRoomRecyclerView;
    private ProgressBar mRoomProgressBar;

    private AWSUtil mAWSUtility;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room);

        Global global = ((Global) getApplicationContext());
        mSocket = global.getSocket();

        mRoomProgressBar = findViewById(R.id.room_progress_bar);
        mRoomInfoText = findViewById(R.id.rooms_info_text);

        mAWSUtility = new AWSUtil(getApplicationContext());

        connectToServer();

        getExtras();
        setToolbar();
        initRoomAdapter();
        initRoomCreation();

        refreshRoomsList();
    }

    private void setToolbar() {
        Toolbar toolbar = findViewById(R.id.my_toolbar);
        toolbar.setTitle("");//
        setSupportActionBar(toolbar);
    }

    /**
     * Connect your socket to the Server
     */
    private void connectToServer() {
        mSocket.connect();
        Log.i(TAG, "Connecting to server...");
    }

    private void showHelpFragment() {
        if (mHelpFragment == null) {
            mHelpFragment = new HelpFragment();
        }

        FragmentTransaction fragTransaction = getSupportFragmentManager().beginTransaction();

        if (mRoomCreationFragment != null && mRoomCreationFragment.isAdded()) {
            fragTransaction.hide(mRoomCreationFragment);
        }

        fragTransaction
                .add(R.id.frameLayout, mHelpFragment)
                .addToBackStack(null)
                .setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
                .commit();
    }

    private void showCreationFragment() {
        if (mRoomCreationFragment == null) {
            mRoomCreationFragment = new RoomCreationFragment();
        }

        FragmentTransaction fragTransaction = getSupportFragmentManager().beginTransaction();

        if (mHelpFragment != null && mHelpFragment.isAdded()) {
            fragTransaction.hide(mHelpFragment);
        }

        fragTransaction
                .add(R.id.frameLayout, mRoomCreationFragment)
                .addToBackStack(null)
                .setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
                .commit();
    }

    @SuppressWarnings("ConstantConditions")
    private void getExtras() {
        try {
            mNickname = getIntent().getExtras().getString(MainActivity.NICKNAME_KEY);
        } catch (NullPointerException e) {
            Log.e(TAG, "Nickname not found");
        }
    }

    /**
     * Sets the room list adapter
     */
    @SuppressWarnings("ConstantConditions")
    private void initRoomAdapter() {
        mRoomRecyclerView = findViewById(R.id.rooms_list);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        mRoomRecyclerView.setLayoutManager(layoutManager);

        mRoomRecyclerView.setAdapter(new RoomListAdapter(this));

        DividerItemDecoration itemDecoration = new DividerItemDecoration(mRoomRecyclerView.getContext(), DividerItemDecoration.VERTICAL);
        itemDecoration.setDrawable(ContextCompat.getDrawable(RoomActivity.this, R.drawable.divider));
        mRoomRecyclerView.addItemDecoration((itemDecoration));
    }

    /**
     * Refreshes the rooms list recycler view
     */
    private void refreshRoomsList() {
        findViewById(R.id.create_button).setVisibility(View.INVISIBLE);

        final Context context = this;

        mSocket.off(ROOMS_CALLBACK);
        mSocket.on(ROOMS_CALLBACK, new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        RoomsFetchAsyncTask roomFetchTask = new RoomsFetchAsyncTask(context);
                        roomFetchTask.execute(args);
                    }
                });
            }
        });

        mRoomInfoText.setText(R.string.fetching_rooms);
        mRoomProgressBar.setVisibility(View.VISIBLE);
        mSocket.emit(GET_ROOMS_EVENT);
    }

    /**
     * Opens room creation fragment on click
     */
    private void initRoomCreation() {
        ImageButton createButton = findViewById(R.id.create_button);

        createButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showCreationFragment();
            }
        });
    }

    /**
     * Give info to the client if no rooms found
     */
    private void setRoomInfoText(boolean roomsFound) {
        if (mRoomInfoText == null) {
            mRoomInfoText = findViewById(R.id.rooms_info_text);
        }

        if (!roomsFound) {
            mRoomInfoText.setVisibility(View.VISIBLE);
            mRoomInfoText.setText(R.string.no_rooms);
        } else {
            mRoomInfoText.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Parses Room objects from Object array (received from server)
     */
    private ArrayList<Room> getParsedRooms(Object[] objects) {
        ArrayList<Room> rooms = new ArrayList<>();

        if (objects == null) {
            return rooms;
        }

        for (Object object : objects) {

            JSONObject roomJSONObject = (JSONObject) object;

            Log.i("getParsedRooms", roomJSONObject.length() + "");

            for (Iterator<String> iterator = roomJSONObject.keys(); iterator.hasNext(); ) {

                String roomName = iterator.next();

                Log.i(TAG, "Parsed Room: " + roomName + "\n");

                /* TODO : The @object also returns all of the client IDs, make better filtering */
                if (roomName.length() > 15) {
                    continue;
                }

                URL url = getImageURL(roomName);
                Bitmap bitmap = null;

                if (url != null) {
                    try {
                        bitmap = ImageUtil.getBitmapFromStream(url);
                      /*  bitmap = ImageUtil.getCroppedBitmap(bitmap);*/
                    } catch (IOException e) {
                        Log.e("Error Getting Bitmap", e.getMessage());
                    }
                }

                Room room = new Room(roomName, bitmap, url);
                rooms.add(room);
            }
        }
        return rooms;
    }

    /* Begins to upload the file specified by the file path.
     */
    private URL getImageURL(String roomName) {
        if (roomName == null) {
            Toast.makeText(
                    this,
                    "Could not find the filepath of the selected file",
                    Toast.LENGTH_LONG).show();
            return null;
        }

        URL url = mAWSUtility.getS3Client().getUrl(
                "android-chat-image-storage",
                roomName + "_" + AWSUtil.IMAGE_NAME_END + ".jpg");

        Log.i("getUrl", url.toString());
        return url;
    }

    /**
     * Creates new room and changes Activity
     */
    public void createRoom(final Room room) {
        mSocket.off(ROOM_CREATED_CALLBACK);
        mSocket.on(ROOM_CREATED_CALLBACK, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Intent intent = new Intent(RoomActivity.this, ChatBoxActivity.class);
                intent.putExtra(MainActivity.NICKNAME_KEY, mNickname);
                intent.putExtra(ROOM_NAME_KEY, room.getName());

                URL url = room.getImageURL();
                Uri uri = room.getImageUri();

                if(url != null) {
                    Log.i("putExtra", url.toString());
                    intent.putExtra(ROOM_IMAGE_URL_KEY, url.toString());
                }
                if(uri != null) {
                    Log.i("putExtra", uri.toString());
                    intent.putExtra(ROOM_IMAGE_URI_KEY, uri);
                }

                finish();
                startActivity(intent);
            }
        });

        mSocket.emit(CREATE_ROOM_EVENT, room.getName());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.appbar_menu_room, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_help:
                showHelpFragment();
                return true;
            case R.id.action_refresh:
                refreshRoomsList();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void OnCreateRoomClicked(String roomName, Bitmap bitmap, Uri uri) {
        Log.i(TAG, "OnCreateRoomClicked: ");
        createRoom(new Room(roomName, bitmap, uri));
    }

    @Override
    public void OnJoinRoomClicked(Room room) {
        Log.i(TAG, "OnJoinRoomClicked: ");
        createRoom(room);
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
        } else {
            Intent intent = new Intent(RoomActivity.this, MainActivity.class);
            startActivity(intent);

            mSocket.disconnect();
            finish();
        }
    }

    private static class RoomsFetchAsyncTask extends AsyncTask<Object, Object, ArrayList<Room>> {

        private WeakReference<Context> mWeakContext;

        private RoomsFetchAsyncTask(Context context) {
            mWeakContext = new WeakReference<>(context);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected ArrayList<Room> doInBackground(Object... objects) {
            Log.i("doInBackground", objects.length + "");
            RoomActivity activity = (RoomActivity) mWeakContext.get();
            return activity.getParsedRooms(objects);

        }

        @Override
        protected void onPostExecute(ArrayList<Room> rooms) {
            super.onPostExecute(rooms);

            final RoomActivity activity = (RoomActivity) mWeakContext.get();

            /*  Update the room recycler view */
            RoomListAdapter roomListAdapter = new RoomListAdapter(rooms);

            roomListAdapter.setOnJoinClickedListener(new OnJoinRoomListener() {
                @Override
                public void OnJoinRoomClicked(Room room) {
                    Log.i(TAG, "OnJoinRoomClicked: ");
                    activity.createRoom(room);
                }
            });

            activity.mRoomRecyclerView.setAdapter((roomListAdapter));
            roomListAdapter.notifyDataSetChanged();

            activity.setRoomInfoText(rooms.size() > 0);
            activity.mRoomProgressBar.setVisibility(View.INVISIBLE);
            activity.findViewById(R.id.create_button).setVisibility(View.VISIBLE);
        }
    }
}
