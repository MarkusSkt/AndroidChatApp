package com.example.socketiochatapplication;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.socketiochatapplication.data.Message;

import java.util.ArrayList;

/** Recycler view for messages */
public class ChatBoxAdapter extends RecyclerView.Adapter<ChatBoxAdapter.MyViewHolder> {

    private static final String COMMA = ": ";

    /** List of currently visible messages */
    private ArrayList<Message> mMessages;

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView mNicknameText;
        public TextView mMessageText;
        public TextView mTimeStampText;

        public MyViewHolder(View view) {
            super(view);

            mNicknameText = view.findViewById(R.id.nickname);
            mMessageText = view.findViewById(R.id.message);
            mTimeStampText = view.findViewById(R.id.timestamp);
        }
    }

    public ChatBoxAdapter(ArrayList<Message> messages) {
        this.mMessages = messages;
    }

    @Override
    public int getItemCount() {
        return mMessages.size();
    }

    @NonNull
    @Override
    public ChatBoxAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        /* Create the items we use for the list items */
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.message_item, parent,false);

        return new ChatBoxAdapter.MyViewHolder(itemView);
    }

    /** Populates the @holder item data with the custom data we want */
    @Override
    public void onBindViewHolder(final ChatBoxAdapter.MyViewHolder holder, final int position) {
        //binding the data from our ArrayList of object to the item.xml using the viewHolder
        Message m = mMessages.get(position);

        String nameText = m.getNickname() + COMMA;

        holder.mNicknameText.setText(nameText);
        holder.mMessageText.setText(m.getMessage());
        holder.mTimeStampText.setText((m.getTimeStamp()));
    }
}
