package com.wonbin.autoclick;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Path;
import android.os.CountDownTimer;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

import java.util.List;
import java.util.logging.Logger;


public class AutoService extends AccessibilityService {

    public static int tipsInterval = 3 * 1000;
    public static final String ACTION = "action";
    public static final String SHOW = "show";
    public static final String STOP_SERVICE = "STOP_SERVICE";
    public static final String HIDE = "hide";
    public static final String PLAY = "play";
    public static final String STOP = "stop";

    public static final String MODE = "mode";
    public static final String TAP = "tap";
    public static final String SWIPE = "swipe";

    public static final String T_X = "T_X";
    public static final String T_Y = "T_Y";

    private FloatingView mFloatingView;
    private int mInterval;
    private int mX, tX;
    private int mY, tY;
    private String mMode;
    private CountDownTimer timer;

    @Override
    public void onCreate() {
        super.onCreate();
        mFloatingView = new FloatingView(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return super.onStartCommand(intent, flags, startId);
        }
        String action = intent.getStringExtra(ACTION);
        if (TextUtils.isEmpty(action)) {
            return super.onStartCommand(intent, flags, startId);
        }
        switch (action) {
            case SHOW:
                mInterval = intent.getIntExtra("interval", 16) * 1000;
                tX = intent.getIntExtra(T_X, 0);
                tY = intent.getIntExtra(T_Y, 0);
                mMode = intent.getStringExtra(MODE);
                mFloatingView.show();
                break;
            case HIDE:
                mFloatingView.hide();
                closeTimer();
                break;
            case PLAY:
                startClickJob();
                break;
            case STOP:
                closeTimer();
                Toast.makeText(getBaseContext(), "已暂停", Toast.LENGTH_SHORT).show();
                break;
            case STOP_SERVICE:
                stopAutoService();
                break;
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void stopAutoService() {
        closeTimer();
        Toast.makeText(getBaseContext(), "关闭服务", Toast.LENGTH_SHORT).show();
        disableSelf();
    }

    private void startClickJob() {
        mFloatingView.updatePosition();
        mX = mFloatingView.mX;
        mY = mFloatingView.mY;
        closeTimer();
        timer = new CountDownTimer(mInterval, tipsInterval) {
            @Override
            public void onTick(long millisUntilFinished) {
                Toast.makeText(getBaseContext(), "还有" + millisUntilFinished / 1000 + "秒", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFinish() {
                if (SWIPE.equals(mMode)) {
                    playSwipe(mX, mY, mX - tX, mY - tY);
                } else {
                    playTap(mX, mY);
                }
                mFloatingView.mCurState = AutoService.STOP;
            }
        }.start();
    }

    private void closeTimer() {
        if (timer == null) {
            return;
        }
        timer.cancel();
    }

    private void playTap(final int x, final int y) {
        try {
            Path path = new Path();
            path.moveTo(x, y);
            path.lineTo(x, y);
            GestureDescription.Builder builder = new GestureDescription.Builder();
            builder.addStroke(new GestureDescription.StrokeDescription(path, 10L, 10L));
            GestureDescription gestureDescription = builder.build();
            dispatchGesture(gestureDescription, new GestureResultCallback() {
                @Override
                public void onCompleted(GestureDescription gestureDescription) {
                    super.onCompleted(gestureDescription);
                }

                @Override
                public void onCancelled(GestureDescription gestureDescription) {
                    super.onCancelled(gestureDescription);
                }
            }, null);
        } catch (Exception e) {
            Toast.makeText(getBaseContext(), "异常，位置不对", Toast.LENGTH_SHORT).show();
        }
    }

    private void playSwipe(int fromX, int fromY, int toX, int toY) {
        try {
            Path path = new Path();
            path.moveTo(fromX, fromY);
            path.lineTo(toX, toY);
            GestureDescription.Builder builder = new GestureDescription.Builder();
            builder.addStroke(new GestureDescription.StrokeDescription(path, 100L, 1000L));
            GestureDescription gestureDescription = builder.build();
            dispatchGesture(gestureDescription, new GestureResultCallback() {
                @Override
                public void onCompleted(GestureDescription gestureDescription) {
                    super.onCompleted(gestureDescription);
                }

                @Override
                public void onCancelled(GestureDescription gestureDescription) {
                    super.onCancelled(gestureDescription);
                }
            }, null);
        } catch (Exception e) {
            Toast.makeText(getBaseContext(), "异常，滑动位置不对", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        int eventType = event.getEventType();
        switch (eventType) {
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
                Log.i("ulog", " -- " + "通知栏 " + eventType);
                dealNotificationChange(event);
                break;
        }
    }

    private void dealNotificationChange(AccessibilityEvent event) {
        List<CharSequence> texts = event.getText();
        if (!texts.isEmpty()) {
            for (CharSequence text : texts) {
                String content = text.toString();
                //通知栏包括威信红包文字
                if (content.contains("elazipa") && content.contains("开")) {
                    needOpenPower();
                    Toast.makeText(getBaseContext(), "收到任务", Toast.LENGTH_SHORT).show();
                    mInterval = 50 * 1000;
                    tipsInterval = 1000;
                    startClickJob();
                }
            }
        }
    }

    /**
     * 是否需要锁屏打开屏幕
     */
    @SuppressLint("InvalidWakeLockTag")
    private void needOpenPower() {
        try {
            String tag = "";
            PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
            pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_DIM_WAKE_LOCK, tag);
            PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_DIM_WAKE_LOCK, "ulogp");
            wl.acquire();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onInterrupt() {

    }

}
