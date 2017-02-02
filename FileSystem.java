package com.septem.a5dmarkv;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by septem on 2016/9/19.
 * 文件操作相关方法，配置文件的读取/保存，媒体文件的保存等
 */
public class FileSystem {
    private static final String APP_DIR_NAME = "5DMarkV";


    /**
     * 获得一个日期时间戳，格式是“yyyyMMdd_HHmm”+3位，最后3位是精确到0.1的秒
     * 这样做是为了避免在秒单位相同导致的同名。
     * @return
     */
    public static String getDateTimeMark() {
        long timeMillis = System.currentTimeMillis();
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        int tenthSeconds = (int)(timeMillis%1000)/100;
        return timestamp+tenthSeconds;
    }

    public static String getOutputDir() {
        String dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                .toString()+"/"+ APP_DIR_NAME;
        return dir;
    }
    /**
     * 获取保存JPEG文件的路径
     * @return 保存文件的路径，string
     */
    public static String getJpegFileName() {
        return getOutputDir()+"/"+getDateTimeMark()+".jpg";
    }

    /**
     * 获取保存mp4文件的路径
     * @return 保存文件的路径，string
     */
    public static String getMP4FileName() {
        return getOutputDir()+"/"+getDateTimeMark()+".mp4";
    }

    /**
     * 保存jpeg文件
     * @param b 被保存的数据
     * @return 保存图片成功，则返回文件路径；保存失败，则返回字符串"err"
     */
    public static String saveJpegFile(byte[] b) {
        if(!new File(getOutputDir()).exists()){
            Log.d("sepLog","mkdir");
            new File(getOutputDir()).mkdir();}
        File jpegImage = new File(getJpegFileName());
        try {
            FileOutputStream fos = new FileOutputStream(jpegImage);
            fos.write(b);
            fos.close();
            return jpegImage.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "err";
        }
    }


    /**
     * 将照片添加到gallery，这样其他的app可以马上查询到照片
     * @param filePath
     */
    public static void addToGallery(Context context,String filePath) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File file = new File(filePath);
        Uri contentUri = Uri.fromFile(file);
        mediaScanIntent.setData(contentUri);
        context.sendBroadcast(mediaScanIntent);
    }

    /**
     * 存储文本内容到txt文件
     * @param content 要存储的文本内容
     * @param filePath 文件路径
     * @throws Exception IOException
     */
    public static void saveTxtFile(String content, String filePath) throws Exception {
        FileOutputStream fout = new FileOutputStream(filePath);
        OutputStreamWriter writer = new OutputStreamWriter(fout);
        writer.write(content);
        writer.close();
    }
}
