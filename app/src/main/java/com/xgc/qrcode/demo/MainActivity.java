package com.xgc.qrcode.demo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback {
    private SurfaceView mSurfaceView;
    private CameraManager mCameraManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mCameraManager = new CameraManager(this);

        mSurfaceView = findViewById(R.id.surfaceview);
        mSurfaceView.getHolder().addCallback(this);


    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d("xiao1", "surfaceCreated");
        mCameraManager.create(holder);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d("xiao1", "surfaceChanged");
        mCameraManager.change(holder, width, height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d("xiao1", "surfaceDestroyed");
        mCameraManager.destroyed();
    }
}
