package com.wonbin.autoclick;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private Button mStart;
    TextView per1, per2;
    private EditText mInterval;
    private RadioGroup mCheckMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mStart = findViewById(R.id.start);
        per1 = findViewById(R.id.permission_1);
        per2 = findViewById(R.id.permission_2);
        mInterval = findViewById(R.id.interval);
        mCheckMode = findViewById(R.id.check_mode);
        mStart.setOnClickListener(this);
        per1.setOnClickListener(this);
        per2.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        Intent intent = new Intent(this, AutoService.class);
        switch (v.getId()) {
            case R.id.start:
                intent.putExtra(AutoService.ACTION, AutoService.SHOW);
                intent.putExtra("interval", Integer.valueOf(mInterval.getText().toString()));
                int id = mCheckMode.getCheckedRadioButtonId();
                intent.putExtra(AutoService.MODE, id == R.id.swipe ? AutoService.SWIPE : AutoService.TAP);
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


}
