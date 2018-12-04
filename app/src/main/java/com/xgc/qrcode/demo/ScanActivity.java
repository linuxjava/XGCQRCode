package com.xgc.qrcode.demo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import com.tencent.qbar.Util;
import com.xgc.qrcode.demo.camera.CameraManager;
import com.xgc.qrcode.demo.decode.DecodeCallback;
import com.xgc.qrcode.demo.decode.DecodeManager;

/**
 * SurfaceView生命周期:
 * 1、程序打开
 * Activity 调用顺序:onCreate()->onStart()->onResume()
 * SurfaceView 调用顺序: surfaceCreated()->surfaceChanged()
 *
 * 2、程序关闭（按 BACK 键）
 * Activity 调用顺序:onPause()->onStop()->onDestory()
 * SurfaceView 调用顺序: surfaceDestroyed()
 *
 * 3、程序切到后台（按 HOME 键）
 * Activity 调用顺序:onPause()->onStop()
 * SurfaceView 调用顺序: surfaceDestroyed()
 *
 * 4、程序切到前台
 * Activity 调用顺序: onRestart()->onStart()->onResume()
 * SurfaceView 调用顺序: surfaceChanged()->surfaceCreated()
 *
 * 5、屏幕锁定（挂断键或锁定屏幕）
 * Activity 调用顺序: onPause()
 * SurfaceView 什么方法都不调用
 *
 * 6、屏幕解锁
 * Activity 调用顺序: onResume()
 * SurfaceView 什么方法都不调用
 */
public class ScanActivity extends AppCompatActivity implements SurfaceHolder.Callback, DecodeCallback, Camera.PreviewCallback {
    public static final String TAG = "XGCQRCode";
    private Context mContext;
    private SurfaceView mSurfaceView;
    private CameraManager mCameraManager;
    private DecodeManager mDecodeManager;

    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            mDecodeManager.enableDecode(true);
            return true;
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        mContext = this;
        mCameraManager = new CameraManager(this);
        mDecodeManager = new DecodeManager(this);
        mDecodeManager.setDecodeCallback(this);
        mCameraManager.setPreviewCallback(this);

        mSurfaceView = findViewById(R.id.surfaceview);
        mSurfaceView.getHolder().addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "surfaceCreated");
        mCameraManager.surfaceCreated(holder);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d(TAG, "surfaceChanged");
        mCameraManager.surfaceChanged(holder, width, height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "surfaceDestroyed");
        mCameraManager.surfaceDestroyed();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //页面切到前台开始预览
        mCameraManager.startPreview();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //页面切到后台后停止预览
        mCameraManager.stopPreview();
    }

    /**
     * 二维码解码成功回调
     *
     * @param data
     */
    @Override
    public void decodeResult(String data) {
        Log.d(TAG, data);
        Toast.makeText(mContext, data, Toast.LENGTH_SHORT).show();
        //延迟10s后再开启预览扫描，模拟业务
        handler.sendEmptyMessageDelayed(0, 3000);
    }

    /**
     * camera预览回调
     *
     * @param data
     * @param camera
     */
    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        Point point = mCameraManager.getPreviewSize();
        Rect rect = new Rect(0, 0, point.x, point.y);
        mDecodeManager.sendScanDecode(data, point.x, point.y, rect);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCameraManager.destroy();
        mDecodeManager.destory();
    }


    public void onFile(View view) {
        Intent albumIntent = new Intent(Intent.ACTION_PICK);
        albumIntent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        startActivityForResult(albumIntent, 0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 0) {
            if (resultCode == Activity.RESULT_OK) {
                Uri uri = data.getData();
                if (uri != null) {
                    String filePath = parseFilePath(uri);
                    if (Util.isNullOrNil(filePath)) {
                        new AlertDialog.Builder(this).setMessage("select picture not found!").show();
                        return;
                    }

                    mDecodeManager.sendFileDecode(filePath);
                }
            }
        }
    }

    private String parseFilePath(Uri uri) {
        String[] filePathColumn = {MediaStore.Images.Media.DATA};
        String picturePath = null;
        Cursor cursor = getContentResolver().query(uri, filePathColumn, null, null, null);
        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            picturePath = cursor.getString(columnIndex);
            cursor.close();
        } else if (!Util.isNullOrNil(uri.getPath())) {
            picturePath = uri.getPath();
        }
        return picturePath;
    }
}
