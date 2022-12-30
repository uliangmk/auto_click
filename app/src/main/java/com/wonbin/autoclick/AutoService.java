package com.wonbin.autoclick;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Path;
import android.os.CountDownTimer;
import android.os.PowerManager;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;


public class AutoService extends AccessibilityService {

    public static final long NOTICE_INTERVAL = 40 * 1000;
    public static final String ACTION = "action";
    public static final String SHOW = "show";
    public static final String STOP = "STOP_SERVICE";
    public static final String HIDE = "hide";
    public static final String PLAY = "play";
    public static final String ADD = "ADD";

    public static final String MODE = "mode";
    public static final String CLICK = "click";
    public static final String SWIPE = "swipe";

    public static final String T_X = "T_X";
    public static final String T_Y = "T_Y";

    public static final String INTERVAL = "interval";

    private FloatingView mFloatingView;
    private long mInterval;
    private int tipsInterval = 3 * 1000;
    private int tX;
    private int tY;
    private String mMode;
    private CountDownTimer timer;
    private Queue<WorkPosition> workQueue = new LinkedBlockingQueue<>();
    private PowerManager.WakeLock wl;

    public static final String START = "预备状态:小窗显示";
    public static final String END = "结束服务";
    public static final String ADD_WORK = "添加任务：";
    public static final String PRE_WORK = "开启任务：";
    public static final String REAL_WORK = "执行任务：";
    public static final String ERROR = "异常：";

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
                LogManager.getInstance().logMsg("\r\n" + START + Utils.getLogDateToString());
                mInterval = intent.getLongExtra(INTERVAL, 16) * 1000;
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
                LogManager.getInstance().logMsg(PRE_WORK + "当前任务数" + workQueue.size() + Utils.getLogDateToString());
                startClickJob();
                break;
            case ADD:
                mFloatingView.updatePosition();
                workQueue.offer(new WorkPosition(mFloatingView.mX, mFloatingView.mY));
                Toast.makeText(getBaseContext(), "当前任务数：" + workQueue.size(), Toast.LENGTH_SHORT).show();
                LogManager.getInstance().logMsg(ADD_WORK + "当前任务数" + workQueue.size() + Utils.getLogDateToString());
                break;
            case STOP:
                stopAutoService();
                break;
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void stopAutoService() {
        LogManager.getInstance().logMsg(END + Utils.getLogDateToString());
        closeTimer();
        Toast.makeText(getBaseContext(), "关闭服务", Toast.LENGTH_SHORT).show();
        disableSelf();
    }

    private void startClickJob() {
        if (workQueue == null) {
            needOpenPower(false);
            LogManager.getInstance().logMsg(PRE_WORK + "队列为空返回" + Utils.getLogDateToString());
            return;
        }
        WorkPosition currentPosition = workQueue.poll();
        if (currentPosition == null) {
            needOpenPower(false);
            LogManager.getInstance().logMsg(PRE_WORK + "点击位置异常返回" + Utils.getLogDateToString());
            return;
        }
        mFloatingView.setFloatPosition(currentPosition);
        closeTimer();
        LogManager.getInstance().logMsg(PRE_WORK + "定时启动 剩余（" + mInterval + "）秒" + Utils.getLogDateToString());
        timer = new CountDownTimer(mInterval, tipsInterval) {
            @Override
            public void onTick(long millisUntilFinished) {
                Toast.makeText(getBaseContext(), "还有" + millisUntilFinished / 1000 + "秒", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFinish() {
                LogManager.getInstance().logMsg(PRE_WORK + "定时结束真正执行类型" + mMode + Utils.getLogDateToString());
                if (SWIPE.equals(mMode)) {
                    playSwipe();
                } else {
                    playTap();
                }
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
            LogManager.getInstance().logMsg(REAL_WORK + "点击位置 " + x + " " + y + "  " + Utils.getLogDateToString());
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
                    LogManager.getInstance().logMsg(REAL_WORK + "点击完成" + "  " + Utils.getLogDateToString());
                    startClickJob();
                }

                @Override
                public void onCancelled(GestureDescription gestureDescription) {
                    LogManager.getInstance().logMsg(REAL_WORK + "中断点击" + "  " + Utils.getLogDateToString());
                    super.onCancelled(gestureDescription);
                }
            }, null);
        } catch (Exception e) {
            LogManager.getInstance().logMsg(ERROR + "位置不对 " + "  " + Utils.getLogDateToString());
        }
    }

    private void playSwipe() {
        try {
            mFloatingView.updatePosition();
            int fromX = mFloatingView.mX - 1;
            int fromY = mFloatingView.mY - 1;
            int toX = mFloatingView.mX - tX - 1;
            int toY = mFloatingView.mY - tY - 1;
            LogManager.getInstance().logMsg(REAL_WORK + "滑动位置 " + fromX + " " + fromY + " 到 " + toX + " " + toY + " " + Utils.getLogDateToString());
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
                    LogManager.getInstance().logMsg(REAL_WORK + "滑动完成" + "  " + Utils.getLogDateToString());
                    startClickJob();
                }

                @Override
                public void onCancelled(GestureDescription gestureDescription) {
                    super.onCancelled(gestureDescription);
                    LogManager.getInstance().logMsg(REAL_WORK + "中断滑动" + "  " + Utils.getLogDateToString());
                }
            }, null);
        } catch (Exception e) {
            LogManager.getInstance().logMsg(ERROR + "滑动位置不对 " + "  " + Utils.getLogDateToString());
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
        LogManager.getInstance().logMsg("收到微信" + "  " + Utils.getLogDateToString());
        List<CharSequence> texts = event.getText();
        if (!texts.isEmpty()) {
            for (CharSequence text : texts) {
                String content = text.toString();
                //通知栏包括威信红包文字
                if (content.contains("elazipa") && content.contains("开")) {
                    LogManager.getInstance().logMsg("微信命中" + "  " + Utils.getLogDateToString());
                    needOpenPower(true);
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
            LogManager.getInstance().logMsg("微信位置" + rx + "  " + ry + Utils.getLogDateToString());
        } catch (Exception e) {
            LogManager.getInstance().logMsg("异常，配置坐标不对" + Utils.getLogDateToString());
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
