package org.reactnative.videoanalyse;

import android.content.Context;
import org.reactnative.videoanalyse.Classifier;
import org.reactnative.videoanalyse.Classifier.Recognition;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import java.io.BufferedReader;
import java.io.FileInputStream;
import org.tensorflow.lite.Interpreter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Vector;
import java.nio.channels.FileChannel;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.util.List;
import android.graphics.Bitmap;
import android.util.SparseArray;
import android.util.Log;
import android.graphics.RectF;
import java.util.HashMap;
import java.util.Map;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class VideoAnalyseDetector implements Classifier{
    private int inputSize;
    private Vector<String> labels = new Vector<String>();
    private Interpreter tfLite;
    private int[] intValues;
    private float[][][] outputLocations;
    private float[][] outputClasses;
    private float[][] outputScores;
    private float[] numDetections;
    private static final int NUM_THREADS = 4;
    private static final int NUM_DETECTIONS = 10;
    private ByteBuffer imgData;
    int labelOffset = 1;
    private static final float IMAGE_MEAN = 128.0f;
    private static final float IMAGE_STD = 128.0f;

    public VideoAnalyseDetector (){

    }

    /** Memory-map the model file in Assets. */
    private static MappedByteBuffer loadModelFile(AssetManager assets, String modelFilename)
            throws IOException {
        AssetFileDescriptor fileDescriptor = assets.openFd(modelFilename);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    public static Classifier create(final String labelNames, final int inputSize,
                                    final AssetManager assetManager,
                                    final String modelFilename){
        final VideoAnalyseDetector d = new VideoAnalyseDetector();
        InputStream labelsInput = null;
        try {
            String actualFilename = labelNames;
            labelsInput = assetManager.open(actualFilename);
            BufferedReader br = null;
            br = new BufferedReader(new InputStreamReader(labelsInput));
            String line;
            while ((line = br.readLine()) != null) {
                d.labels.add(line);
            }
            br.close();
            d.inputSize = inputSize;
            d.tfLite = new Interpreter(loadModelFile(assetManager, modelFilename));
            d.tfLite.setUseNNAPI(true);
            //d.tfLite.setNumThreads(2);
        } catch (Exception e) {
            Log.e("Error","found an error");
            throw new RuntimeException(e);
        }

        int numBytesPerChannel;
        if (false) {
            numBytesPerChannel = 1; // Quantized
        } else {
            numBytesPerChannel = 4; // Floating point
        }

        d.imgData = ByteBuffer.allocateDirect(1 * d.inputSize * d.inputSize * 3 * numBytesPerChannel);
        d.imgData.order(ByteOrder.nativeOrder());
        d.intValues = new int[d.inputSize * d.inputSize];

        d.tfLite.setNumThreads(NUM_THREADS);
        d.outputLocations = new float[1][NUM_DETECTIONS][4];
        d.outputClasses = new float[1][NUM_DETECTIONS];
        d.outputScores = new float[1][NUM_DETECTIONS];
        d.numDetections = new float[1];

        Log.e("Error","Created something with loaded model");
        return d;
    }

    public boolean isOperational () {
        return true;
    }

    public SparseArray<Recognition> detect(final Bitmap bitmap){
        long time= System.currentTimeMillis();
        int lastScanline = (0+(bitmap.getHeight() - 1) * bitmap.getWidth());
        Log.e("Error","values                        =  "+ lastScanline + " w: " + bitmap.getWidth() + " l: " + intValues.length);
        Log.e("Error","offset <0                     =  " + false);
        Log.e("Error","offset + width > length       =  " + ((0+bitmap.getWidth()) > intValues.length));
        Log.e("Error","lastscnaline < 0              =  " + (lastScanline < 0));
        Log.e("Error","lastscanline + width > length =  " + (lastScanline + bitmap.getWidth() > intValues.length));
        //               pixel   offset   stride          x  y      width             height
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        imgData.rewind();
        for (int i = 0; i < inputSize; ++i){
            for (int j = 0; j < inputSize; ++j){
                int pixelValue = intValues[i*inputSize+j];
                if (false) {
                    // Quantized model
                    imgData.put((byte) ((pixelValue >> 16) & 0xFF));
                    imgData.put((byte) ((pixelValue >> 8) & 0xFF));
                    imgData.put((byte) (pixelValue & 0xFF));
                } else { // Float model
                    imgData.putFloat((((pixelValue >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                    imgData.putFloat((((pixelValue >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                    imgData.putFloat(((pixelValue & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                }
            }
        }
        Log.e("Error","Vertsirchene Zeit in detect<irgendwas mit Pixeln>: "+ (System.currentTimeMillis()- time));
        outputLocations = new float[1][NUM_DETECTIONS][4];
        outputClasses = new float[1][NUM_DETECTIONS];
        outputScores = new float[1][NUM_DETECTIONS];
        numDetections = new float[1];

        Object[] inputArray = {imgData};
        Map<Integer, Object> outputMap = new HashMap<>();
        outputMap.put(0, outputLocations);
        outputMap.put(1, outputClasses);
        outputMap.put(2, outputScores);
        outputMap.put(3, numDetections);
        Log.e("Error","Vertsirchene Zeit in detect<Direkt vor detect>: "+ (System.currentTimeMillis()- time));
        tfLite.runForMultipleInputsOutputs(inputArray, outputMap);
        Log.e("Error","Vertsirchene Zeit in detect<direkt nach detect>: "+ (System.currentTimeMillis()- time));
        SparseArray<Recognition> boxes = new SparseArray(NUM_DETECTIONS);
        for (int i = 0; i < NUM_DETECTIONS; ++i) {
            final RectF detection =
                    new RectF(
                            outputLocations[0][i][1] * inputSize,
                            outputLocations[0][i][0] * inputSize,
                            outputLocations[0][i][3] * inputSize,
                            outputLocations[0][i][2] * inputSize);
            Log.e("Error","number = "+ i );
            Log.e("error"," outputclasses = " + ((int) outputClasses[0][i]) );
            Log.e("Error"," labels "+ labels.get((int) outputClasses[0][i] + labelOffset));
            boxes.append(i,
                    new Recognition(
                            "" + i,
                            labels.get((int) outputClasses[0][i] + labelOffset),
                            outputScores[0][i],
                            detection));
        }

        Log.e("Error","Vertsirchene Zeit in detect<am Ende>: "+ (System.currentTimeMillis()- time));
        return boxes;
    }

    public void close(){

    }

    public String getStatString() {
        return "";
    }

    public List<Recognition> recognizeImage(Bitmap bitmap){
        return null;
    }

}
