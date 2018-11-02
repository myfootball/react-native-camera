package org.reactnative.camera.tasks;

import android.util.SparseArray;
import org.reactnative.frame.RNFrame;
import org.reactnative.frame.RNFrameFactory;
import org.reactnative.videoanalyse.RNVideoAnalyse;
import org.reactnative.videoanalyse.Classifier.Recognition;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class VideoAnalyseDetectorAsyncTask extends android.os.AsyncTask<Void, Void, SparseArray<Recognition>> {
    private byte[] mImageData;
    private int mWidth;
    private int mHeight;
    private int mRotation;

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

    @Override
    protected SparseArray<Recognition> doInBackground(Void... ignored){
        if (isCancelled() || mDelegate == null || mVideoAnalyse == null) {
            return null;
        }
        Bitmap frame = convertCompressedByteArrayToBitmap(mImageData);
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