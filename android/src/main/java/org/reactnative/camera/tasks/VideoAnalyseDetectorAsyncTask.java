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
    public static int counter = 0;
    public static boolean takePicture = true;
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

    public void saveBitmap(Bitmap bitmap, String name){
        long time= System.currentTimeMillis();
        if(bitmap!=null){
            String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Testfolder";
            File dir = new File(path);
            if(!dir.exists())
                dir.mkdirs();
            File file = new File(dir,name);
            try {
                FileOutputStream outputStream = null;
                try {
                    outputStream = new FileOutputStream(file);

                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                } catch (Exception e) {
                    Log.e("Error","found an error at save picture");
                    Log.e("Error",e.toString());
                    e.printStackTrace();
                } finally {
                    try {
                        if (outputStream != null) {
                            outputStream.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Log.e("Error","Zeit für Bild speichern: "+ (System.currentTimeMillis()- time));
    }

    @Override
    protected SparseArray<Recognition> doInBackground(Void... ignored){
        long time= System.currentTimeMillis();
        Log.e("Error Benny","what ist happening here"+isCancelled()+ " " + mDelegate + " " + mVideoAnalyse);
        if (isCancelled() || mDelegate == null || mVideoAnalyse == null) {
            return null;
        }
        //Log.e("Error","width and height"+mWidth+" "+mHeight);
        //Log.e("Error","byte image: "+ mImageData);
        Log.e("Error","Verstrichene Zeit<vor toyuvBild>: "+ (System.currentTimeMillis()- time));
        Bitmap frame = VideoAnalyseUtil.toYuvBitmap(mImageData, mWidth, mHeight);
        Log.e("Error","Verstrichene Zeit<nach toyuvBild>: "+ (System.currentTimeMillis()- time));
        if (frame == null) {
            mDelegate.onVideoAnalyseTaskCompleted();
            return null;
        }
        if(true){
//            try {
                Log.e("Error", "Verstrichene Zeit<vor Detection>: " + (System.currentTimeMillis() - time));
                SparseArray<Recognition> detections = mVideoAnalyse.detect(frame, mHeight, mWidth);
                Log.e("Error", "Verstrichene Zeit<nach Detection>: " + (System.currentTimeMillis() - time));
                return detections;
 //           } catch (Exception e){
 //               Log.e("Error", "Detections failed errormessage: " + e);
 //               return new SparseArray<Recognition>();
//            }
        } else {
            SparseArray<Recognition> detections = mVideoAnalyse.detect(frame, mHeight, mWidth);

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