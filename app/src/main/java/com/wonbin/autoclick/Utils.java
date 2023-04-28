package com.wonbin.autoclick;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @Author： ZhangYuLiang
 * @description：
 */
public class Utils {
    private static final String MODEL_LOG = "MM-dd HH:mm:ss";
    private static final String MODEL_DATE = "yyyy-MM-dd HH:mm:ss";

    //Android6.0之后系统对权限的管理更加严格了，不但要在AndroidManifest中添加，还要在应用运行的时候动态申请
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static final String[] PERMISSION_STORAGE = {"android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE"};

    public static void verifyStoragePermissions(Activity activity) {
        try {
            int permission = ActivityCompat.checkSelfPermission(activity, "android.permission.WRITE_EXTERNAL_STORAGE");
            if (permission != PackageManager.PERMISSION_GRANTED) {//判断是否已经授予权限
                ActivityCompat.requestPermissions(activity, PERMISSION_STORAGE, REQUEST_EXTERNAL_STORAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("SimpleDateFormat")
    public static String getLogDateToString() {
        try {
            long milSecond = System.currentTimeMillis();
            Date date = new Date(milSecond);
            SimpleDateFormat format = new SimpleDateFormat(MODEL_LOG);
            return "[" + format.format(date) + "]";
        } catch (Exception e) {
            return "[error time]";
        }
    }

    @SuppressLint("SimpleDateFormat")
    public static String getDateToString() {
        try {
            long milSecond = System.currentTimeMillis();
            Date date = new Date(milSecond);
            SimpleDateFormat format = new SimpleDateFormat(MODEL_DATE);
            return format.format(date);
        } catch (Exception e) {
            return "";
        }
    }

    @SuppressLint("SimpleDateFormat")
    public static long getStringToDate(String dateString) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(MODEL_DATE);
        Date date = new Date();
        try {
            date = dateFormat.parse(dateString);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return date.getTime();
    }

    public static long changeToTargetTime(int h, int m, int s) {
        String date1 = getDateToString();
        String[] dates = date1.split(" ");
        String date = dates[0] + " " + h + ":" + m + ":" + s;
        long timeMillis = getStringToDate(date);
        return timeMillis / 1000;
    }

}
