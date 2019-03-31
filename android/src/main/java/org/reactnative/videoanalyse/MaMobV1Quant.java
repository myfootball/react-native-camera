package org.reactnative.videoanalyse;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.IOException;

public class MaMobV1Quant extends Classifier {
    public MaMobV1Quant(Context context) throws IOException {
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
        return 1;
    }

    @Override
    protected String getModelPath() {
        return "MA_Mob_V1_Quant.tflite";
    }

    @Override
    protected String getLabelPath() {
        return "mobilelabels.txt";
    }

    @Override
    protected void runInference() {
        Object[] inputArray = {imgData};
        tflite.runForMultipleInputsOutputs(inputArray, outputMap);
    }

    @Override
    protected void addPixelValue(int pixelValue) {
        imgData.put((byte) ((pixelValue >> 16) & 0xFF));
        imgData.put((byte) ((pixelValue >> 8) & 0xFF));
        imgData.put((byte) (pixelValue & 0xFF));
    }
}
