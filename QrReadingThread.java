package com.septem.a5dmarkv;

import android.content.Context;
import android.widget.Toast;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.GlobalHistogramBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

/**
 * Created by septem on 2016/11/1.
 */

public class QrReadingThread extends Thread {
    private Context context;
    private byte[] bytes;
    private int dataWidth;
    private int dataHeight;
    private int left;
    private int top;
    private int width;
    private int height;

    public QrReadingThread(Context context,
                           int dataWidth,
                           int dataHeight,
                           int left,
                           int top,
                           int width,
                           int height) {
        this.context = context;
        this.dataHeight = dataHeight;
        this.dataWidth = dataWidth;
        this.left = left;
        this.top = top;
        this.width = width;
        this.height = height;
    }

    public void getBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    @Override
    public void run() {
        PlanarYUVLuminanceSource yv =
                new PlanarYUVLuminanceSource(
                        bytes,
                        dataWidth,
                        dataHeight,
                        left,
                        top,
                        width,
                        height,
                        false);
        GlobalHistogramBinarizer bin =
                new GlobalHistogramBinarizer(yv);
        BinaryBitmap binaryBitmap = new BinaryBitmap(bin);
        QRCodeReader reader = new QRCodeReader();

        Toast.makeText(context," "+bytes.length,Toast.LENGTH_LONG);
        try {
            Result result = reader.decode(binaryBitmap);
        } catch (NotFoundException e) {
            Toast.makeText(context,"没有找到",Toast.LENGTH_LONG);
        } catch (ChecksumException e) {
            e.printStackTrace();
            Toast.makeText(context,"checksum failed",Toast.LENGTH_LONG);
        } catch (FormatException e) {
            e.printStackTrace();
            Toast.makeText(context,"format err",Toast.LENGTH_LONG);
        }
    }
}
