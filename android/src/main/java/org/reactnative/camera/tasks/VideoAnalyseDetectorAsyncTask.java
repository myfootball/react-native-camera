package org.reactnative.camera.tasks;

import android.util.SparseArray;
import org.reactnative.frame.RNFrame;
import org.reactnative.frame.RNFrameFactory;
import org.reactnative.videoanalyse.RNVideoAnalyse;
import org.reactnative.videoanalyse.Classifier.Recognition;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import android.graphics.YuvImage;
import android.graphics.Rect;
import android.graphics.ImageFormat;

public class VideoAnalyseDetectorAsyncTask extends android.os.AsyncTask<Void, Void, SparseArray<Recognition>> {
    private byte[] mImageData;
    private int mWidth;
    private int mHeight;
    private int mRotation;
    public static int counter = 0;

    private RNVideoAnalyse mVideoAnalyse;
    private VideoAnalyseDetectorAsyncTaskDelegate mDelegate;

    public VideoAnalyseDetectorAsyncTask (
            RNVideoAnalyse videoAnalyse,
            VideoAnalyseDetectorAsyncTaskDelegate delegate,
            byte[] imageData,
            int width,
            int height,
            int rotation
    ){
        Log.e("Error","Start Detect"+ ++counter);
        mImageData = imageData;
        mWidth = width;
        mHeight = height;
        mRotation = rotation;
        mDelegate = delegate;
        mVideoAnalyse = videoAnalyse;
    }

    public static Bitmap convertCompressedByteArrayToBitmap(byte[] src){
        return BitmapFactory.decodeByteArray(src, 0, src.length);
    }

    private static Bitmap toBitmap(byte[] data) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
            Bitmap photo = BitmapFactory.decodeStream(inputStream);
            inputStream.close();
            return photo;
        } catch (IOException e) {
            throw new IllegalStateException("Will not happen", e);
        }
    }

    private Bitmap toYuvBitmap(byte[] data){
        YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21, mWidth, mHeight, null);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, mWidth, mHeight), 100, os);
        byte[] jpegByteArray = os.toByteArray();
        Bitmap bitmap = BitmapFactory.decodeByteArray(jpegByteArray, 0, jpegByteArray.length);
        return Bitmap.createScaledBitmap(bitmap,300,300,false);
    }

    @Override
    protected SparseArray<Recognition> doInBackground(Void... ignored){
        if (isCancelled() || mDelegate == null || mVideoAnalyse == null) {
            return null;
        }
        Log.e("Error","byte image: "+ mImageData);
        Bitmap frame = toYuvBitmap(mImageData);
        Log.e("Error"," frame "+ frame);
        if (frame == null) {
            mDelegate.onVideoAnalyseTaskCompleted();
            return null;
        }
        return mVideoAnalyse.detect(frame);
    }

    @Override
    protected void onPostExecute(SparseArray<Recognition> recognition){
        super.onPostExecute(recognition);

        if (recognition == null){
            mDelegate.onVideoAnalyseError(mVideoAnalyse);
        } else {
            if (recognition.size() > 0){
                mDelegate.onVideoAnalyseDetected(recognition, mWidth, mHeight, mRotation);
            }
            mDelegate.onVideoAnalyseTaskCompleted();
        }
    }
}