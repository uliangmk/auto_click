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

import java.util.ArrayList;
import java.util.List;


public class AutoService extends AccessibilityService {
    public static final String ACTION = "action";
    public static final String ACTION_SHOW = "SHOW";
    public static final String ACTION_STOP = "STOP";
    public static final String ACTION_HIDE = "HIDE";
    public static final String ACTION_PLAY = "PLAY";
    public static final String ACTION_ADD = "ADD";

    public static final String MODE = "mode";
    public static final String MODE_CLICK = "CLICK";
    public static final String MODE_SWIPE = "SWIPE";

    public static final String T_X = "T_X";//滑动模式-横向滑动距离
    public static final String T_Y = "T_Y";//滑动模式-纵向滑动距离
    public static final String INTERVAL = "interval";//倒计时剩余

    private final int TIPS_INTERVAL = 3 * 1000;//提示倒计时间隔
    private final int EVENT_INTERVAL = 5 * 1000;//多个事件间隔

    public static final String START = "预备状态:小窗显示";
    public static final String END = "结束服务";
    public static final String ADD_WORK = "添加任务：";
    public static final String PRE_WORK = "开启任务：";
    public static final String REAL_WORK = "执行任务：";
    public static final String NEW_MSG = "收到微信：";
    public static final String ERROR = "--异常：";

    private FloatingView mFloatingView;
    private WorkPositionData cacheData = new WorkPositionData();
    private CountDownTimer timer;
    private List<WorkPositionData> workQueue = new ArrayList<>();
    private PowerManager.WakeLock wl;
    private boolean isTimerWorking = false;

    @Override
    public void onCreate() {
        super.onCreate();
        mFloatingView = new FloatingView(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return super.onStartCommand(null, flags, startId);
        }
        String action = intent.getStringExtra(ACTION);
        if (TextUtils.isEmpty(action)) {
            return super.onStartCommand(intent, flags, startId);
        }
        switch (action) {
            case ACTION_SHOW:
                LogManager.getInstance().logMsg("\r\n" + START + Utils.getLogDateToString());
                cacheData = new WorkPositionData();
                cacheData.interval = intent.getLongExtra(INTERVAL, 16) * 1000;
                cacheData.toX = intent.getIntExtra(T_X, 0);
                cacheData.toY = intent.getIntExtra(T_Y, 0);
                cacheData.mode = intent.getStringExtra(MODE);
                mFloatingView.show();
                break;
            case ACTION_HIDE:
                mFloatingView.hide();
                closeTimer();
                break;
            case ACTION_PLAY:
                mFloatingView.updatePosition();
                WorkPositionData data = new WorkPositionData(mFloatingView.mX, mFloatingView.mY);
                data.mode = cacheData.mode;
                data.interval = cacheData.interval;
                addTask(data, true);
                LogManager.getInstance().logMsg(PRE_WORK + "当前任务数" + workQueue.size() + Utils.getLogDateToString());
                startJob();
                break;
            case ACTION_ADD:
                mFloatingView.updatePosition();
                WorkPositionData dataAdd = new WorkPositionData(mFloatingView.mX, mFloatingView.mY);
                dataAdd.interval = EVENT_INTERVAL;
                addTask(dataAdd, false);
                toastPositionMsg(dataAdd);
                break;
            case ACTION_STOP:
                stopAutoService();
                break;
        }
        return super.onStartCommand(intent, flags, startId);
    }

