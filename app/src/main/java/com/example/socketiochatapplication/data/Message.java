package com.example.socketiochatapplication.data;

public class Message {

    private String mNickname;
    private String mMessage ;
    private String mTimeStamp;
    
    public Message(String nickname, String message, String timeStamp) {
        this.mNickname = nickname;
        this.mMessage = message;
        this.mTimeStamp = timeStamp;
    }

    public String getNickname() {
        return mNickname;
    }

    public String getMessage() {
        return mMessage;
    }

    public String getTimeStamp() {
        return mTimeStamp;
    }
}