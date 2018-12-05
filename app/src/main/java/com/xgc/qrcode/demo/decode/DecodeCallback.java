package com.xgc.qrcode.demo.decode;

public interface DecodeCallback {
    public static final int CODE_SUCC = 0;//解码成功
    public static final int CODE_DATA_NULL = -1;//输入数据为空
    public static final int CODE_GRAY_CROP_ERROR = -2;//数据灰度裁剪错误
    public static final int CODE_NO_QRCODE = -3;//没有找到二维码
    public static final int CODE_GET_QRCODE_ERROR = -4;//解析二维码错误

    /**
     * 二维码解码成功回调(UI线程)
     *
     * @param data
     */
    void decodeResult(int code, final String data);
}
