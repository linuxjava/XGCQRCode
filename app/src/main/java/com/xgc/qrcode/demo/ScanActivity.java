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
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
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
 * <p>
 * 2、程序关闭（按 BACK 键）
 * Activity 调用顺序:onPause()->onStop()->onDestory()
 * SurfaceView 调用顺序: surfaceDestroyed()
 * <p>
 * 3、程序切到后台（按 HOME 键）
 * Activity 调用顺序:onPause()->onStop()
 * SurfaceView 调用顺序: surfaceDestroyed()
 * <p>
 * 4、程序切到前台
 * Activity 调用顺序: onRestart()->onStart()->onResume()
 * SurfaceView 调用顺序: surfaceChanged()->surfaceCreated()
 * <p>
 * 5、屏幕锁定（挂断键或锁定屏幕）
 * Activity 调用顺序: onPause()
 * SurfaceView 什么方法都不调用
 * <p>
 * 6、屏幕解锁
 * Activity 调用顺序: onResume()
 * SurfaceView 什么方法都不调用
 */
public class ScanActivity extends AppCompatActivity implements SurfaceHolder.Callback, DecodeCallback, Camera.PreviewCallback {
    public static final String TAG = "XGCQRCode";
    private static final int SIZE = 300;
    private Context mContext;
    private SurfaceView mSurfaceView;
    private CameraManager mCameraManager;
    private DecodeManager mDecodeManager;
    private ViewGroup mLayout;
    private ImageView mScanLineImg;
    private FrameLayout mScanLayout;
    private int mRealHeight;
    private TranslateAnimation mScanDownAnim;
    private Rect mScanRect;//屏幕上扫描区域的位置(以屏幕分辨率为参考)
    private Rect mPreviewScanImageRect;//扫描图像在预览照片中的位置(以预览图像分辨率为参考)
    private int mSurfaceWidth;
    private int mSurfaceHeight;
    private Point mPreviewPoint;

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

        mLayout = findViewById(R.id.layout);
        mScanLineImg = findViewById(R.id.qrcode_scan_line);
        mScanLayout = findViewById(R.id.qrcode_mask);

        mCameraManager = new CameraManager(this);
        mDecodeManager = new DecodeManager(this);
        mDecodeManager.setDecodeCallback(this);
        mCameraManager.setPreviewCallback(this);

        mSurfaceView = findViewById(R.id.surfaceview);
        mSurfaceView.getHolder().addCallback(this);

        mScanLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mScanLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                int scanLayoutHeight = mScanLayout.getHeight();
                int scanLineHeight = mScanLineImg.getHeight();

                int fromY = -scanLineHeight / 2;
                int toY = scanLayoutHeight - scanLineHeight / 2;
                mScanDownAnim = new TranslateAnimation(0, 0, fromY, toY);
                mScanDownAnim.setRepeatCount(Animation.INFINITE);
                mScanDownAnim.setDuration(2500);
                mScanDownAnim.setStartOffset(500);
                startScanAnimation();

                mSurfaceWidth = mSurfaceView.getWidth();
                mSurfaceHeight = mSurfaceView.getHeight();

                int top = mLayout.getTop();
                int bottom = mLayout.getTop() + scanLayoutHeight;
                int left = mScanLayout.getLeft();
                int right = mScanLayout.getRight();

                mScanRect = new Rect(top, left, bottom, right);
            }
        });
    }

    /**
     * 开始扫描动画
     */
    private void startScanAnimation() {
        if (mScanDownAnim != null && mScanLineImg != null) {
            mScanLineImg.clearAnimation();
            mScanLineImg.startAnimation(mScanDownAnim);
            mScanLineImg.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 停止扫描动画
     */
    private void stopScanLineAnimation() {
        if (mScanLineImg != null) {
            mScanLineImg.clearAnimation();
            mScanLineImg.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "surfaceCreated");
        mCameraManager.surfaceCreated(holder);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d(TAG, "surfaceChanged " + width + "x" + height);
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
        startScanAnimation();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //页面切到后台后停止预览
        mCameraManager.stopPreview();
        stopScanLineAnimation();
    }

    /**
     * 二维码解码成功回调
     *
     * @param data
     */
    @Override
    public void decodeResult(int code, final String data) {
        Log.d(TAG, code + "::" + data);
        if(code == DecodeCallback.CODE_SUCC) {
            Toast.makeText(mContext, data, Toast.LENGTH_SHORT).show();
            //延迟10s后再开启预览扫描，模拟业务
            handler.sendEmptyMessageDelayed(0, 3000);
        }else {
            if(code == DecodeCallback.CODE_NO_QRCODE){
                Toast.makeText(mContext, "未检测到二维码", Toast.LENGTH_SHORT).show();
            }else {
                Toast.makeText(mContext, "解码失败", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * camera预览回调
     *
     * @param data
     * @param camera
     */
    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (mScanRect != null) {
            if(mPreviewScanImageRect == null){
                mPreviewScanImageRect = new Rect();
                mPreviewPoint = mCameraManager.getPreviewSize();

                //将屏幕上扫描区域的位置转换Camera中Preview图像的位置
                float previewHeightRatio = mPreviewPoint.x * 1.0f / mSurfaceHeight;
                float previewWidthRatio = mPreviewPoint.y * 1.0f / mSurfaceWidth;

                mPreviewScanImageRect.left = (int) (mScanRect.left * previewHeightRatio);
                mPreviewScanImageRect.right = (int) (mScanRect.right * previewHeightRatio);
                mPreviewScanImageRect.top = (int) (mScanRect.top * previewWidthRatio);
                mPreviewScanImageRect.bottom = (int) (mScanRect.bottom * previewWidthRatio);
            }

            mDecodeManager.sendScanDecode(data, mPreviewPoint.x, mPreviewPoint.y, mPreviewScanImageRect);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCameraManager.destroy();
        mDecodeManager.destory();
    }


    public void onPhotos(View view) {
        Intent albumIntent = new Intent(Intent.ACTION_PICK);
        albumIntent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        startActivityForResult(albumIntent, 0);
    }
    public void onBack(View view) {
        finish();
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
