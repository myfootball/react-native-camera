package org.reactnative.videoanalyse;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.os.SystemClock;
import android.util.Log;
import android.util.SparseArray;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import org.tensorflow.lite.Delegate;
import org.tensorflow.lite.Interpreter;

public abstract class Classifier {

    private final Interpreter.Options tfliteOptions = new Interpreter.Options();
    private MappedByteBuffer tfliteModel;
    protected Interpreter tflite;
    private List<String> labelList;
    protected ByteBuffer imgData;
    private Delegate gpuDelegate = null;
    private float[][][] outputLocations;
    private float[][] outputClasses;
    private float[][] outputScores;
    private float[] numDetections;
    private static final int NUM_DETECTIONS = 10;
    Map<Integer, Object> outputMap;
    private int[] intValues = new int[getImageSizeX() * getImageSizeY()];
    private int labelOffset = 1;
    private float minDetection = 0.5f;
    private String TAG = "Classifier";

    Classifier(final AssetManager assetManager) throws IOException {
        tfliteModel = loadModelFile(assetManager);
        tflite = new Interpreter(tfliteModel, tfliteOptions);
        labelList = loadLabelList(assetManager);
        imgData =
                ByteBuffer.allocateDirect(
                        1*getImageSizeX()*getImageSizeY()*3*getNumBytesChannels());
        imgData.order(ByteOrder.nativeOrder());

        outputLocations = new float[1][NUM_DETECTIONS][4];
        outputClasses = new float[1][NUM_DETECTIONS];
        outputScores = new float[1][NUM_DETECTIONS];
        numDetections = new float[1];

        outputMap = new HashMap<>();
        outputMap.put(0, outputLocations);
        outputMap.put(1, outputClasses);
        outputMap.put(2, outputScores);
        outputMap.put(3, numDetections);
    }

    public SparseArray<Recognition> analyseFrame(Bitmap bitmap){
        if (tflite == null){
            return null;
        }
        long convertStart = SystemClock.uptimeMillis();
        convertBitmapToByteBuffer(bitmap);
        long infernceStart = SystemClock.uptimeMillis();
        runInference();
        long outputStart = SystemClock.uptimeMillis();
        printOutput();

        long allEndTime = SystemClock.uptimeMillis();
        Log.e(TAG, "Timecost to convert Bitmap: " + Long.toString(infernceStart - convertStart));
        Log.e(TAG, "Timecost to runinfernce: " + Long.toString(outputStart - infernceStart));
        Log.e(TAG, "Timecost to print: " + Long.toString(allEndTime - outputStart));

        return prepareResults();
    }

    private void convertBitmapToByteBuffer(Bitmap bitmap) {
        if (imgData == null) {
            return;
        }
        imgData.rewind();
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        int pixel = 0;
        long startTime = SystemClock.uptimeMillis();
        for (int i = 0; i < getImageSizeX(); ++i) {
            for (int j = 0; j < getImageSizeY(); ++j) {
                final int val = intValues[pixel++];
                addPixelValue(val);
            }
        }
        long endTime = SystemClock.uptimeMillis();
        Log.d(TAG, "Timecost to put values into ByteBuffer: " + Long.toString(endTime - startTime));
    }

    private MappedByteBuffer loadModelFile(final AssetManager assetManager) throws IOException {
        AssetFileDescriptor fileDescriptor = assetManager.openFd(getModelPath());
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private List<String> loadLabelList(final AssetManager assetManager) throws IOException {
        List<String> labelList = new ArrayList<String>();
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(assetManager.open(getLabelPath())));
        String line;
        while ((line = reader.readLine()) != null) {
            labelList.add(line);
        }
        reader.close();
        return labelList;
    }

    private void recreateInterpreter() {
        if (tflite != null) {
            tflite.close();
            tflite = new Interpreter(tfliteModel, tfliteOptions);
        }
    }

    private void printOutput(){
        for (int i = 0; i < NUM_DETECTIONS; ++i) {
            if (outputScores[0][i] >= minDetection) {
                Log.e(TAG,  labelList.get((int) outputClasses[0][i] + 1) + " " + (outputScores[0][i] * 100));
            }
        }
    }

    private SparseArray<Recognition> prepareResults(){
        SparseArray<Recognition> boxes = new SparseArray(NUM_DETECTIONS);
        int heigth = 330;
        int width = 690;
        for (int i = 0; i < NUM_DETECTIONS; ++i) {
            if (((int) outputClasses[0][i]) < 0 && outputScores[0][i] < minDetection){

            } else{
            final RectF detection =
                    new RectF(
                            outputLocations[0][i][1] * getImageSizeX() * (width/getImageSizeX()),
                            outputLocations[0][i][0] * getImageSizeY() * (heigth/getImageSizeX()),
                            outputLocations[0][i][3] * getImageSizeX() * (width/getImageSizeX()),
                            outputLocations[0][i][2] * getImageSizeX() * (heigth/getImageSizeX()));
                boxes.append(i,
                        new Recognition(
                                "" + i,
                                labelList.get((int) outputClasses[0][i] + labelOffset),
                                outputScores[0][i],
                                detection));
            }
        }
        return boxes;
    }

    public void useGpu() {
        if (gpuDelegate == null && GpuDelegateHelper.isGpuDelegateAvailable()) {
            gpuDelegate = GpuDelegateHelper.createGpuDelegate();
            tfliteOptions.addDelegate(gpuDelegate);
            recreateInterpreter();
        }
    }

    public void useNNAPI() {
        tfliteOptions.setUseNNAPI(true);
        recreateInterpreter();
    }

    public void setNumThreads(int numThreads) {
        tfliteOptions.setNumThreads(numThreads);
        recreateInterpreter();
    }

    public void close() {
        tflite.close();
        tflite = null;
        tfliteModel = null;
    }

    public abstract int getImageSizeX();
    public abstract int getImageSizeY();
    protected abstract int getNumBytesChannels();
    protected abstract String getModelPath();
    protected abstract String getLabelPath();
    protected abstract void runInference();
    protected abstract void addPixelValue(int pixelValue);

    public class Recognition {
        private final String id;
        private final String title;
        private final Float confidence;
        private RectF location;

        public Recognition(
                final String id, final String title, final Float confidence, final RectF location) {
            this.id = id;
            this.title = title;
            this.confidence = confidence;
            this.location = location;
        }

        public String getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public Float getConfidence() {
            return confidence;
        }

        public RectF getLocation() {
            return new RectF(location);
        }

        public void setLocation(RectF location) {
            this.location = location;
        }

        @Override
        public String toString() {
            String resultString = "";
            if (id != null) {
                resultString += "[" + id + "] ";
            }

            if (title != null) {
                resultString += title + " ";
            }

            if (confidence != null) {
                resultString += String.format("(%.1f%%) ", confidence * 100.0f);
            }

            if (location != null) {
                resultString += location + " ";
            }

            return resultString.trim();
        }
    }
}