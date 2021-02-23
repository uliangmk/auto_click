package com.wonbin.autoclick;

import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;


public class FloatingView extends FrameLayout implements View.OnClickListener {
    private Context mContext;
    private View mView;
    private ImageView mPlayView;
    private ImageView mAddView;
    private ImageView mCloseView;
    private int mTouchStartX, mTouchStartY;//手指按下时坐标
    private WindowManager.LayoutParams mParams;
    private FloatingManager mWindowManager;

    public int mX, mY;

    public FloatingView(Context context) {
        super(context);
        mContext = context.getApplicationContext();
        LayoutInflater mLayoutInflater = LayoutInflater.from(context);
        mView = mLayoutInflater.inflate(R.layout.floating_view, null);

        mPlayView = (ImageView) mView.findViewById(R.id.play);
        mAddView = (ImageView) mView.findViewById(R.id.add);
        mCloseView = (ImageView) mView.findViewById(R.id.close);
        mPlayView.setOnClickListener(this);
        mAddView.setOnClickListener(this);
        mCloseView.setOnClickListener(this);

        mWindowManager = FloatingManager.getInstance(mContext);
    }

    public void show() {
        mParams = new WindowManager.LayoutParams();
        mParams.gravity = Gravity.LEFT | Gravity.TOP;
        if (Build.VERSION.SDK_INT >= 26) {
            mParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            mParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        }
        mParams.format = PixelFormat.RGBA_8888;
        mParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR |
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
        mParams.width = LayoutParams.WRAP_CONTENT;
        mParams.height = LayoutParams.WRAP_CONTENT;
        boolean result = mWindowManager.addView(mView, mParams);
        if (result) {
            Toast.makeText(getContext(), "点击开始，并将左上角黄点对准", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getContext(), "无法工作：没开启小窗权限", Toast.LENGTH_LONG).show();
        }
        mView.setOnTouchListener(mOnTouchListener);
    }

    public void hide() {
        mWindowManager.removeView(mView);
    }

    private OnTouchListener mOnTouchListener = new OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mTouchStartX = (int) event.getRawX();
                    mTouchStartY = (int) event.getRawY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    mParams.x += (int) event.getRawX() - mTouchStartX;
                    mParams.y += (int) event.getRawY() - mTouchStartY;//相对于屏幕左上角的位置
//                    Log.i("ulog", " -- " + mParams.x + " " + mParams.y);
                    mWindowManager.updateView(mView, mParams);
                    mTouchStartX = (int) event.getRawX();
                    mTouchStartY = (int) event.getRawY();
                    break;
                case MotionEvent.ACTION_UP:
                    break;
            }
            return true;
        }
    };

    @Override
    public void onClick(View view) {
        Intent intent = new Intent(getContext(), AutoService.class);
        switch (view.getId()) {
            case R.id.play:
                intent.putExtra(AutoService.ACTION, AutoService.PLAY);
                break;
            case R.id.add:
                intent.putExtra(AutoService.ACTION, AutoService.ADD);
                break;
            case R.id.close:
                intent.putExtra(AutoService.ACTION, AutoService.HIDE);
                Intent appMain = new Intent(getContext(), MainActivity.class);
                getContext().startActivity(appMain);
                break;
        }
        getContext().startService(intent);
    }

    public void updatePosition() {
        int[] location = new int[2];
        mView.getLocationOnScreen(location);
        mX = location[0];
        mY = location[1];
        Log.e("ulog", "刷新位置 -- " + location[0] + " " + location[1]);
    }

    public void setFloatPosition(AutoService.WorkPosition position) {
        mParams.x = position.workX;
        mParams.y = position.workY;
        Log.w("ulog", " --设置位置 " + mParams.x + " " + mParams.y);
        mWindowManager.updateView(mView, mParams);
    }

}
