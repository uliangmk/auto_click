package com.wonbin.autoclick;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.annotation.SuppressLint;
import android.app.admin.DevicePolicyManager;
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
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;


public class AutoService extends AccessibilityService {

    public static final int NOTICE_INTERVAL = 40 * 1000;
    public static final String ACTION = "action";
    public static final String SHOW = "show";
    public static final String STOP = "STOP_SERVICE";
    public static final String HIDE = "hide";
    public static final String PLAY = "play";
    public static final String ADD = "ADD";

    public static final String MODE = "mode";
    public static final String TAP = "tap";
    public static final String SWIPE = "swipe";

    public static final String T_X = "T_X";
    public static final String T_Y = "T_Y";

    private FloatingView mFloatingView;
    private int mInterval;
    private int tipsInterval = 3 * 1000;
    private int tX;
    private int tY;
    private String mMode;
    private CountDownTimer timer;
    private Queue<WorkPosition> workQueue = new LinkedBlockingQueue<>();
    private PowerManager.WakeLock wl;


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
                if (workQueue.size() == 0) {
                    mFloatingView.updatePosition();
                    workQueue.offer(new WorkPosition(mFloatingView.mX, mFloatingView.mY));
                }
                startClickJob();
                break;
            case ADD:
                mFloatingView.updatePosition();
                workQueue.offer(new WorkPosition(mFloatingView.mX, mFloatingView.mY));
                Toast.makeText(getBaseContext(), "当前任务数：" + workQueue.size(), Toast.LENGTH_SHORT).show();
                break;
            case STOP:
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
        if (workQueue == null) {
            needOpenPower(false);
            return;
        }
        WorkPosition currentPosition = workQueue.poll();
        if (currentPosition == null) {
            needOpenPower(false);
            return;
        }
        mFloatingView.setFloatPosition(currentPosition);
        closeTimer();
        timer = new CountDownTimer(mInterval, tipsInterval) {
            @Override
            public void onTick(long millisUntilFinished) {
                Toast.makeText(getBaseContext(), "还有" + millisUntilFinished / 1000 + "秒", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFinish() {
                if (SWIPE.equals(mMode)) {
                    playSwipe();
                } else {
                    playTap();
                }
                startClickJob();
            }
        }.start();
    }

    private void closeTimer() {
        if (timer == null) {
            return;
        }
        timer.cancel();
    }

    private void playTap() {
        try {
            mFloatingView.updatePosition();
            //必须减1像素不然点击到了自己window
            int x = mFloatingView.mX - 1;
            int y = mFloatingView.mY - 1;
            Log.i("ulog", " 点击位置-- " + x + " " + y);
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
                    Toast.makeText(getBaseContext(), "点击中断", Toast.LENGTH_SHORT).show();
                    super.onCancelled(gestureDescription);
                }
            }, null);
        } catch (Exception e) {
            Toast.makeText(getBaseContext(), "异常，位置不对", Toast.LENGTH_SHORT).show();
        }
    }

    private void playSwipe() {
        try {
            mFloatingView.updatePosition();
            int fromX = mFloatingView.mX - 1;
            int fromY = mFloatingView.mY - 1;
            int toX = mFloatingView.mX - tX - 1;
            int toY = mFloatingView.mY - tY - 1;

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
                    needOpenPower(true);
                    Toast.makeText(getBaseContext(), "收到任务", Toast.LENGTH_SHORT).show();
                    dealPositionChange(content);
                    mInterval = NOTICE_INTERVAL;
                    tipsInterval = 1000;
                    if (workQueue.size() == 0) {
                        mFloatingView.updatePosition();
                        workQueue.offer(new WorkPosition(mFloatingView.mX, mFloatingView.mY));
                    }
                    startClickJob();
                }
            }
        }
    }

    private void dealPositionChange(String content) {
        try {
            int indexX = content.indexOf("%");
            String sx = content.substring(indexX + 1, indexX + 3);
            int x = Integer.parseInt(sx);
            String left = content.substring(indexX + 1);
            int indexY = left.indexOf("%");
            String sy = left.substring(indexY + 1, indexY + 3);
            int y = Integer.parseInt(sy);

            int screenWidth = mFloatingView.getScreenWidth();
            int screenHeight = mFloatingView.getScreenHeight();
            int rx = screenWidth * x / 100;
            int ry = screenHeight * y / 100;

            WorkPosition currentPosition = new WorkPosition(rx, ry);
            mFloatingView.setFloatPosition(currentPosition);
            workQueue.offer(new WorkPosition(rx, ry));
        } catch (Exception e) {
            Toast.makeText(getBaseContext(), "异常，配置坐标不对", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 是否需要锁屏打开屏幕
     */
    @SuppressLint("InvalidWakeLockTag")
    private void needOpenPower(boolean open) {
        try {
            if (wl == null) {
                PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
                wl = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_DIM_WAKE_LOCK, "tag");
            }
            if (open) {
                wl.acquire();
            } else {
                if (wl.isHeld()) {
                    wl.release();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onInterrupt() {

    }

    public class WorkPosition {
        public int workX;
        public int workY;

        public WorkPosition(int x, int y) {
            workX = x;
            workY = y;
        }
    }

}
