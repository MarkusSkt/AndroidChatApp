package com.example.socketiochatapplication.data;

import android.content.Context;
import android.util.Log;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.Callback;
import com.amazonaws.mobile.client.UserStateDetails;
import com.amazonaws.mobile.config.AWSConfiguration;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;

import java.util.concurrent.CountDownLatch;

import static com.amazonaws.mobile.client.internal.oauth2.OAuth2Client.TAG;

/**
 * Functions to enable access to AWS S3 and Cognito
 * */
public class AWSUtil {

    private Context mContext;
    private AmazonS3Client mS3Client;
    private AWSCredentialsProvider mCredentialProvider;
    private CognitoCachingCredentialsProvider mCognitoCredentialsProvider;
    private TransferUtility mTransferUtility;

    public static final String IMAGE_NAME_END = "rimg";

    private static final Regions COGNITO_REGION = Regions.EU_WEST_2;
    private static final String COGNITO_IDENTITY_POOL_ID = "SET ME";

    public AWSUtil(Context context) {
        mContext = context;
    }

    @SuppressWarnings("All")
    private AWSCredentialsProvider getCredentialProvider(Context context) {
        if (mCredentialProvider == null) {
            final CountDownLatch latch = new CountDownLatch(1);
            AWSMobileClient.getInstance().initialize(context, new Callback<UserStateDetails>() {
                @Override
                public void onResult(UserStateDetails result) {
                    latch.countDown();
                }

                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "onError: ", e);
                    latch.countDown();
                }
            });
            try {
                latch.await();
                mCredentialProvider = AWSMobileClient.getInstance();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return mCredentialProvider;
    }

    /** Cognito credential to allow access to the S3 bucket */
    private CognitoCachingCredentialsProvider getCredentialProvider() {
        if (mCognitoCredentialsProvider == null) {
            mCognitoCredentialsProvider = new CognitoCachingCredentialsProvider(
                    mContext,
                    COGNITO_IDENTITY_POOL_ID,
                    COGNITO_REGION
            );
        }

        return mCognitoCredentialsProvider;
    }

    /** Provides access to the AWS S3 Bucket*/
    public AmazonS3Client getS3Client() {
        if (mS3Client == null) {
            mS3Client = new AmazonS3Client(
                    getCredentialProvider(),
                    Region.getRegion(COGNITO_REGION));
        }

        return mS3Client;
    }

    /** API for Uploading/Downloading content from/to AWS */
    public TransferUtility getTransferUtility() {
        if (mTransferUtility == null) {
            AmazonS3Client client = getS3Client();
            AWSConfiguration configuration = new AWSConfiguration(mContext);

            mTransferUtility = TransferUtility.builder()
                    .context(mContext)
                    .s3Client(client)
                    .awsConfiguration(configuration)
                    .build();
        }

        return mTransferUtility;
    }
}
