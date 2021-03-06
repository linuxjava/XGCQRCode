package com.xgc.qrcode.demo.camera;

import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * camera使用注意事项：
 * 1.autoFocus方法需要在startPreview后才可以调用；如果在stopPreview后调用autoFocus的调用则会产生如下错误：
 * java.lang.RuntimeException: autoFocus failed
 * 2.setParameters方法只能在stopPreview后调用，如果已startPreview调用setParameters会产生如下错误信息：
 * java.lang.RuntimeException: setParameters failed
 */
public class CameraManager {
    private static final String TAG = "CameraManager";
    private static final int MIN_PREVIEW_PIXELS = 470 * 320; // normal screen
    private static final int MAX_PREVIEW_PIXELS = 1920 * 1080; // uses full screen resolution on all 'large' screens
    private static final int MSG_AUTO_FOCUS = 1;
    private Context context;
    private Camera camera;
    private AutoFocusCallback autoFocusCallback;//自动对焦回调
    private Camera.PreviewCallback previewCallback;//
    private boolean isStopPreview = true;//是否停止预览
    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if (msg.what == MSG_AUTO_FOCUS) {
                autoFocus();
            }

            return true;
        }
    });

    private CameraManager() {

    }

    public CameraManager(Context c) {
        context = c;
    }

    public Point getPreviewSize() {
        if (camera == null) {
            return null;
        }

        Camera.Size size = camera.getParameters().getPreviewSize();
        return new Point(size.width, size.height);
    }

    public void startPreview() {
        if (camera != null) {
            Log.d(TAG, "startPreview");
            isStopPreview = false;
            camera.startPreview();
            autoFocus();
        }
    }

    public void stopPreview() {
        if (camera != null) {
            Log.d(TAG, "stopPreview");
            isStopPreview = true;
            camera.stopPreview();
        }
    }

    public void setPreviewCallback(Camera.PreviewCallback previewCallback) {
        this.previewCallback = previewCallback;
    }

    public void surfaceCreated(SurfaceHolder holder) {
        camera = Camera.open();

        if (camera != null) {
            camera.setDisplayOrientation(90);//设置竖屏方向
        }
        autoFocusCallback = new AutoFocusCallback();
    }

    public void surfaceChanged(SurfaceHolder holder, int width, int height) {
        if (camera == null) {
            return;
        }

        //摄像头画面显示在Surface上
        try {
            /**
             * 从其它页面返回扫描页面时surfaceChanged可能被调用多次,如果已开启预览时，更新camera的Parameters就会奔溃，所以在更新
             * Parameters前先调用stopPreview。奔溃错误信息如下：
             * java.lang.RuntimeException: setParameters failed
             */
            stopPreview();
            updateCameraParameters(width, height);
            camera.setPreviewDisplay(holder);
            if (previewCallback != null) {
                camera.setPreviewCallback(previewCallback);
            }
            startPreview();
        } catch (IOException e) {
            e.printStackTrace();
            if (camera != null) {
                camera.release();
            }
            camera = null;
        }
    }

    public void surfaceDestroyed() {
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }

        if (camera != null) {
            camera.cancelAutoFocus();
            stopPreview();
            camera.setPreviewCallback(null);
            camera.release();
            camera = null;
        }
    }

    public void destroy() {
        surfaceDestroyed();
    }

    /**
     * 自动对焦
     */
    private void autoFocus() {
        if (camera != null && !isStopPreview) {
            /**
             * autoFocus的调用需要是startPreview，否则会产生如下错误：
             * java.lang.RuntimeException: autoFocus failed
             */
            Log.d(TAG, "autoFocus");
            camera.autoFocus(autoFocusCallback);
        }
    }

    /**
     * 设置相机参数
     *
     * @param surfaceWidth  SurfaceView的宽度
     * @param surfaceHeight SurfaceView的高度
     */
    private void updateCameraParameters(int surfaceWidth, int surfaceHeight) {
        if (camera != null) {
            Camera.Parameters p = camera.getParameters();

            //设置画面的颜色
            p.setPreviewFormat(PixelFormat.YCbCr_420_SP);

            long time = new Date().getTime();
            p.setGpsTimestamp(time);

            //设置预览画面分辨率
            Point previewSize = findBestSize(p, new Point(surfaceWidth, surfaceHeight));
            p.setPreviewSize(previewSize.x, previewSize.y);
            //设置拍照图片分辨率
//            Camera.Size pictureSize = findBestPictureSize(p);
//            p.setPictureSize(pictureSize.width, pictureSize.height);

            //在酷派8190 4.0.4上，返回的modesList=null，需要加空指针保护，对应的bugId:49617825
            List<String> modesList = p.getSupportedFocusModes();
            if (modesList != null && modesList.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                p.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            }

            camera.setParameters(p);
        }
    }

    /**
     * 找到相机最合适的分辨率
     *
     * @param parameters
     * @param targetResolution 目标分辨率
     * @return
     */
    private static Point findBestSize(Camera.Parameters parameters, Point targetResolution) {
        Point bestSize = findBestResolutionSize(parameters, targetResolution);
        if (bestSize != null) {
            Log.i(TAG, "findBestResolutionSize Found best approximate preview size: " + bestSize);
            return bestSize;
        }
        return findBestRatioSize(parameters, targetResolution);
    }

    /**
     * 找到最接近的分辨率
     *
     * @param parameters
     * @param targetResolution
     * @return
     */
    private static Point findBestResolutionSize(Camera.Parameters parameters, Point targetResolution) {
        //将camera支持的预览分辨率降序排列
        List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();
        List<Camera.Size> supportedPreviewSizes = null;
        if (sizes == null) {
            supportedPreviewSizes = new ArrayList<Camera.Size>();
        } else {
            supportedPreviewSizes = new ArrayList<Camera.Size>(sizes);
        }
        Collections.sort(supportedPreviewSizes, new Comparator<Camera.Size>() {
            @Override
            public int compare(Camera.Size a, Camera.Size b) {
                int aPixels = a.height * a.width;
                int bPixels = b.height * b.width;
                if (bPixels < aPixels) {
                    return -1;
                }
                if (bPixels > aPixels) {
                    return 1;
                }
                return 0;
            }
        });

        //打印支持的所有预览分辨率
        StringBuilder previewSizesString = new StringBuilder();
        for (Camera.Size supportedPreviewSize : supportedPreviewSizes) {
            previewSizesString.append(supportedPreviewSize.width).append('x').append(supportedPreviewSize.height).append(' ');
        }
        Log.i(TAG, "Supported preview sizes: " + previewSizesString);

        //找到分辨率最接近targetPixels的
        Point bestSize = null;
        final int targetPixels = targetResolution.x * targetResolution.y;
        int diffPixels = Integer.MAX_VALUE;
        for (Camera.Size supportedPreviewSize : supportedPreviewSizes) {
            int realWidth = supportedPreviewSize.width;
            int realHeight = supportedPreviewSize.height;
            int pixels = realWidth * realHeight;
            if (pixels < MIN_PREVIEW_PIXELS || pixels > MAX_PREVIEW_PIXELS) {
                continue;
            }
            //supportedPreviewSizes中的宽为手机屏幕的高,高为手机屏幕的宽
            boolean isCandidatePortrait = realWidth > realHeight;
            int maybeFlippedWidth = isCandidatePortrait ? realHeight : realWidth;
            int maybeFlippedHeight = isCandidatePortrait ? realWidth : realHeight;
            if (maybeFlippedWidth == targetResolution.x && maybeFlippedHeight == targetResolution.y) {
                return new Point(realWidth, realHeight);
            }
            int cameraPixels = maybeFlippedWidth * maybeFlippedHeight;
            int newDiff = Math.abs(cameraPixels - targetPixels);
            if (newDiff < diffPixels) {
                bestSize = new Point(realWidth, realHeight);
                diffPixels = newDiff;
            }
        }

        //如果没有找到，那么设置默认的PreviewSize
        if (bestSize == null) {
            Camera.Size defaultSize = parameters.getPreviewSize();
            if (defaultSize == null) {
                bestSize = new Point();
            } else {
                bestSize = new Point(defaultSize.width, defaultSize.height);
            }
            Log.i(TAG, "findBestResolutionSize , using default: " + bestSize);
        }

        //宽度相同，高度相差要小于100像素
        if ((targetResolution.x == bestSize.y && Math.abs(targetResolution.y - bestSize.x) < 100)) {
            return bestSize;
        }
        //高度相同，宽度相差要小于100像素
        if ((targetResolution.y == bestSize.x && Math.abs(targetResolution.x - bestSize.y) < 100)) {
            return bestSize;
        }

        return null;
    }

    /**
     * 找到最接近的比例
     *
     * @param parameters
     * @param targetResolution
     * @return
     */
    private static Point findBestRatioSize(Camera.Parameters parameters, Point targetResolution) {
        List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();
        List<Camera.Size> supportedPreviewSizes = null;
        if (sizes == null) {
            supportedPreviewSizes = new ArrayList<Camera.Size>();
        } else {
            supportedPreviewSizes = new ArrayList<Camera.Size>(sizes);
        }
        Collections.sort(supportedPreviewSizes, new Comparator<Camera.Size>() {
            @Override
            public int compare(Camera.Size a, Camera.Size b) {
                int aPixels = a.height * a.width;
                int bPixels = b.height * b.width;
                if (bPixels < aPixels) {
                    return -1;
                }
                if (bPixels > aPixels) {
                    return 1;
                }
                return 0;
            }
        });

        Point bestSize = null;
        float screenAspectRatio = (float) targetResolution.x / (float) targetResolution.y;
        final int SCREEN_PIXELS = targetResolution.x * targetResolution.y;

        float diffRatio = Float.POSITIVE_INFINITY;
        for (Camera.Size supportedPreviewSize : supportedPreviewSizes) {
            int realWidth = supportedPreviewSize.width;
            int realHeight = supportedPreviewSize.height;
            int pixels = realWidth * realHeight;
            if (pixels < MIN_PREVIEW_PIXELS || pixels > MAX_PREVIEW_PIXELS || pixels > SCREEN_PIXELS) {
                continue;
            }
            //supportedPreviewSizes中的宽为手机屏幕的高,高为手机屏幕的宽
            boolean isCandidatePortrait = realWidth > realHeight;
            int maybeFlippedWidth = isCandidatePortrait ? realHeight : realWidth;
            int maybeFlippedHeight = isCandidatePortrait ? realWidth : realHeight;
            if (maybeFlippedWidth == targetResolution.x && maybeFlippedHeight == targetResolution.y) {
                return new Point(realWidth, realHeight);
            }
            float aspectRatio = (float) maybeFlippedWidth / (float) maybeFlippedHeight;
            float newDiff = Math.abs(aspectRatio - screenAspectRatio);
            if (newDiff < diffRatio) {
                bestSize = new Point(realWidth, realHeight);
                diffRatio = newDiff;
            }
        }

        if (bestSize == null) {
            Camera.Size defaultSize = parameters.getPreviewSize();
            if (defaultSize == null) {
                bestSize = new Point();
            } else {
                bestSize = new Point(defaultSize.width, defaultSize.height);
            }
            Log.i(TAG, "findBestRatioSize, using default: " + bestSize);
        }

        Log.i(TAG, "findBestRatioSize Found best approximate preview size: " + bestSize);
        return bestSize;
    }

    final class AutoFocusCallback implements Camera.AutoFocusCallback {
        private static final long AUTOFOCUS_INTERVAL_MS = 1000L;

        /**
         * 预览模式循环自动对焦,固定时间的延迟后会发送下一次的自动聚焦消息，如此达到循环聚焦的目的
         *
         * @param success
         * @param camera
         */
        public void onAutoFocus(boolean success, Camera camera) {
            //Log.i(TAG, "onAutoFocus success = " + success);
            if (handler != null) {
                handler.sendEmptyMessageDelayed(MSG_AUTO_FOCUS, AUTOFOCUS_INTERVAL_MS);
            }
        }

    }

    private boolean isUIThread() {
        return Thread.currentThread() == Looper.getMainLooper().getThread();

    }

    private float getScreenW() {
        return context.getResources().getDisplayMetrics().widthPixels;
    }

    private float getScreenH() {
        return context.getResources().getDisplayMetrics().heightPixels;
    }
}
