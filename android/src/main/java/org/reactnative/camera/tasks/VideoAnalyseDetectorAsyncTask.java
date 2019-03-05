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
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.graphics.Canvas;
import android.graphics.Color;
import org.reactnative.videoanalyse.VideoAnalyseUtil;

import java.io.File;
import java.io.FileOutputStream;
import android.os.Environment;

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

    @Override
    protected SparseArray<Recognition> doInBackground(Void... ignored){
        long time= System.currentTimeMillis();
        if (isCancelled() || mDelegate == null || mVideoAnalyse == null) {
            return null;
        }
        Log.e("Zeit","Verstrichene Zeit<vor toyuvBild>: "+ (System.currentTimeMillis()- time));
        Bitmap frame = VideoAnalyseUtil.toYuvBitmap(mImageData, mWidth, mHeight);
        Log.e("Zeit","Verstrichene Zeit<nach toyuvBild>: "+ (System.currentTimeMillis()- time));
        if (frame == null) {
            mDelegate.onVideoAnalyseTaskCompleted();
            return null;
        }
        Log.e("Zeit", "Verstrichene Zeit<vor Detection>: " + (System.currentTimeMillis() - time));
        SparseArray<Recognition> detections = mVideoAnalyse.detect(frame, mHeight, mWidth);
        Log.e("Zeit", "Verstrichene Zeit<nach Detection>: " + (System.currentTimeMillis() - time));
        return detections;


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