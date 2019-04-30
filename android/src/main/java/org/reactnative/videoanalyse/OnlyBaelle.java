package org.reactnative.videoanalyse;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.IOException;

public class OnlyBaelle extends Classifier {
    public OnlyBaelle(Context context) throws IOException {
        super(context.getAssets());
    }

    @Override
    public int getImageSizeX() {
        return 300;
    }

    @Override
    public int getImageSizeY() {
        return 300;
    }

    @Override
    protected int getNumBytesChannels() {
        return 4;
    }

    @Override
    public String getModelPath() {
        return "MobileV2Baelle_12_04_19.tflite";
    }

    @Override
    protected String getLabelPath() {
        return "baellelabel.txt";
    }

    @Override
    protected void runInference() {
        Object[] inputArray = {imgData};
        tflite.runForMultipleInputsOutputs(inputArray, outputMap);
    }

    @Override
    protected void addPixelValue(int pixelValue) {
        imgData.putFloat(((pixelValue >> 16) & 0xFF) / 255.f);
        imgData.putFloat(((pixelValue >> 8) & 0xFF) / 255.f);
        imgData.putFloat((pixelValue & 0xFF) / 255.f);
    }
}
