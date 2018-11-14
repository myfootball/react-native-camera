package org.reactnative.camera.events;

import com.facebook.react.uimanager.events.Event;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import android.support.v4.util.Pools;
import android.util.SparseArray;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.events.Event;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import org.reactnative.camera.CameraViewManager;
import org.reactnative.videoanalyse.Classifier.Recognition;
import android.graphics.RectF;

public class VideoAnalyseDetectEvent extends Event<VideoAnalyseDetectEvent> {
    private static final Pools.SynchronizedPool<VideoAnalyseDetectEvent> EVENTS_POOL = new Pools.SynchronizedPool<>(3);

    private SparseArray<Recognition> mBoxes;
    private static long lastTime = 0;

    private VideoAnalyseDetectEvent(){

    }

    public static VideoAnalyseDetectEvent obtain(
        int viewTag,
        SparseArray<Recognition> boxes
    ){
        VideoAnalyseDetectEvent event = EVENTS_POOL.acquire();
        if (event == null){
            event = new VideoAnalyseDetectEvent();
        }
        event.init(viewTag, boxes);
        return event;
    }

    private void init(
            int viewTag,
            SparseArray<Recognition> boxes
    ) {
        super.init(viewTag);
        mBoxes = boxes;
    }

    @Override
    public short getCoalescingKey() {
        if (mBoxes.size() > Short.MAX_VALUE) {
            return Short.MAX_VALUE;
        }

        return (short) mBoxes.size();
    }

    @Override
    public String getEventName() {
        return CameraViewManager.Events.EVENT_ON_BOXES_DETECTED.toString();
    }

    @Override
    public void dispatch(RCTEventEmitter rctEventEmitter) {
        rctEventEmitter.receiveEvent(getViewTag(), getEventName(), serializeEventData());
    }

    private WritableMap serializeEventData() {
        WritableArray boxList = Arguments.createArray();

        for (int i = 0; i < mBoxes.size(); ++i){
            Recognition recognition = mBoxes.valueAt(i);

            WritableMap serializedBox = Arguments.createMap();
            serializedBox.putString("id", recognition.getId());
            serializedBox.putString("title", recognition.getTitle());
            serializedBox.putDouble("confidence", recognition.getConfidence());
            serializedBox.putDouble("bottom", recognition.getLocation().bottom);
            serializedBox.putDouble("top", recognition.getLocation().top);
            serializedBox.putDouble("left", recognition.getLocation().left);
            serializedBox.putDouble("right", recognition.getLocation().right);
            if (lastTime > 0) {
                serializedBox.putString("time", String.valueOf(System.currentTimeMillis() - lastTime));
            }

            boxList.pushMap(serializedBox);
        }

        WritableMap event = Arguments.createMap();
        event.putString("type", "boxes");
        event.putArray("boxes", boxList);
        event.putInt("target", getViewTag());

        lastTime = System.currentTimeMillis();
        return event;
    }
}