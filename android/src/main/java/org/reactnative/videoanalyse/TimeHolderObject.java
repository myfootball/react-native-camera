package org.reactnative.videoanalyse;


import android.os.SystemClock;

public class TimeHolderObject {
    private long inferenceTime;
    private long pictureLoadingTime;
    private long fullTime;
    private long creationMoment;
    private long bitmaploadingtime;
    private long avergage;
    private int objectsDetected;
    private int runs;
    private long preInfernenceStart;
    private long preInfernence;
    private long postInferenceStart;
    private long postInference;

    public TimeHolderObject setPreInfernenceStart() {
        this.preInfernenceStart = SystemClock.uptimeMillis();
        return this;
    }

    public TimeHolderObject setPostInference() {
        this.postInference = SystemClock.uptimeMillis() - postInferenceStart;
        return this;
    }


    public TimeHolderObject setPreInfernence() {
        this.preInfernence = SystemClock.uptimeMillis() - preInfernenceStart;

        return this;
    }

    public TimeHolderObject setPostInferenceStart() {
        this.postInferenceStart = SystemClock.uptimeMillis();

        return this;
    }


    public TimeHolderObject setRuns(int runs) {
        this.runs = runs;
        return this;
    }

    public TimeHolderObject incObjectsDetected() {
        this.objectsDetected +=1;
        return this;
    }


    public TimeHolderObject setAvergage(long avergage) {
        this.avergage = avergage;
        return this;
    }


    public TimeHolderObject setBitmaploadingtime(long bitmaploadingtime) {
        this.bitmaploadingtime = bitmaploadingtime;
        return this;
    }


    public TimeHolderObject setInferenceTime(long inferenceTime) {
        this.inferenceTime = inferenceTime;
        return this;
    }

    public TimeHolderObject setPictureLoadingTime(long pictureLoadingTime) {
        this.pictureLoadingTime = pictureLoadingTime;
        return this;
    }

    public TimeHolderObject setFullTime(long fullTime) {
        this.fullTime = fullTime;
        return this;
    }

    public TimeHolderObject(){
        creationMoment = System.currentTimeMillis();
        objectsDetected = 0;
    }

    @Override
    public String toString() {
        // inferenceTime, convertBitmapToBytebuffer, full runtime without bitmaploading time, Timestamp, bitmaploadingtime, average
        return inferenceTime+","+preInfernence+","+postInference+","+pictureLoadingTime+","+fullTime+","+creationMoment+","+bitmaploadingtime+","+avergage+","+objectsDetected+","+runs;
    }
}
