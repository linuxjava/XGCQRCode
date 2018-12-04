package com.xgc.qrcode.demo.decode;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.text.TextUtils;

import com.tencent.qbar.QBarAIDecoder;
import com.tencent.qbar.QbarNative;

public class DecodeManager {
    private static final String TAG = "QBarAIDecoder";
    public static final int MSG_DECODE_SCAN = 1;//扫描解码
    public static final int MSG_DECODE_FILE = 2;//文件解码
    public static final int MSG_DECODE_SUCC = 3;//解码成功
    public static final int MSG_DECODE_FAIL = 4;//解码失败

    private static final int STATUS_NONE = 0;
    private static final int STATUS_PREVIEW = 1;//预览
    private static final int STATUS_DECODEING = 2;//解码中

    private Context mContext;
    private QBarAIDecoder mQBarAIDecoder;
    private Handler mDecodeHandler;//解码子线程handler
    private DecodeCallback mDecodeCallback;
    private int mStatus = STATUS_PREVIEW;

    public DecodeManager(Context context) {
        mContext = context;
        mQBarAIDecoder = new QBarAIDecoder(context, new InnerDecodeCallBack());


        HandlerThread handlerThread = new HandlerThread("HandlerThread");
        handlerThread.start();
        mDecodeHandler = new Handler(handlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_DECODE_SCAN:
                        doScanDecode(msg);
                        break;
                    case MSG_DECODE_FILE:
                        doFileDecode(msg);
                        break;
                    case MSG_DECODE_FAIL:
                        //从解码状态转换为预览状态
                        if(mStatus == STATUS_DECODEING){
                            mStatus = STATUS_PREVIEW;
                        }
                        break;
                }

            }
        };
    }

    public void setDecodeCallback(DecodeCallback mDecodeCallback) {
        this.mDecodeCallback = mDecodeCallback;
    }

    /**
     * 解码成功后停止解码,业务处理完后需要调用该方法继续开启解码
     */
    public void enableDecode(boolean enable) {
        if(enable) {
            mStatus = STATUS_PREVIEW;
        }else {
            mStatus = STATUS_NONE;
        }
    }

    /**
     * 二维码扫描解码
     *
     * @param data
     * @param previewWidth
     * @param previewHeight
     * @param scanRect
     */
    public void sendScanDecode(byte[] data, int previewWidth, int previewHeight, Rect scanRect) {
        if(mStatus == STATUS_PREVIEW){
            mStatus = STATUS_DECODEING;
        }else {
            return;
        }

        if (data == null || data.length <= 0) {
            return;
        }

        Message message = mDecodeHandler.obtainMessage(MSG_DECODE_SCAN);
        message.obj = data;
        Bundle bundle = new Bundle();
        bundle.putInt("previewWidth", previewWidth);
        bundle.putInt("previewHeight", previewHeight);
        bundle.putParcelable("scanRect", scanRect);

        message.setData(bundle);
        mDecodeHandler.sendMessage(message);
    }

    /**
     * 图片扫描
     *
     * @param filePath
     */
    public void sendFileDecode(String filePath) {
        if(mStatus == STATUS_PREVIEW){
            mStatus = STATUS_DECODEING;
        }else {
            return;
        }

        if (TextUtils.isEmpty(filePath)) {
            return;
        }

        Message message = mDecodeHandler.obtainMessage(MSG_DECODE_FILE);
        message.obj = filePath;
        message.sendToTarget();
    }

    public void destory() {
        if (mQBarAIDecoder != null) {
            mQBarAIDecoder.release();
        }
    }

    private void doScanDecode(Message msg) {
        int previewWidth = msg.getData().getInt("previewWidth");
        int previewHeight = msg.getData().getInt("previewHeight");
        Rect scanRect = msg.getData().getParcelable("scanRect");
        Point point = new Point(previewWidth, previewHeight);
        mQBarAIDecoder.init(QbarNative.SCAN_VIDEO);
        mQBarAIDecoder.decodeFrame((byte[]) msg.obj, point, scanRect);
    }

    private void doFileDecode(Message msg) {
        String filePath = (String) msg.obj;

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 2;
        Bitmap bitmap = BitmapFactory.decodeFile(filePath, options);
        if (bitmap != null) {
            int[] pixels = new int[bitmap.getWidth() * bitmap.getHeight()];
            bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

            mQBarAIDecoder.init(QbarNative.SCAN_FILE);
            mQBarAIDecoder.decodeFile(pixels, new Point(bitmap.getWidth(), bitmap.getHeight()));
            bitmap = null;
        }
    }

    class InnerDecodeCallBack implements QBarAIDecoder.DecodeCallBack {
        /**
         * 解码结果
         *
         * @param result
         */
        @Override
        public void afterDecode(final String result) {
            if (TextUtils.isEmpty(result)) {
                //解码失败,延迟发消息，避免频繁地灰度、裁剪、解码操作
                Message message = mDecodeHandler.obtainMessage(MSG_DECODE_FAIL);
                mDecodeHandler.sendMessageDelayed(message, 200);
            } else {
                if (mDecodeCallback != null && mContext != null) {
                    ((Activity) mContext).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mDecodeCallback.decodeResult(result);
                        }
                    });
                }
            }
        }
    }
}
