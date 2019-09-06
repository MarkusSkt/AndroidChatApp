package com.example.socketiochatapplication;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.example.socketiochatapplication.data.AWSUtil;

import java.io.File;
import java.util.Objects;

import static android.app.Activity.RESULT_OK;

/**
 * Fragment for creating a new chat room
 */
public class RoomCreationFragment extends Fragment {

    private static final String TAG = "RoomCreationFragment";
    private static final int MIN_ROOM_LENGTH = 3;
    private static final int IMAGE_REQUEST_CODE = 0;
    private static final int PERMISSION_REQUEST_CODE = 1;

    private OnCreateRoomListener mCreationCallback;

    private EditText mRoomNameText;
    private ImageView mRoomImageButton;

    private Uri mSelectedImageUri;
    private Bitmap mSelectedImageBitmap;

    private AWSUtil mAWSUtility;

    public RoomCreationFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_room_creation, container, false);
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAWSUtility = new AWSUtil(getActivity().getApplicationContext());
        mRoomNameText = view.findViewById(R.id.room_name_edit);

        mRoomImageButton = view.findViewById(R.id.image_room);
        mRoomImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectImageFromGallery();
            }
        });

        initCreateRoomButton(view);
    }

    private void initCreateRoomButton(View view) {
        Button createRoomButton = view.findViewById(R.id.create_room_button);

        createRoomButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String roomName = mRoomNameText.getText().toString();

                /* Validate the room name */
                if (roomName.length() < MIN_ROOM_LENGTH) {
                    Toast.makeText(getActivity(), "Room name is too short", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (mSelectedImageUri != null) {
                    try {
                        beginUpload(getRealPathFromURI(mSelectedImageUri));
                    } catch (Exception e) {
                        Toast.makeText(
                                getActivity(),
                                "Unable to get the file from the given URI. See error log for details",
                                Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Unable to upload file from the given uri", e);
                    }
                }

                /* Callback that the create room was clicked */
                if (mCreationCallback != null) {
                    Log.i(TAG, "OnCreateRoomClicked");
                    mCreationCallback.OnCreateRoomClicked(
                            roomName,
                            mSelectedImageBitmap,
                            mSelectedImageUri);
                }
            }
        });
    }

    private void selectImageFromGallery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Objects.requireNonNull(getActivity()).checkSelfPermission(
                    Manifest.permission.READ_EXTERNAL_STORAGE) ==
                    PackageManager.PERMISSION_DENIED) {

                String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE};
                requestPermissions(permissions, PERMISSION_REQUEST_CODE);
                return;
            }
        }

        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, IMAGE_REQUEST_CODE);
    }

    /* Begins to upload the file specified by the file path.
     */
    private void beginUpload(String filePath) {
        if (filePath == null) {
            Toast.makeText(
                    getActivity(),
                    "Could not find the filepath of the selected file",
                    Toast.LENGTH_LONG).show();
            return;
        }

        Log.i("beginUpload", "Upload");

        File file = new File(filePath);
        String extension = filePath.substring(filePath.lastIndexOf("."));

        TransferObserver observer = mAWSUtility.getTransferUtility().upload(
                mRoomNameText.getText().toString() + "_" + AWSUtil.IMAGE_NAME_END + extension,
                file,
                CannedAccessControlList.PublicRead
        );

        observer.cleanTransferListener();
        observer.setTransferListener(new UploadListener());
    }

    @SuppressWarnings("ConstantConditions")
    private String getRealPathFromURI(Uri contentURI) {
        String result;
        Cursor cursor = getActivity().getContentResolver().query(contentURI, null, null, null, null);
        if (cursor == null) { // Source is Dropbox or other similar local file path
            result = contentURI.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            result = cursor.getString(idx);
            cursor.close();
        }
        return result;
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        final int maxSize = 2000;

        if (resultCode == RESULT_OK && requestCode == IMAGE_REQUEST_CODE) {
            Uri imageUri = data.getData();

            try {
                ContentResolver resolver = getActivity().getContentResolver();
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(resolver, imageUri);

                int width = bitmap.getWidth();
                int height = bitmap.getHeight();

                if (width > maxSize || height > maxSize) {
                    Toast.makeText(
                            getActivity(),
                            "Image size " + bitmap.getWidth() + "x" + bitmap.getHeight() + " is too big!",
                            Toast.LENGTH_SHORT).
                            show();
                    return;
                }

                mRoomImageButton.setImageBitmap(bitmap);

                mSelectedImageUri = imageUri;
                mSelectedImageBitmap = bitmap;
            } catch (Exception e) {
                Log.e("getBitmap", "Failed fetching bitmap");
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case PERMISSION_REQUEST_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    selectImageFromGallery();
                } else {
                    Toast.makeText(getContext(), "Permission denied!", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnCreateRoomListener) {
            mCreationCallback = (OnCreateRoomListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCreationCallback = null;
    }

    /**
     * Interface for room creation event
     */
    public interface OnCreateRoomListener {
        void OnCreateRoomClicked(String name, Bitmap bitmap, Uri uri);
    }

    /**
     * A TransferListener class that can listen to a upload task and be notified
     * when the status changes.
     */
    static class UploadListener implements TransferListener {
        @Override
        public void onError(int id, Exception e) {
            Log.e(TAG, "Error during upload: " + id, e);
        }

        @Override
        public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
            Log.d(TAG, String.format("onProgressChanged: %d, total: %d, current: %d",
                    id, bytesTotal, bytesCurrent));
        }

        @Override
        public void onStateChanged(int id, TransferState newState) {
            Log.d(TAG, "onStateChanged: " + id + ", " + newState);

            if (newState == TransferState.CANCELED || newState == TransferState.FAILED || newState == TransferState.WAITING_FOR_NETWORK) {
                Log.e(TAG, "Error during upload: " + id + " " + newState.toString());
            }
        }
    }
}
