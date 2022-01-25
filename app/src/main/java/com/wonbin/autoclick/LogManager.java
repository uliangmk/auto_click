package com.wonbin.autoclick;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.WindowManager;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @Author： ZhangYuLiang
 * @description：
 */
public class LogManager {
    private Queue<String> queue;
    private static LogManager INSTANCE;
    private Thread thread;
    public static final String ADDRESS_FILE = "auto_click_log.txt";

    public static LogManager getInstance() {
        if (INSTANCE == null) {
            synchronized (LogManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new LogManager();
                }
                return INSTANCE;
            }
        }
        return INSTANCE;
    }

    public LogManager() {
        appDir = Environment.getExternalStorageDirectory();
        addressTxt = new File(appDir, ADDRESS_FILE);

        queue = new LinkedBlockingQueue();
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    if (queue == null) {
                        Log.e("ulog"," queue == null"+" ");
                        return;
                    }
                    String msg = queue.poll();
                    if (TextUtils.isEmpty(msg)) {
                        Log.e("ulog"," isEmpty"+" ");
                        continue;
                    }
                    saveLogTxt(msg);
                }
            }
        });
        thread.start();
    }


    public boolean logMsg(String msg) {
        if (queue == null) {
            return false;
        }
        queue.offer(msg);
        return true;
    }


    private File appDir;
    private File addressTxt;

    public void saveLogTxt(String msg) {
        try {
            if (!addressTxt.exists()) {
                addressTxt.createNewFile();
            }
            FileWriter fw = new FileWriter(addressTxt, true);
            fw.write(msg + "\r\n");
            fw.close();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("ulog"," 写文件-- Exception"+" ");
        }
    }



}
