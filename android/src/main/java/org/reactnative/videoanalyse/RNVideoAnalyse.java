package org.reactnative.videoanalyse;

import org.tensorflow.lite.Interpreter;
import org.reactnative.videoanalyse.VideoAnalyseDetector;
import android.util.SparseArray;
import org.reactnative.frame.RNFrame;
import org.reactnative.camera.utils.ImageDimensions;
import android.content.Context;
import android.util.Log;
import org.reactnative.videoanalyse.Classifier.Recognition;
import android.graphics.Bitmap;

public class RNVideoAnalyse{

    private VideoAnalyseDetector mVideoAnaylseDetector;
    private ImageDimensions mPreviousDimensions;
    private VideoAnalyseDetector mDecoder;
    private Context mContext;

    private static final int TF_OD_API_INPUT_SIZE = 300;
    private static final String TF_OD_API_MODEL_FILE = "detect.tflite";
    private static final String TF_OD_API_LABELS_FILE = "labels.txt";

    public RNVideoAnalyse(Context context){
        mContext = context;
        mDecoder = new VideoAnalyseDetector();
        if (mVideoAnaylseDetector == null){
            createVideoAnalyseDetector();
        }
        Log.e("Error","created detector");
    }

    public boolean isOperational() {
        if (mVideoAnaylseDetector == null){
            createVideoAnalyseDetector();
        }

        return mVideoAnaylseDetector.isOperational();
    }

    public SparseArray<Recognition> detect(Bitmap frame) {
        if (mVideoAnaylseDetector == null){
            createVideoAnalyseDetector();
            //mPreviousDimensions = frame.getDimensions();
        }

        return mVideoAnaylseDetector.detect(frame);
    }

    private void createVideoAnalyseDetector(){
        mVideoAnaylseDetector = (VideoAnalyseDetector) mDecoder.create(TF_OD_API_LABELS_FILE,TF_OD_API_INPUT_SIZE,mContext.getAssets(),TF_OD_API_MODEL_FILE);
    }


    public void release () {
        mVideoAnaylseDetector.close();
    }
}