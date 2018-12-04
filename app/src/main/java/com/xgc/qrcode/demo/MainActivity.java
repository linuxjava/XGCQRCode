package com.xgc.qrcode.demo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.tencent.qbar.QBarAIDecoder;
import com.tencent.qbar.QbarNative;
import com.tencent.qbar.Util;
import com.xgc.qrcode.demo.decode.DecodeCallback;
import com.xgc.qrcode.demo.decode.DecodeManager;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    /**
     * 扫码
     * @param view
     */
    public void onScan(View view){
        startActivity(new Intent(this, ScanActivity.class));
    }
}
