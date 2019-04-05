package org.reactnative.videoanalyse;

import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileHelper {
    final static String fileName = "Data.csv";
    final static String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/MA_Tests/" ;
    final static String TAG = FileHelper.class.getName();

    public static boolean saveToFile( String data, String modelName, int saveNumber){
        try {
            new File(path  ).mkdir();
            File file = new File(path+ modelName+saveNumber+fileName);
            if (!file.exists()) {
                file.createNewFile();
            }
            FileOutputStream fileOutputStream = new FileOutputStream(file,true);
            fileOutputStream.write((data + System.getProperty("line.separator")).getBytes());

            return true;
        }  catch(FileNotFoundException ex) {
            Log.d(TAG, ex.getMessage());
        }  catch(IOException ex) {
            Log.d(TAG, ex.getMessage());
        }
        return  false;

    }
}
