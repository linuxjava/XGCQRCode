package com.tencent.qbar;

import android.graphics.Bitmap;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class QbarNative {
    private static final String TAG = "QbarNative";
    //inner class for get decodeResult

    public static class QbarAiModelParam {
        public String detect_model_bin_path_;
        public String detect_model_param_path_;
        public String superresolution_model_bin_path_;
        public String superresolution_model_param_path_;
    }

    public static class QBarPoint {
        public int point_cnt;
        public float x0, x1, x2, x3;
        public float y0, y1, y2, y3;
    }

    public static class QBarReportMsg {
        public int qrcodeVersion;
        public int pyramidLv;
        public String binaryMethod;
        public String ecLevel;
        public String charsetMode;
        public String scaleList;
        public float decodeScale;
        public int detectTime;
        public int srTime;
    }

    public static class QBarResult {
        public int typeID;
        public String typeName;
        public String data;
        public byte[] rawData;
        public String charset;
    }
    public static class QBarResultJNI {
        public int typeID;
        public String typeName;
        public byte[] data;
        public String charset;
    }

    public static class QBarCodeDetectInfo {
        public int readerId;
        public float prob;
    }

    public static class QBarZoomInfo {
        public boolean isZoom;
        public float zoomFactor;
    }


    //private static QbarPossibleInfo possibleInfo = new  QbarPossibleInfo();

    /**
     * QBAR_SEARCH_MODE
     */
    public final static int SEARCH_ONE = 0;
    public final static int SEARCH_MULTI = 1;
    /**
     * QBAR_SPEED_MODE
     */
    public final static int FAST = 0;
    public final static int NORMAL = 1;
    public final static int TRYHARDER = 2;
    /**
     * QBAR_SCAN_MODE
     */
    public final static int SCAN_VIDEO = 0;
    public final static int SCAN_FILE = 1;
    /**
     * QBAR_READER
     */
    public final static int ALL_READERS = 0;
    public final static int ONED_BARCODE = 1;
    public final static int QRCODE = 2;
    public final static int WXCODE = 3;
    public final static int PDF417 = 4;
    public final static int DATAMATRIX = 5;
    /**
     * QBAR_COLOR
     */
    public final static int GRAY = 0;
    public final static int RGB = 1;
    public final static int RGBA = 2;
    /**
     * QBAR_ERROR_LEVEL
     */
    public final static int L = 0;
    public final static int M = 1;
    public final static int Q = 2;
    public final static int H = 3;
    /**
     * QBAR_CODE_FORMAT
     */
    public final static int AZTEC = 1;
    public final static int CODABAR = 2;
    public final static int CODE_39 = 3;
    public final static int CODE_93 = 4;
    public final static int CODE_128 = 5;
    public final static int DATA_MATRIX = 6;
    public final static int EAN_8 = 7;
    public final static int EAN_13 = 8;
    public final static int ITF = 9;
    public final static int MAXICODE = 10;
    public final static int PDF_417 = 11;
    public final static int QR_CODE = 12;
    public final static int RSS_14 = 13;
    public final static int RSS_EXPANDED = 14;
    public final static int UPC_A = 15;
    public final static int UPC_E = 16;
    public final static int UPC_EAN_EXTENSION = 17;
    public final static int WX_CODE = 18;
    public final static int CODE25 = 19;


    //buffer to get recognition decodeResult
    public byte[] type = new byte[100];
    public byte[] data = new byte[3000];
    public byte[] charset = new byte[100];
    public int[] sizeArr = new int[4];
    public byte[] binaryMethod = new byte[300];
    public int[] versionAndPyralv = new int[2];
    //variables for get decodeResult
    public int typeID;
    public int x;
    public int y;
    public int width;
    public int height;
    public int pointNum;

    private int qbarId = -1;

    static {
        System.loadLibrary("QBarMod");
    }

    /**
     * @param content    encode content
     * @param width      image code's width
     * @param height     image code's height
     * @param format     code format,currently only support QR_CODE,CODE_128
     * @param errorLevel L,M,Q,H
     * @param encode     encode charset
     * @param version    1-40
     * @param min_margin minimum margin for the image code
     * @return return the Bitmap if success,or null if failed
     */
    public static Bitmap encode(String content, int width, int height,
                                int format, int errorLevel, String encode, int version) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        int result = EncodeBitmap(content, bitmap, width, height, format, errorLevel, encode, version);
        if (result > 0) {
            return bitmap;
        } else {
            return null;
        }
    }

    public static String getVersion() {
        return GetVersion();
    }

    public static int encode(byte[] matrix, int[] size, String content, int format, int errorLevel, String encode, int version) {
        return Encode(matrix, size, content, format, errorLevel, encode, version);
    }

    public int init(int searchMode, int scanMode, String inputCharset, String outputCharset, QbarAiModelParam aiModelParam) {
        Log.i(TAG, "QBAR_ERR : init ");
        if (qbarId < 0) {
            if (aiModelParam != null) {
                qbarId = Init(searchMode, scanMode, inputCharset, outputCharset, aiModelParam);
            } else {
                qbarId = Init(searchMode, scanMode, inputCharset, outputCharset);
            }
        }
        if (qbarId < 0)
            return -1;
        else
            return 0;
    }

    public int setReaders(int[] readers, int len) {
        Log.i(TAG, "QBAR_ERR : init ");
        return SetReaders(readers, len, qbarId);
    }

    public int scanImage(byte[] grayImage, int width, int height, int mode) {
        Log.i(TAG, "QBAR_ERR scanimage ");
        ScanImage(grayImage, width, height, mode, qbarId);
        return GetResults(3).size() > 0 ? 0 : -1;
    }


    public int release() {
        int result = Release(qbarId);
        qbarId = -1;
        return result;
    }

    /**
     * @param stype content type
     * @param sdata content
     * @return if get decodeResult success,return>0,else = 0
     */
    public int GetOneResult(StringBuilder stype, StringBuilder sdata, StringBuilder sCharSet, StringBuilder sBinaryMethod, int[] versionAndPyralvArray) {
        int result = 0;
        result = GetOneResultReport(type, data, charset, binaryMethod, versionAndPyralv, sizeArr, qbarId);
        try {
            versionAndPyralvArray[0] = versionAndPyralv[0];
            versionAndPyralvArray[1] = versionAndPyralv[1];
            String charSet = new String(charset, 0, sizeArr[2], "UTF-8");
            sCharSet.append(charSet);
            if (charSet.equals("ANY")) {
                stype.append(new String(type, 0, sizeArr[0], "UTF-8"));
                sdata.append(new String(data, 0, sizeArr[1], "UTF-8"));
                sBinaryMethod.append(new String(binaryMethod, 0, sizeArr[3], "UTF-8"));
                if (sdata.length() == 0) {
                    stype.append(new String(type, 0, sizeArr[0], "ASCII"));
                    sdata.append(new String(data, 0, sizeArr[1], "ASCII"));
                    sBinaryMethod.append(new String(binaryMethod, 0, sizeArr[3], "ASCII"));
                }
            } else {
                stype.append(new String(type, 0, sizeArr[0], charSet));
                sdata.append(new String(data, 0, sizeArr[1], charSet));
                sBinaryMethod.append(new String(binaryMethod, 0, sizeArr[3], charSet));
            }
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "GetOneResult exp:" + e.getMessage());
        }
        return result;
    }

    public List<QBarResult> GetResults(int num) {
        if (num <= 0) {
            return null;
        }
        QBarResultJNI[] results = new QBarResultJNI[num];
        for (int i = 0; i < results.length; i++) {
            results[i] = new QBarResultJNI();
            results[i].charset = new String();
            results[i].data = new byte [1024];
            results[i].typeName = new String();
        }
        GetResults(results, qbarId);
        List<QBarResult> resultList = new ArrayList<>();
        try {
            for (QBarResultJNI resultJNI : results) {
                if (!Util.isNullOrNil(resultJNI.typeName)) {
                    QBarResult result = new QBarResult();
                    result.charset = resultJNI.charset;
                    result.typeID = resultJNI.typeID;
                    result.typeName = resultJNI.typeName;
                    result.rawData = resultJNI.data;
                    if (result.charset.equals("ANY")) {
                        result.data = new String(resultJNI.data,"UTF-8");
                        if (result.data.length() == 0) {
                            result.data = new String(resultJNI.data, "ASCII");
                        }
                    } else {
                        result.data = new String(resultJNI.data,result.charset);
                    }
                    resultList.add(result);
                }
            }
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "GetResults exp:" + e.getMessage());
        }
        return resultList;
    }

    public int GetResults(int num, List<QBarResult> qbar_result_list, List<QBarPoint> qbar_points_list, List<QBarReportMsg> qbar_report_msg_list) {
        if (num <= 0) {
            return 0;
        }
        QBarResultJNI[] results = new QBarResultJNI[num];
        QBarPoint[] points = new QBarPoint[num];
        QBarReportMsg[] rptmsgs = new QBarReportMsg[num];
        for (int i = 0; i < num; i++) {
            results[i] = new QBarResultJNI();
            results[i].charset = new String();
            results[i].data = new byte [1024];
            results[i].typeName = new String();

            points[i] = new QBarPoint();

            rptmsgs[i] = new QBarReportMsg();
            rptmsgs[i].binaryMethod = new String();
            rptmsgs[i].charsetMode = new String();
            rptmsgs[i].ecLevel = new String();
            rptmsgs[i].scaleList = new String();
        }

        qbar_result_list.clear();
        if(qbar_points_list != null) {
            qbar_points_list.clear();
        }
        qbar_report_msg_list.clear();

        GetDetailResults(results, points, rptmsgs, qbarId);

        try {
            for (QBarResultJNI resultJNI : results) {
                if (!Util.isNullOrNil(resultJNI.typeName)) {
                    QBarResult result = new QBarResult();
                    result.charset = resultJNI.charset;
                    result.typeID = resultJNI.typeID;
                    result.typeName = resultJNI.typeName;
                    result.rawData = resultJNI.data;
                    if (result.charset.equals("ANY")) {
                        result.data = new String(resultJNI.data,"UTF-8");
                        if (result.data.length() == 0) {
                            result.data = new String(resultJNI.data, "ASCII");
                        }
                    } else {
                        result.data = new String(resultJNI.data,result.charset);
                    }
                    qbar_result_list.add(result);
                }
            }
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "GetResults exp:" + e.getMessage());
        }

        if(qbar_points_list != null) {
            for (QBarPoint point : points) {
                if (point.point_cnt != 0) {
                    qbar_points_list.add(point);
                }
            }
        }

        for (QBarReportMsg rptmsg : rptmsgs) {
            if (!Util.isNullOrNil(rptmsg.charsetMode)) {
                qbar_report_msg_list.add(rptmsg);
            }
        }
        return qbar_result_list.size();
    }

    public int GetCodeDetectInfo(int num, List<QBarCodeDetectInfo> qbar_detect_info_list, List<QBarPoint> qbar_points_list) {
        if (num <= 0 || qbarId < 0) {
            return 0;
        }
        QBarCodeDetectInfo[] detect_infos = new QBarCodeDetectInfo[num];
        QBarPoint[] points = new QBarPoint[num];
        for (int i = 0; i < num; i++) {
            detect_infos[i] = new QBarCodeDetectInfo();
            points[i] = new QBarPoint();
        }

        qbar_detect_info_list.clear();
        qbar_points_list.clear();

        GetCodeDetectInfo(detect_infos, points, qbarId);

        for (QBarCodeDetectInfo detect_info : detect_infos) {
            if (detect_info.readerId > 0) {
                qbar_detect_info_list.add(detect_info);
            }
        }

        for (QBarPoint point : points) {
            if (point.point_cnt != 0) {
                qbar_points_list.add(point);
            }
        }

        return qbar_detect_info_list.size();
    }

    public int  GetDetectInfoByFrames(QBarCodeDetectInfo detectInfo, QBarPoint point) {
        return GetDetectInfoByFrames(detectInfo, point , qbarId);
    }

    public QBarZoomInfo GetZoomInfo() {
        QBarZoomInfo zoomInfo = new QBarZoomInfo();
        GetZoomInfo(zoomInfo, qbarId);

        return zoomInfo;
    }

    public int SetCenterCoordinate(int screenX, int screenY, int screenW, int screenH)
    {
        SetCenterCoordinate(screenX,screenY, screenW, screenH, qbarId);
        return 0;
    }

    private native static String GetVersion();

    private native static int Encode(byte[] matrix, int[] size, String content, int format, int errorLevel, String encode, int version);

    private native static int EncodeBitmap(String content, Bitmap bitmap, int width, int height, int format, int errorLevel, String encode, int version);

    private native int Init(int searchMode, int scanMode, String inputCharset, String outputCharset);

    private native int Init(int searchMode, int scanMode, String inputCharset, String outputCharset, QbarAiModelParam aiModelParam);

    private native int SetReaders(int[] readers, int len, int id);

    private native int ScanImage(byte[] grayImage, int width, int height, int mode, int id);

    private native int Release(int id);

    private native int GetOneResultReport(byte[] typeName, byte[] data, byte[] charset, byte[] binaryMethod, int[] versionAndPyralv, int[] sizeArr, int id);

    private native int GetOneResult(byte[] typeName, byte[] data, byte[] charset, int[] sizeArr, int id);

    private native int GetResults(QBarResultJNI[] results, int id);

    private native int GetDetailResults(QBarResultJNI[] results, QBarPoint[] points, QBarReportMsg[] rptmsg, int id);

    private native int GetCodeDetectInfo(QBarCodeDetectInfo[] detect_infos, QBarPoint[] points, int id);

    private native int GetDetectInfoByFrames(QBarCodeDetectInfo detect_info, QBarPoint points,int id);

    private native int GetZoomInfo(QBarZoomInfo zoom_info, int id);

    private native int SetCenterCoordinate(int screenX, int screenY, int screenW, int screenH, int id);

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////
    public final static int ROTATE_0 = 0;
    public final static int ROTATE_90 = 90;
    public final static int ROTATE_180 = 180;
    public final static int ROTATE_270 = 270;

    public final static int SUBSAMPLE = 1;
    public final static int NOSUBSAMPLE = 0;

    /**
     * 对一幅输入图像做灰度化、旋转或下采样处理，并保存为输出图像。输入图像是灰度或彩色图像均可。
     *
     * @param outImg      输出图像，yuv格式的字节数组，经过处理的灰度图像。
     * @param outImgSize  输出图像的长和宽 注意：数组大小一定要等于width*height*3/2。
     * @param yuvImg      输入图像，yuv格式的字节数组，灰度或彩色图像均可。 注意：未下采样时，数组大小一定要等于width*height*3/2。
     *                    下采样时，数组大小一定要等于width/2*height/2*3/2。
     * @param width       输入图像的宽度。
     * @param height      输入图像的高度。
     * @param angle       将输入图像顺时针旋转0,90,180或270度
     * @param ifsubsample 是否需要将输入图像进行2倍下采样，是：1，否：0。
     * @return 参数错误：-1 图像处理成功：0
     */
    public static int gray_rotate_sub(byte[] outImg, int[] outImgSize,
                                      byte[] yuvImg, int width, int height, int angle, int ifsubsample) {
        if (outImg == null || yuvImg == null) {
            return -1;
        }
        return nativeGrayRotateCropSub(yuvImg, width, height, 0, 0, width,
                height, outImg, outImgSize, angle, ifsubsample);
    }

    /**
     * 对一幅输入图像的指定区域做灰度化、旋转或下采样处理，并保存为输出图像。输入图像是灰度或彩色图像均可。
     *
     * @param outImg      输出图像，yuv格式的字节数组，经过处理的灰度图像。 注意：数组大小一定要等于width*height*3/2。
     * @param outImgSize  输出图像的长和宽
     * @param yuvImg      输入图像，yuv格式的字节数组，灰度或彩色图像均可。
     *                    注意：未下采样时，数组大小一定要等于(cropWidth)*(cropHeight)*3/2。
     *                    下采样时，数组大小一定要等于(cropWidth)/2*(cropHeight)/2*3/2。
     * @param width       输入图像的宽度。
     * @param height      输入图像的高度。
     * @param left        选择区域的左边界，[0,width-1]，注意：左边界必须比右边界小。
     * @param top         选择区域的上边界，[0,height-1]，注意：上边界必须比下边界小。
     * @param cropWidth   选择区域的宽度。
     * @param cropHeight  选择区域的高度。
     * @param angle       是否需要将输入图像顺时针旋转0,90,180或270度
     * @param ifsubsample 是否需要将输入图像进行2倍下采样，是：1，否：0。
     * @return 参数错误：-1 图像处理成功：0
     */
    public static int gray_rotate_crop_sub(byte[] outImg, int[] outImgSize,
                                           byte[] yuvImg, int width, int height, int left, int top,
                                           int cropWidth, int cropHeight, int angle, int ifsubsample) {
        if (outImg == null || yuvImg == null)
            return -1;

        return nativeGrayRotateCropSub(yuvImg, width, height, left, top,
                cropWidth, cropHeight, outImg, outImgSize, angle, ifsubsample);
    }

    /**
     * 对输入的图像做顺时针旋转90度操作，输入图像是灰度或彩色图像均可。注意，不会对图像做灰度化处理,图像长宽必须是8的倍数。
     *
     * @param outImg 输出图像，yuv格式的字节数组，经过旋转的图像。
     * @param yuvImg 输入图像，yuv格式的字节数组，灰度或彩色图像均可。
     * @param width  输入图像的宽度。
     * @param height 输入图像的高度。
     * @return 参数错误：-1 图像处理成功：0
     */
    public static int YUVrotate(byte[] outImg, byte[] yuvImg, int width,
                                int height) {
        if (outImg == null || yuvImg == null) {
            return -1;
        }
        return nativeYUVrotate(outImg, yuvImg, width, height);
    }

    /**
     * 对输入的图像原地做顺时针旋转90度操作，输入图像是灰度或彩色图像均可。注意，不会对图像做灰度化处理,图像长宽必须是8的倍数。
     *
     * @param yuvImg 待处理图像，yuv格式的字节数组，灰度或彩色图像均可。
     * @param width  输入图像的宽度。
     * @param height 输入图像的高度。
     * @return 参数错误or内存分配失败：-1 图像处理成功：0
     */
    public static int YUVrotateLessMemCost(byte[] yuvImg, int width, int height) {
        if (yuvImg == null) {
            return -1;
        }
        return nativeYUVrotateLess(yuvImg, width, height);
    }

    /**
     * 对输入的yuv图像做灰度化，并返回int数组形式的灰度图像，用于创建bitmap
     *
     * @param outImg 输出图像，ARGB格式的整形数组。
     * @param yuvImg 输入图像，yuv格式的字节数组，灰度或彩色图像均可。
     * @param width  输入图像的宽度。
     * @param height 输入图像的高度。
     * @return 参数错误：-1 图像处理成功：0
     */
    public static int TransPixels(int[] outImg, byte[] yuvImg, int width,
                                  int height) {
        if (outImg == null || yuvImg == null) {
            return -1;
        }
        return nativeTransPixels(outImg, yuvImg, width, height);
    }

    /**
     * 对输入的RGB图像做灰度化，并返回byte数组形式的灰度图像，用于将bitmap转化为byte数组
     *
     * @param inImg  输入图像，ARGB格式的整形数组。
     * @param outImg 输出图像，字节数组的灰度图像。
     * @param width  输入图像的宽度。
     * @param height 输入图像的高度。
     * @return 参数错误：-1 图像处理成功：0
     */
    public static int TransBytes(int[] inImg, byte[] outImg, int width,
                                 int height) {
        if (inImg == null || outImg == null)
            return -1;
        return nativeTransBytes(inImg, outImg, width, height);
    }

    /**
     * byte数组转int数组 or int数组转byte数组
     *
     * @param intTobyte 0：int转byte。1：byte转int
     * @param byteArr   字节数组。
     * @param intArr    整型数组。
     * @return 参数错误：-1 处理成功：0
     */
    public static int ArrayConvert(int intTobyte, byte[] byteArr, int[] intArr) {
        if (byteArr == null || intArr == null)
            return -1;
        return nativeArrayConvert(intTobyte, byteArr.length, byteArr, intArr);
    }

    /**
     * 对当前图像做灰度化和下采样（可选）处理
     *
     * @param dstYuvImg   输出图像
     * @param srcImg      输入图像
     * @param width       图像宽带
     * @param height      图像高度
     * @param ifsubsample 是否下采样，1：是，0：否
     * @return 参数错误：-1 处理成功：0
     */
    public static int CropGray2(byte[] dstYuvImg, byte[] srcImg, int width,
                                int height, int ifsubsample) {
        if (dstYuvImg == null || srcImg == null)
            return -1;
        return nativeCropGray2(dstYuvImg, srcImg, width, height, ifsubsample);
    }

    /**
     * 裁剪输入图像的部分区域并输出为int数组
     *
     * @param wholeImgYuv    输入YUV图像
     * @param outIntArray    输出int数组
     * @param wholeImgWidth  输入图像宽度
     * @param wholeImgHeight 输入图像高度
     * @param rectLeft       区域左边界坐标
     * @param rectTop        区域上边界坐标
     * @param rectWidth      区域宽度
     * @param rectHeight     区域高度
     * @return 参数错误：-1 处理成功：0
     */
    public static int YuvToCropIntArray(byte[] wholeImgYuv, int[] outIntArray,
                                        int wholeImgWidth, int wholeImgHeight, int rectLeft, int rectTop,
                                        int rectWidth, int rectHeight) {
        if (wholeImgYuv == null || outIntArray == null) {
            return -1;
        }
        return nativeYuvToCropIntArray(wholeImgYuv, outIntArray, wholeImgWidth,
                wholeImgHeight, rectLeft, rectTop, rectWidth, rectHeight);
    }

    /*************************** native 方法 ****************************************************/
    private static native int nativeGrayRotateCropSub(byte[] srcImg, int width,
                                                      int height, int left, int top, int cropWidth, int cropHeight,
                                                      byte[] dstImg, int[] dstImgSize, int angle, int ifsubsample);

    private static native int nativeCropGray2(byte[] outImg, byte[] yuvImg,
                                              int width, int height, int ifsubsample);

    private static native int nativeYUVrotate(byte[] outImg, byte[] yuvImg,
                                              int width, int height);

    private static native int nativeYUVrotateLess(byte[] yuvImg, int width,
                                                  int height);

    private static native int nativeTransPixels(int[] outImg, byte[] yuvImg,
                                                int width, int height);

    private static native int nativeTransBytes(int[] inImg, byte[] outImg,
                                               int width, int height);

    private static native int nativeArrayConvert(int intTobyte, int length,
                                                 byte[] yuvImg, int[] outImg);

    private static native int nativeYuvToCropIntArray(byte[] wholeImgYuv,
                                                      int[] outIntArray, int width, int height, int rectLeft,
                                                      int rectTop, int rectWidth, int rectHeight);

    /**
     * 释放以上函数需要的资源
     *
     * @return
     */
    public static native int nativeRelease();
    /*************************************** 用于扫封面和翻译的接口 **********************************************************/
    /***************************************************************
     * 扫封面和翻译引擎初始化
     *
     * @param imgWidth
     *            : 输入图像宽度
     * @param imgHeight
     *            : 输入图像高度
     * @param _isForOCR
     *            : 是否是扫单词，扫封面：false，翻译：true
     * @param _bestImgThreshold
     *            : 最清晰图像阈值
     * @param _sameSceneDiff
     *            : 同一场景阈值
     * @return
     */
    public static native int FocusInit(int width, int height, boolean isForOCR,
                                       int bestImgThreshold, int sameSceneDiff);

    /**
     * 扫封面和翻译引擎调用
     *
     * @param imgData  输入YUV图像
     * @param noRotate 是否需要旋转图像
     * @param paraArr  返回参数
     * @return
     */
    public static native boolean FocusPro(byte[] imgData, boolean noRotate,
                                          boolean[] paraArr);

    /**
     * 扫封面和翻译引擎释放
     */
    public static native int FocusRelease();

    /*************************************** 用于银行卡识别的接口 **********************************************************/
    /****************************************************************
     * 快速YUV格式图像裁剪
     *
     * @param dst_img
     *            : 裁剪后的图像数组
     * @param src_img
     *            : 源图像的数组
     * @param srcWidth
     *            : 源图像宽度
     * @param srcHeight
     *            : 源图像高度
     * @param cropX
     *            : 裁剪区域最左边x坐标
     * @param cropY
     *            : 裁剪区域最上面y坐标
     * @param cropWidth
     *            : 裁剪区域宽度
     * @param cropHeight
     *            : 裁剪区域高度
     * @return 0裁剪成功，-1裁剪失败
     */
    public static native int QIPUtilYUVCrop(byte[] dst_img, byte[] src_img,
                                            int srcWidth, int srcHeight, int cropX, int cropY, int cropWidth,
                                            int cropHeight);

    /***************************************************************
     * 扫银行卡 清晰帧检测算法初始化
     *
     * @param width
     *            : 输入图像宽度
     * @param height
     *            : 输入图像高度
     * @param bestImgThreshold
     *            : 最清晰图像阈值
     * @param cardWidth
     *            : 图像银行卡部分宽度
     * @param cardHeight
     *            : 图像银行卡部分高度
     * @param isPortrait
     *            : 是否是竖屏幕
     * @return: 0初始化成功，-1初始化失败
     */
    public static native int focusedEngineForBankcardInit(int width,
                                                          int height, int bestImgThreshold, boolean isPortrait);

    /***************************************************************
     * 清晰帧检测算法释放
     *
     * @return 0成功， -1失败
     */
    public static native int focusedEngineRelease();

    /***************************************************************
     * 清晰帧检测算法
     *
     * @param src_img
     *            : 输入图像源图像
     * @return: 0，采集的帧数不够，需要继续采集下一帧。 2，不清晰，需要调用系统对焦 1，清晰的一帧，可以进入识别流程 -1:失败
     */
    public static native int focusedEngineProcess(byte[] src_img);

    /**
     * 清晰帧检测算法返回版本号
     *
     * @return
     */
    public static native int focusedEngineGetVersion();
}
