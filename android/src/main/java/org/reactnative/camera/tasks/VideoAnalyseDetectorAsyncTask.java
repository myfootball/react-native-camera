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

    @Override
    protected SparseArray<Recognition> doInBackground(Void... ignored){
        long time= System.currentTimeMillis();
        if (isCancelled() || mDelegate == null || mVideoAnalyse == null) {
            return null;
        }
        Log.e("Error","byte image: "+ mImageData);
        Bitmap frame = VideoAnalyseUtil.toYuvBitmap(mImageData, mWidth, mHeight);
        Log.e("Error","Vertsirchene Zeit<Bild>: "+ (System.currentTimeMillis()- time));
        Log.e("Error"," frame "+ frame);
        if (frame == null) {
            mDelegate.onVideoAnalyseTaskCompleted();
            return null;
        }
        if(true){
            SparseArray<Recognition> detections =  mVideoAnalyse.detect(frame);
            Log.e("Error","Vertsirchene Zeit<Detection>: "+ (System.currentTimeMillis()- time));
            return detections;
        } else {
            SparseArray<Recognition> detections = mVideoAnalyse.detect(frame);

            Bitmap copyFrame = Bitmap.createBitmap(frame);
            final Canvas canvas = new Canvas(copyFrame);
            final Paint paint = new Paint();
            paint.setColor(Color.RED);
            paint.setStyle(Style.STROKE);
            paint.setStrokeWidth(2.0f);

            for (int i = 0; i < detections.size(); ++i){
                Recognition recognition = detections.valueAt(i);
                final RectF location = recognition.getLocation();
                if (location != null) {
                    canvas.drawRect(location,paint);
                }
            }

            return detections;
        }
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