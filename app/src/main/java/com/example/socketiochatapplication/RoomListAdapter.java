package com.example.socketiochatapplication;

import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.socketiochatapplication.data.ImageUtil;
import com.example.socketiochatapplication.data.Room;

import java.net.URL;
import java.util.ArrayList;

public class RoomListAdapter extends RecyclerView.Adapter<RoomListAdapter.MyViewHolder> {

    private ArrayList<Room> mRooms;

    private OnJoinRoomListener mJoinCallback;

    /**
     * Individual view entry
     */
    public class MyViewHolder extends RecyclerView.ViewHolder {

        public TextView mRoomName;
        public ImageView mRoomImage;
        public Button mJoinButton;

        public MyViewHolder(View view) {
            super(view);

            mRoomName = view.findViewById(R.id.room_name);
            mRoomImage = view.findViewById(R.id.room_image);
            mJoinButton = view.findViewById(R.id.room_join_button);
        }
    }

    public RoomListAdapter(OnJoinRoomListener listener) {
        mRooms = new ArrayList<>();
        mJoinCallback = listener;
    }

    public RoomListAdapter(ArrayList<Room> rooms) {
        mRooms = rooms;
    }

    public void setOnJoinClickedListener(OnJoinRoomListener listener) {
        mJoinCallback = listener;
    }

    @NonNull
    @Override
    public RoomListAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        /* Create the items we use for the list items */
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.room_item, parent, false);

        return new RoomListAdapter.MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull RoomListAdapter.MyViewHolder holder, int position) {
        final Room room = mRooms.get(position);

        Bitmap roomImageBitmap = room.getImageBitmap();

        if (roomImageBitmap != null) {
            holder.mRoomImage.setImageBitmap(roomImageBitmap);
        }

        holder.mRoomName.setText(room.getName());

        holder.mJoinButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mJoinCallback != null) {
                    mJoinCallback.OnJoinRoomClicked(room);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mRooms.size();
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        mJoinCallback = null;
    }
}

/**
 * Interface for room creation event
 */
interface OnJoinRoomListener {
    void OnJoinRoomClicked(Room room);
}
