package com.tencent.qbar;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.Log;

import com.xgc.qrcode.demo.decode.DecodeCallback;

import java.util.List;


/**
 * Created by williamjin on 18/5/14.
 */

public class QBarAIDecoder {
    private static final String TAG = "QBarAIDecoder";

    private QbarNative qbarNative;
    private Context context;
    private static final String AIQBAR_DATA_DIR = "qbar";
    private static final int MAX_CODE_NUM = 3;

    private boolean inited;
    private static boolean aiModelCopyed;


    private Object syncObject = new Object();

    private byte[] tempOutBytes;
    private byte[] tempGrayData;

    public interface DecodeCallBack {
        void afterDecode(int code, String result);
    }

    private DecodeCallBack callBack;

    public QBarAIDecoder(Context context, DecodeCallBack callBack) {
        qbarNative = new QbarNative();
        this.context = context;
        this.callBack = callBack;
    }

    public void init(int scanSource) {
        if (inited) {
            return;
        }
        try {
            final String detect_model_bin_path = context.getFilesDir().getAbsolutePath() + "/" + AIQBAR_DATA_DIR + "/qbar/detect_model.bin";
            final String detect_model_param_path = context.getFilesDir().getAbsolutePath() + "/" + AIQBAR_DATA_DIR + "/qbar/detect_model.param";
            final String srnet_bin_path = context.getFilesDir().getAbsolutePath() + "/" + AIQBAR_DATA_DIR + "/qbar/srnet.bin";
            final String srnet_param_path = context.getFilesDir().getAbsolutePath() + "/" + AIQBAR_DATA_DIR + "/qbar/srnet.param";

            if (!aiModelCopyed) {
                Util.copyFile(context.getResources().getAssets().open("qbar/detect_model.bin"), detect_model_bin_path, true);
                Util.copyFile(context.getResources().getAssets().open("qbar/detect_model.param"), detect_model_param_path, true);
                Util.copyFile(context.getResources().getAssets().open("qbar/srnet.bin"), srnet_bin_path, true);
                Util.copyFile(context.getResources().getAssets().open("qbar/srnet.param"), srnet_param_path, true);
                aiModelCopyed = true;
            }
            Log.i(TAG, "init model param");
            QbarNative.QbarAiModelParam qbarAiModelParam = new QbarNative.QbarAiModelParam();
            qbarAiModelParam.detect_model_bin_path_ = detect_model_bin_path;
            qbarAiModelParam.detect_model_param_path_ = detect_model_param_path;
            qbarAiModelParam.superresolution_model_bin_path_ = srnet_bin_path;
            qbarAiModelParam.superresolution_model_param_path_ = srnet_param_path;
            int result = qbarNative.init(QbarNative.SEARCH_MULTI, scanSource, "ANY", "UTF-8", qbarAiModelParam);
            if (result != 0) {
                Log.i(TAG, "init qbar error, " + result);
                return;
            }
            result = setReaders();
            if (result != 0) {
                Log.i(TAG, "set qbar  readers error, " + result);
                return;
            }

            inited = true;

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 解码数据data
     *
     * @param data
     * @param size data的分辨率
     * @param rect 扫描的矩形区域
     */
    public void decodeFrame(byte[] data, Point size, Rect rect) {
        if (data == null || data.length <= 0) {
            Log.w(TAG, "prepareGrayData , data is null");
            callBack.afterDecode(DecodeCallback.CODE_DATA_NULL, null);
            return;
        }

        Log.i(TAG, String.format("decode, size %s, rect %s", size, rect));

        //int width = rect.width();
        //int height = rect.height();
        int width = size.x;
        int height = size.y;

        if (tempOutBytes == null) {
            tempOutBytes = new byte[width * height * 3 / 2];
            tempGrayData = new byte[width * height];
        } else if (tempOutBytes.length != width * height * 3 / 2) {
            tempOutBytes = null;
            tempOutBytes = new byte[width * height * 3 / 2];
            tempGrayData = null;
            tempGrayData = new byte[width * height];
        }

        int[] outImgSize = new int[2];

        synchronized (syncObject) {

            if (!inited) {
                return;
            }

            //对图片进行灰度处理并且裁剪获得扫描区域
            int result = QbarNative.gray_rotate_crop_sub(tempOutBytes, outImgSize, data, size.x, size.y,
                    rect.left, rect.top, (rect.right - rect.left), (rect.bottom - rect.top), 90, 0);
            //不进行裁剪
            //int decodeResult = QbarNative.gray_rotate_crop_sub(tempOutBytes, outImgSize, data, size.x, size.y, 0, 0, size.x, size.y, 90, 0);
            if (result != 0) {
                Log.e(TAG, "rotate decodeResult " + result);
                callBack.afterDecode(DecodeCallback.CODE_GRAY_CROP_ERROR, null);
                return;
            }

            System.arraycopy(tempOutBytes, 0, tempGrayData, 0, tempGrayData.length);

            //Log.i(TAG, String.format(" rotate x %d y %d l %d t %d w %d h %d out1 %d out2 %d", size.x, size.y ,rect.left, rect.top, width, height, outImgSize[0], outImgSize[1]));
            decodeInternal(tempGrayData, outImgSize[0], outImgSize[1]);
        }
    }

    /**
     * 解码图片文件
     *
     * @param pixels 图片文件像素
     * @param size   图片文件bitmap大小
     */
    public void decodeFile(int[] pixels, Point size) {
        Log.i(TAG, String.format("decode, size %s", size.toString()));
        if (pixels == null || pixels.length <= 0) {
            Log.w(TAG, "prepareGrayData , data is null");
            callBack.afterDecode(DecodeCallback.CODE_DATA_NULL, null);
            return;
        }

        byte[] data = new byte[size.x * size.y];

        int result = QbarNative.TransBytes(pixels, data, size.x, size.y);
        if (result != 0) {
            Log.e(TAG, "rotate decodeResult " + result);
            callBack.afterDecode(DecodeCallback.CODE_GRAY_CROP_ERROR, null);
            return;
        }

        decodeInternal(data, size.x, size.y);
    }

    private void decodeInternal(byte[] data, int width, int height) {

        long startTime = System.currentTimeMillis();
        int result = qbarNative.scanImage(data, width, height, QbarNative.GRAY);//识别到二维码数量
        if (result < 0) {
            Log.e(TAG, "scanImage decodeResult " + result);
            callBack.afterDecode(DecodeCallback.CODE_NO_QRCODE, null);
            return;
        }

        //方式一：获取扫码结果
//        StringBuilder stype = new StringBuilder();
//        StringBuilder sdata = new StringBuilder();
//        StringBuilder sCharset = new StringBuilder();
//        StringBuilder sBinaryMethod = new StringBuilder();
//        int[] versionAndPylvrArray = new int[2];
//        decodeResult = qbarNative.GetOneResult(stype, sdata, sCharset, sBinaryMethod, versionAndPylvrArray);

        //方式二：获取扫码结果
        List<QbarNative.QBarResult> results = qbarNative.GetResults(1);
        if (results == null || results.size() == 0) {
            Log.e(TAG, "GetResults " + result);
            callBack.afterDecode(DecodeCallback.CODE_GET_QRCODE_ERROR, null);
            return;
        }

        callBack.afterDecode(DecodeCallback.CODE_SUCC, results.get(0).data);

//        List<QbarNative.QBarResult> results = new ArrayList<>();
//        List<QbarNative.QBarPoint> points = new ArrayList<>();
//        List<QbarNative.QBarReportMsg> rptmsgs = new ArrayList<>();
//        int ret_size = qbarNative.GetResults(MAX_CODE_NUM, results, points, rptmsgs);
//
//        //if (results == null || results.size() == 0) {
//        if (ret_size == 0) {
//            Log.e(TAG, String.format("get no results ,cost %dms", System.currentTimeMillis() - startTime));
//            callBack.afterDecode(null);
//            //return;
//        }
//
//        Log.i(TAG, String.format("get %d results ,cost %dms", results.size(), System.currentTimeMillis() - startTime));
//
//        StringBuilder stringBuilder = new StringBuilder();
//        int index = 0;
//        for (QbarNative.QBarResult resultObj : results) {
//            Log.i(TAG, String.format("decode type:%s, sCharset: %s, data:%s", resultObj.typeName, resultObj.charset, resultObj.data));
//            stringBuilder.append(index + " : " + resultObj.data + "\n");
//            index++;
//        }
//
//        for (QbarNative.QBarPoint point : points) {
//            Log.i(TAG, String.format("get point size %d x0 %f y0 %f x1 %f y1 %f", point.point_cnt, point.x0, point.y0, point.x1, point.y1));
//        }
//
//        for (QbarNative.QBarReportMsg rptmsg : rptmsgs) {
//            Log.i(TAG, String.format("rptmsg %s version %d, scalelist %s, decodescale %f detecttime %dms, sr time %dms",
//                    rptmsg.ecLevel, rptmsg.qrcodeVersion, rptmsg.scaleList, rptmsg.decodeScale, rptmsg.detectTime, rptmsg.srTime));
//        }
//
//        stringBuilder.append(',');
//        stringBuilder.deleteCharAt(stringBuilder.length() - 1);
//
//        // for codedetect
//        {
//            List<QbarNative.QBarCodeDetectInfo> detect_infos = new ArrayList<>();
//            List<QbarNative.QBarPoint> new_points = new ArrayList<>();
//            int rst = qbarNative.GetCodeDetectInfo(MAX_CODE_NUM, detect_infos, new_points);
//            Log.i(TAG, String.format("detect_size %d points %d", detect_infos.size(), new_points.size()));
//            if (new_points.size() > 0) {
//                Log.i(TAG, String.format("p1[%.0f,%.0f] ", new_points.get(0).x0, new_points.get(0).y0));
//                Log.i(TAG, String.format("p2[%.0f,%.0f] ", new_points.get(0).x1, new_points.get(0).y1));
//                Log.i(TAG, String.format("p3[%.0f,%.0f] ", new_points.get(0).x2, new_points.get(0).y2));
//                Log.i(TAG, String.format("p4[%.0f,%.0f] ", new_points.get(0).x3, new_points.get(0).y3));
//            }
//        }
//
//        //for zoom
//        {
//            QbarNative.QBarZoomInfo zoom_info = qbarNative.GetZoomInfo();
//            Log.i(TAG, String.format("iszoom %b factor %f", zoom_info.isZoom, zoom_info.zoomFactor));
//        }
//
//        callBack.afterDecode(stringBuilder.toString());
        //callBack.afterDecode(null);

    }

    private int setReaders() {
        int[] supportTypeSet = new int[5];
        supportTypeSet[0] = QbarNative.QRCODE;
        supportTypeSet[1] = QbarNative.ONED_BARCODE;
        supportTypeSet[2] = QbarNative.PDF417;
        supportTypeSet[3] = QbarNative.DATAMATRIX;
        supportTypeSet[4] = QbarNative.WXCODE;
        return qbarNative.setReaders(supportTypeSet, supportTypeSet.length);
    }

    public void release() {
        synchronized (syncObject) {
            inited = false;
            qbarNative.release();
        }
    }
}
