package com.xgc.qrcode.demo.decode;

public interface DecodeCallback {
    /**
     * 二维码解码成功回调(UI线程)
     * @param data
     */
    void decodeResult(String data);
}
