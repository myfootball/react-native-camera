package org.reactnative.videoanalyse;

import java.io.ByteArrayOutputStream;
import android.graphics.YuvImage;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.IOException;
import android.graphics.Rect;
import android.graphics.ImageFormat;
import java.io.ByteArrayInputStream;

public class VideoAnalyseUtil {
    public static Bitmap convertCompressedByteArrayToBitmap(byte[] src){
        return BitmapFactory.decodeByteArray(src, 0, src.length);
    }

    public static Bitmap toBitmap(byte[] data) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
            Bitmap photo = BitmapFactory.decodeStream(inputStream);
            inputStream.close();
            return photo;
        } catch (IOException e) {
            throw new IllegalStateException("Will not happen", e);
        }
    }

    public static Bitmap toYuvBitmap(byte[] data, int mWidth, int mHeight){
        YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21, mWidth, mHeight, null);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, mWidth, mHeight), 100, os);
        byte[] jpegByteArray = os.toByteArray();
        Bitmap bitmap = BitmapFactory.decodeByteArray(jpegByteArray, 0, jpegByteArray.length);
        return Bitmap.createScaledBitmap(bitmap,300,300,false);
    }
}