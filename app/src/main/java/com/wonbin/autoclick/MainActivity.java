package com.wonbin.autoclick;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private Button mStart, mStop;
    TextView per1, per2;
    private EditText mIntervalS, mIntervalH, mIntervalM, swipeX, swipeY;
    private View swipeLayout;
    private RadioGroup mCheckMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mStart = findViewById(R.id.start);
        mStop = findViewById(R.id.stop);
        per1 = findViewById(R.id.permission_1);
        per2 = findViewById(R.id.permission_2);
        mIntervalS = findViewById(R.id.interval_s);
        mIntervalH = findViewById(R.id.interval_h);
        mIntervalM = findViewById(R.id.interval_m);
        swipeX = findViewById(R.id.to_x);
        swipeY = findViewById(R.id.to_y);
        mCheckMode = findViewById(R.id.check_mode);
        swipeLayout = findViewById(R.id.swipe_layout);
        mStart.setOnClickListener(this);
        mStop.setOnClickListener(this);
        per1.setOnClickListener(this);
        per2.setOnClickListener(this);

        mCheckMode.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                int id = radioGroup.getCheckedRadioButtonId();
                if (id == R.id.swipe) {
                    swipeLayout.setVisibility(View.VISIBLE);
                } else {
                    swipeLayout.setVisibility(View.GONE);
                }
            }
        });

        Utils.verifyStoragePermissions(this);
    }

    @Override
    public void onClick(View v) {
        Intent intent = new Intent(this, AutoService.class);
        switch (v.getId()) {
            case R.id.start:
                intent.putExtra(AutoService.ACTION, AutoService.SHOW);
                intent.putExtra(AutoService.INTERVAL, getTimeSecond());
                intent.putExtra(AutoService.T_X, getEditInt(swipeX));
                intent.putExtra(AutoService.T_Y, getEditInt(swipeY));
                int id = mCheckMode.getCheckedRadioButtonId();
                intent.putExtra(AutoService.MODE, id == R.id.swipe ? AutoService.SWIPE : AutoService.TAP);
                startService(intent);
                finish();
                break;
            case R.id.stop:
                intent.putExtra(AutoService.ACTION, AutoService.STOP);
                startService(intent);
                finish();
                break;
            case R.id.permission_1:
                Intent intent2 = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                startActivity(intent2);
                break;
            case R.id.permission_2:
                Intent intentFloat = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                startActivity(intentFloat);
                break;
        }
    }

    private long getTimeSecond() {



        long result = 0L;
        int timeH = getEditInt(mIntervalH);
        int timeM = getEditInt(mIntervalM);
        int timeS = getEditInt(mIntervalS);
        long currentTime = Utils.getStringToDate(Utils.getDateToString());
        long targetTime = Utils.changeToTargetTime(timeH, timeM, timeS);
        Log.i("ulog", "currentTime -- " + currentTime + "  targetTime --  " + targetTime);
        result = targetTime - currentTime;
        return result;
    }

    private int getEditInt(EditText swipeY) {
        int re = 0;
        if (swipeY == null || TextUtils.isEmpty(swipeY.getText())) {
            return re;
        }
        try {
            re = Integer.parseInt(swipeY.getText().toString());
        } catch (Exception e) {
            re = 0;
        }
        return re;
    }


}
