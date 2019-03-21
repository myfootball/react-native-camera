package org.reactnative.videoanalyse;

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

import org.tensorflow.lite.Delegate;

public class VideoAnalyseDetector implements Classifier{
    private int inputSize;
    private Vector<String> labels = new Vector<String>();
    private Interpreter tfLite;
    private int[] intValues;
    private float[][][] outputLocations;
    private float[][] outputClasses;
    private float[][] outputScores;
    private float[] numDetections;
    private static final int NUM_THREADS = 1;
    private static final int NUM_DETECTIONS = 10;
    private ByteBuffer imgData;
    int labelOffset = 1;
    private static final float IMAGE_MEAN = 128.0f;
    private static final float IMAGE_STD = 128.0f;
    private boolean isGPU = false;
    private boolean isInterpreterRecreated = false;

    private MappedByteBuffer loadedModel = null;

    private boolean quant = false;


    private Delegate gpuDelegate = null;
    private Interpreter.Options tfoptions =  new Interpreter.Options();


    public VideoAnalyseDetector (){
        tfoptions = new Interpreter.Options();
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
            if(d.tfoptions == null){
                d.tfoptions = new Interpreter.Options();
            }
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
            d.loadedModel = loadModelFile(assetManager, modelFilename);
            //d.tfoptions.setNumThreads(4);
            d.tfLite = new Interpreter(d.loadedModel,d.tfoptions);//,options);
            //d.tfLite.setUseNNAPI(false); // Error with inception net
            d.tfLite.setNumThreads(4);
        } catch (Exception e) {
            Log.e("Error","found an error");
            throw new RuntimeException(e);
        }

        int numBytesPerChannel;
        if (d.quant) {
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
        return d;
    }

    public boolean isOperational () {
        return true;
    }

    public SparseArray<Recognition> detect(final Bitmap bitmap, int mHeigth, int mWidth){
        long time= System.currentTimeMillis();
        int lastScanline = (0+(bitmap.getHeight() - 1) * bitmap.getWidth());
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        imgData.rewind();
        for (int i = 0; i < inputSize; ++i){
            for (int j = 0; j < inputSize; ++j){
                int pixelValue = intValues[i*inputSize+j];
                if (quant) {
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
        Log.e("##Error","Verstrichene Zeit in detect<irgendwas mit Pixeln>: "+ (System.currentTimeMillis()- time));
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
        Log.e("GPU","gpu "+isGPU + " " + isInterpreterRecreated);
        if(isGPU && !isInterpreterRecreated){
            isInterpreterRecreated = true;
            setGPUProcess();
            Log.e("##Error","Verstrichene Zeit in detect<Direkt vor recreate Interpreter for GPU>: "+ (System.currentTimeMillis()- time));
            recreateInterpreter();
            Log.e("##Error","Verstrichene Zeit in detect<direkt nach recreate Interpreter for GPU>: "+ (System.currentTimeMillis()- time));
        }
        Log.e("##Error","Verstrichene Zeit in detect<Direkt vor detect>: "+ (System.currentTimeMillis()- time));
        tfLite.runForMultipleInputsOutputs(inputArray, outputMap);
        Log.e("##Error","Verstrichene Zeit in detect<direkt nach detect>: "+ (System.currentTimeMillis()- time));
        SparseArray<Recognition> boxes = new SparseArray(NUM_DETECTIONS);
        int heigth = 330;
        int width = 690;
        for (int i = 0; i < NUM_DETECTIONS; ++i) {
            final RectF detection =
                    new RectF(
                            outputLocations[0][i][1] * inputSize * (width/inputSize),
                            outputLocations[0][i][0] * inputSize * (heigth/inputSize),
                            outputLocations[0][i][3] * inputSize * (width/inputSize),
                            outputLocations[0][i][2] * inputSize * (heigth/inputSize));
            if (((int) outputClasses[0][i]) < 0){

            } else{
                boxes.append(i,
                        new Recognition(
                                "" + i,
                                labels.get((int) outputClasses[0][i] + labelOffset),
                                outputScores[0][i],
                                detection));
            }
        }

        Log.e("##Error","Verstrichene Zeit in detect<am Ende>: "+ (System.currentTimeMillis()- time));
        return boxes;
    }

    private void recreateInterpreter() {

        if (tfLite != null && loadedModel != null && tfoptions != null) {
            Log.e("Error"," recreacting teflite interpreter "+ tfoptions);
            tfLite.close();
            tfLite = new Interpreter(loadedModel, tfoptions);
            tfLite.setNumThreads(4);
        } else {
            Log.e("Error"," at recreating model tflite:"+ tfLite+" loadedModel:"+loadedModel+" options:"+tfoptions);
        }
    }

    public void close(){

    }

    private void setGPUProcess(){
        if (gpuDelegate == null && GpuDelegateHelper.isGpuDelegateAvailable()) {
            gpuDelegate = GpuDelegateHelper.createGpuDelegate();
            tfoptions.addDelegate(gpuDelegate);
        }
    }

    public void setGPU(boolean gpu){
        Log.e("GPU","Set GPU "+ gpu);
        isGPU = gpu;
        isInterpreterRecreated = !gpu;

    }

    public void setNNAPI(boolean nnapi){
        Log.e("NNAPI", "setNNAPI "+ nnapi);
        tfoptions.setUseNNAPI(nnapi);
        recreateInterpreter();
    }


    public String getStatString() {
        return "";
    }

    public List<Recognition> recognizeImage(Bitmap bitmap){
        return null;
    }

}
