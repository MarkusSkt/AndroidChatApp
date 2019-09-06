package com.example.socketiochatapplication.data;

import android.graphics.Bitmap;
import android.net.Uri;

import java.net.URL;

public class Room {

    private String mName;

    private Bitmap mImageBitmap;
    private Uri mImageUri;
    private URL mImageURL;

    public Room(String name, Bitmap imageBitmap, URL imageURL) {
        mName = name;
        mImageBitmap = imageBitmap;
        mImageURL = imageURL;
    }

    public Room(String name, Bitmap imageBitmap, Uri imageUri) {
        mName = name;
        mImageBitmap = imageBitmap;
        mImageUri = imageUri;
    }

    public String getName() {
        return mName;
    }

    public Bitmap getImageBitmap() {
        return mImageBitmap;
    }

    public Uri getImageUri() {
        return mImageUri;
    }

    public URL getImageURL() {
        return mImageURL;
    }
}
