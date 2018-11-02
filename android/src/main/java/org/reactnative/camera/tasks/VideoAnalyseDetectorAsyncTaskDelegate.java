package org.reactnative.camera.tasks;

import org.reactnative.videoanalyse.RNVideoAnalyse;
import android.util.SparseArray;
import org.reactnative.videoanalyse.Classifier.Recognition;

public interface VideoAnalyseDetectorAsyncTaskDelegate {
    void onVideoAnalyseDetected(SparseArray<Recognition> recognition, int mWidth, int mHeight, int mRotation);
    void onVideoAnalyseError(RNVideoAnalyse videoAnalyse);
    void onVideoAnalyseTaskCompleted();
}