    //提示当前位置占屏幕百分比
    private void toastPositionMsg(WorkPositionData data) {
        try {
            if (data == null) {
                return;
            }
            int screenWidth = mFloatingView.getScreenWidth();
            int screenHeight = mFloatingView.getScreenHeight();
            float x = (float) data.workX / (float) screenWidth;
            float y = (float) data.workY / (float) screenHeight;
            Toast.makeText(getBaseContext(), "Y=  " + y + "  X=  " + x, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
        }
    }

    private void addTask(WorkPositionData workPositionData, boolean insertFirst) {
        if (workPositionData == null) {
            return;
        }
        if (insertFirst) {
            workQueue.add(0, workPositionData);
        } else {
            workQueue.add(workPositionData);
        }
        LogManager.getInstance().logMsg(ADD_WORK + "当前任务数" + workQueue.size() + Utils.getLogDateToString());
    }

    private void stopAutoService() {
        LogManager.getInstance().logMsg(END + Utils.getLogDateToString());
        closeTimer();
        Toast.makeText(getBaseContext(), "关闭服务", Toast.LENGTH_SHORT).show();
        disableSelf();
    }


    private void startJob() {
        if (workQueue == null || workQueue.isEmpty()) {
            isTimerWorking = false;
            needOpenPower(false);
            LogManager.getInstance().logMsg(PRE_WORK + "队列为空 返回" + Utils.getLogDateToString());
            return;
        }
        final WorkPositionData currentData = workQueue.remove(0);
        if (currentData == null) {
            isTimerWorking = false;
            needOpenPower(false);
            LogManager.getInstance().logMsg(PRE_WORK + "点击位置为空 返回" + Utils.getLogDateToString());
            return;
        }
        mFloatingView.setFloatPosition(currentData);
        closeTimer();
        LogManager.getInstance().logMsg(PRE_WORK + "定时启动 剩余（" + currentData.interval + "）毫秒" + Utils.getLogDateToString());
        timer = new CountDownTimer(currentData.interval, TIPS_INTERVAL) {
            @Override
            public void onTick(long millisUntilFinished) {
                Toast.makeText(getBaseContext(), "还有" + millisUntilFinished / 1000 + "秒", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFinish() {
                LogManager.getInstance().logMsg(PRE_WORK + "定时结束真正执行类型" + currentData.mode + Utils.getLogDateToString());
                if (MODE_SWIPE.equals(currentData.mode)) {
                    playSwipe(currentData);
                } else {
                    playClick(currentData);
                }
            }
        }.start();
        isTimerWorking = true;
    }

    private void closeTimer() {
        if (timer == null) {
            return;
        }
        timer.cancel();
    }

    private void playClick(WorkPositionData currentData) {
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
                    startJob();
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

    private void playSwipe(WorkPositionData currentData) {
        try {
            mFloatingView.updatePosition();
            int fromX = mFloatingView.mX - 1;
            int fromY = mFloatingView.mY - 1;
            int toX = mFloatingView.mX - currentData.toX - 1;
            int toY = mFloatingView.mY + currentData.toY;
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
                    startJob();
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
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED://com.tencent.mm
                if (!TextUtils.equals("com.tencent.mm", event.getPackageName())) {
                    return;
                }
                dealNotificationChange(event);
                break;
        }
    }

    private void dealNotificationChange(AccessibilityEvent event) {
        LogManager.getInstance().logMsg(NEW_MSG + "  " + Utils.getLogDateToString());
        List<CharSequence> texts = event.getText();
        if (!texts.isEmpty()) {
            for (CharSequence text : texts) {
                String content = text.toString();
                //通知栏包括威信红包文字
                if (content.contains("elazipa") && content.contains("点击") || content.contains("滑动")) {
                    LogManager.getInstance().logMsg(NEW_MSG + "消息命中" + "  " + Utils.getLogDateToString());
                    needOpenPower(true);
                    WorkPositionData data = convertStringToData(content);
                    addTask(data, false);
                    if (!isTimerWorking) {
                        startJob();
                    }

                }
            }
        }
    }

    //模版  滑动_    点击_70_10 (先y后x x可没有默认50 点击_70)
    private WorkPositionData convertStringToData(String content) {
        WorkPositionData data = new WorkPositionData();
        try {
            if (!TextUtils.isEmpty(content)) {
                int screenWidth = mFloatingView.getScreenWidth();
                int screenHeight = mFloatingView.getScreenHeight();
                if (content.contains("滑动")) {
                    data.mode = MODE_SWIPE;
                    data.interval = 5 * 1000;
                    data.toX = 0;
                    data.toY = screenHeight / 4;
                    data.workX = screenWidth / 2;
                    data.workY = screenHeight / 2;
                } else {
                    data.mode = MODE_CLICK;
                    data.interval = 15 * 1000;
                    data.workX = screenWidth / 2;
                    String[] result = content.split("_");
                    if (result != null && result.length >= 2) {
                        int y = Integer.parseInt(result[1]);
                        data.workY = screenHeight * y / 100;
                    }
                    if (result != null && result.length >= 3) {
                        int x = Integer.parseInt(result[2]);
                        data.workX = screenWidth * x / 100;
                    }
                }
            }
        } catch (Exception e) {
            LogManager.getInstance().logMsg(ERROR + "微信数据转换异常 " + "  " + Utils.getLogDateToString());
        }
        return data;
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
}
