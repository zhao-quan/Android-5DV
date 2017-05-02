package com.septem.a5dmarkv;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.AnimationDrawable;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.GlobalHistogramBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private final String TAG = "sepLog";
    //变量区
    private Activity thisActivity = this;
    private final int CAMERA_PERMISSION = 1;
    private final int EXTERNAL_STORAGE_PERMISSION = 2;
    private final int AUDIO_RECORD_PERMISSION = 3;
    private int originalScreenBrightnessMode;
    private int originalScreenBrightness = 200;

    private int cameraId = 0;
    private int numOfCameras;
    private Camera mCamera ;
    private Window window;
    private WindowManager.LayoutParams windowLayoutParams;
    //camera parameters
    private Camera.Parameters mCameraParameters;
    private List<Camera.Size> supportedPreviewSizes;
    private List<Camera.Size> supportedPhotoSizes;
    private List<String> supportedFocusMode;
    private List<String> supportedFlashMode;
    private List<String> supportedSceneMode;
    private List<String> supportedWhiteBalance;
    private int jpegQuality = 97;
    //settings
    private boolean widePhotoFront = true;
    private boolean widePhotoRear = false;
    private boolean isWidePhoto = false;
    private String flashMode;
    private String focusMode;
    private String sceneMode;
    private String whiteBalance;

    private Handler mHandler;
    private SensorManager mSensorManager;
    private WindowManager wm;
    private TextureView mTextureView;
    private ScreenMaskView mScreenMaskView;
    private ImageButton shutterBtn;
    private ImageButton switchBtn;
    private ImageButton settingBtn;
    private ImageButton flashBtn;
    private ImageButton qrModeBtn;
    private ImageButton videoBtn;
    private boolean isPreviewing = false;
    private boolean isRecording = false;
    private MediaRecorder mMediaRecorder;
    private String videoFileName;

    private boolean isQrMode = false;
    //areaSize表示读取QR CODE区域的边长
    //areaYOffset表示从屏幕正中间往Y方向的偏移量
    private int qrAreaSize = 600;
    private int qrAreaYOffset = -200;
    private byte[] previewBytes;

    private int mTextureViewYOffset = -70;
    private int orientation = 90;
    private Camera.Size previewSize;
    private Camera.Size photoSize;
    private final float[] gravity = new float[3];


    private SensorEventListener mSensorEventListener =
            new SensorEventListener() {
                @Override
                public void onSensorChanged(SensorEvent sensorEvent) {
                    switch (sensorEvent.sensor.getType()){
                        case Sensor.TYPE_ACCELEROMETER:
                            gravity[0] = sensorEvent.values[0];
                            gravity[1] = sensorEvent.values[1];
                            gravity[2] = sensorEvent.values[2];
                            orientation = getPhotoOrientation(gravity[0],gravity[1]);
                            break;
                    }
                }

                @Override
                public void onAccuracyChanged(Sensor sensor, int i) {

                }
            };
    //以上是变量区

    /**
     * 测试用方法，在没有测试的时候清空
     */
    private void onTesting() {

    }
    //Override区
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume() {
        super.onResume();

        requestPermissions();
    }

    @Override
    protected void onPause() {
        if(isRecording)
            stopRecording();

        restoreBrightness();
        releaseAll();

        super.onPause();
    }

    @Override
    protected void onStop() {

        super.onStop();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case CAMERA_PERMISSION:
                if(grantResults.length>0 &&
                        grantResults[0] == PackageManager.PERMISSION_DENIED)
                {
                    if(thisActivity!=null)
                        thisActivity.finish();
                }
                break;
            case EXTERNAL_STORAGE_PERMISSION:
                if(grantResults.length>0 &&
                        grantResults[0] == PackageManager.PERMISSION_DENIED)
                {
                    /*new AlertDialog.Builder(thisActivity)
                            .setMessage(R.string.requireWriteStorageFailed)
                            .setPositiveButton("确定",null)
                            .create().show();*/
                    if(thisActivity!=null)
                        thisActivity.finish();
                }
                break;
            case AUDIO_RECORD_PERMISSION:
                if(grantResults.length>0 &&
                        grantResults[0] == PackageManager.PERMISSION_DENIED)
                {
                    if(thisActivity!=null)
                        thisActivity.finish();
                }
                break;
            default:
                requestPermissions();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if(isPreviewing&&cameraId==0
                        &&isInView(mTextureView,(int)event.getX(),(int)event.getY())) {
                    mScreenMaskView.startFocus(event.getX(),event.getY());
                    startFocus((int) event.getX(), (int) event.getY());
                }
                break;
        }

        return super.onTouchEvent(event);
    }

    //以上是Override区

    /**
     * 获得基本组件的instance
     */
    private void getWidgetInstances() {
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mTextureView = (TextureView)findViewById(R.id.preview_texture);
        mScreenMaskView = (ScreenMaskView)findViewById(R.id.mask_view);
        shutterBtn = (ImageButton)findViewById(R.id.shutter_button);
        settingBtn = (ImageButton)findViewById(R.id.setting_button);
        switchBtn = (ImageButton)findViewById(R.id.switch_camera_button);
        flashBtn = (ImageButton)findViewById(R.id.flash_button);
        qrModeBtn = (ImageButton)findViewById(R.id.qr_button);
        videoBtn = (ImageButton)findViewById(R.id.video_button);
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        mHandler = new Handler();

        shutterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                shutterClicked();
            }
        });
        settingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                settingClicked();
            }
        });
        switchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switchCameraClicked();
            }
        });
        flashBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                flashClicked();
            }
        });
        qrModeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                qrClicked();
            }
        });
        videoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                videoClicked();
            }
        });
    }

    /**
     * 传感器开始活动,用于记录屏幕是横屏还是竖屏.
     */
    private void startSensor() {
        mSensorManager.registerListener(mSensorEventListener,
                mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    /**
     * 获取camera.parameters的设备信息，比如输出尺寸，支持的对焦模式，等等
     */
    private void getCameraParameters() {
        numOfCameras = Camera.getNumberOfCameras();
        mCameraParameters = mCamera.getParameters();
        supportedPreviewSizes = mCameraParameters.getSupportedPreviewSizes();
        supportedPhotoSizes = mCameraParameters.getSupportedPictureSizes();
        supportedFocusMode = mCameraParameters.getSupportedFocusModes();
        supportedFlashMode = mCameraParameters.getSupportedFlashModes();
        supportedSceneMode = mCameraParameters.getSupportedSceneModes();
        supportedWhiteBalance = mCameraParameters.getSupportedWhiteBalance();
    }

    /**
     * 获得权限
     */
    private void requestPermissions() {
        //试图获取摄像头权限
        if(ActivityCompat.checkSelfPermission(thisActivity,Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(thisActivity,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION);
        }
        //试图获取写入设定的权限
        /*else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(!Settings.System.canWrite(this)) {
                new AlertDialog.Builder(thisActivity).setMessage(R.string.whyRequireSetting)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                                startActivity(intent);
                            }
                        }).setNegativeButton(R.string.no,null)
                        .create().show();
            }
        }*/
        //试图写入麦克风权限
        if(ActivityCompat.checkSelfPermission(thisActivity,Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(thisActivity,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    AUDIO_RECORD_PERMISSION);
        }

        //试图获取写入文件的权限
        else if(ActivityCompat.checkSelfPermission(thisActivity,Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(thisActivity,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    EXTERNAL_STORAGE_PERMISSION);
        }

        //正常启动
        else
            startAvalible();
    }

    /**
     * 释放所有组件
     */
    private void releaseAll() {
        //unregister sensor listener
        if(mSensorManager!=null) {
            mSensorManager.unregisterListener(mSensorEventListener);
        }
        if(mSensorManager!=null)
            mSensorManager = null;
        //release camera
        if(mCamera!=null)
        {
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
    }
    /**
     * 设置亮度到最大,并且保持屏幕常亮
     */
    private void setBrightnessMax() {
        window = getWindow();
        windowLayoutParams = window.getAttributes();
        windowLayoutParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL;
        window.setAttributes(windowLayoutParams);
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    /**
     * 还原亮度/亮度模式
     */
    private void restoreBrightness() {
        if(windowLayoutParams!=null) {
            windowLayoutParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
            window.setAttributes(windowLayoutParams);
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    /**
     * 通过加速橦暗器得到的x,y方向值计算出屏幕的方向，是横屏还是竖屏
     * @param x
     * @param y
     * @return 屏幕的角度，正常竖屏为90，逆时针旋转90度为横屏0，倒转为270，顺时针90度为180
     */
    private int getPhotoOrientation(float x,float y) {
        if(Math.abs(x)>Math.abs(y))
        {
            if(x>0)return 0;
            else return 180;
        }else {
            if(y>0)return 90;
            else return 270;
        }
    }

    /**
     * 判断某个坐标是否在view范围内
     * @param view 被判断的view
     * @param x 坐标的x
     * @param y 坐标的y
     * @return true of false
     */
    private boolean isInView(View view,int x,int y) {
        int[] viewLocation = new int[2];
        view.getLocationOnScreen(viewLocation);
        int locationX = viewLocation[0];
        int locationY = viewLocation[1];
        int viewWidth = view.getWidth();
        int viewHeight = view.getHeight();
        if(x>locationX&&x<(locationX+viewWidth)&&y>locationY&&y<(locationY+viewHeight))
            return true;
        else return false;

    }

    /**
     * 快门按钮按下事件
     */
    private void shutterClicked() {
        mCameraParameters.setPictureSize(photoSize.width,photoSize.height);
        mCameraParameters.setJpegQuality(jpegQuality);
        if(cameraId==1){
            mCameraParameters.setRotation((360- orientation)%360);
        }
        else mCameraParameters.setRotation(orientation);
        mCamera.setParameters(mCameraParameters);

        mScreenMaskView.shutterClicked();
        isPreviewing = false;
        mCamera.takePicture(null, null, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                mCamera.cancelAutoFocus(); //这一句很关键
                mCamera.startPreview();
                isPreviewing = true;
                //恢复对焦模式，前摄像头不需要
                if(cameraId==0) {
                    mCameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                    mCameraParameters.setFocusAreas(null);
                    mCamera.setParameters(mCameraParameters);
                }
                //保存jpg照片
                String status = FileSystem.saveJpegFile(data);
                if(status.equals("err"))
                    Toast.makeText(thisActivity, "保存图片文件失败", Toast.LENGTH_SHORT).show();
                else {
                    FileSystem.addToGallery(thisActivity,status);
                }
            }
        });
    }

    /**
     * 设定按钮
     */
    private void settingClicked() {
        Toast.makeText(thisActivity, "do nothing", Toast.LENGTH_SHORT).show();
    }

    /**
     * 闪光灯模式按钮
     */
    private void flashClicked() {
        int size = supportedFlashMode.size();
        int index = supportedFlashMode.indexOf(flashMode);
        if(index<size-1)
            index ++;
        else index = 0;
        flashMode = supportedFlashMode.get(index);
        setFlashMode();
    }

    /**
     * 读取QR code按钮
     */
    private void qrClicked() {
        if(cameraId!=0)
        {
            Toast.makeText(thisActivity,"切换到后置摄像头才能读取二维码",Toast.LENGTH_LONG).show();
            return;
        }
        if(!isQrMode)
        {
            isQrMode = true;
            //因为textureView在4：3模式下有位移，所以，也需要加上这个位移“mTextureViewYOffset”
            mScreenMaskView.startQRreading(qrAreaSize,qrAreaYOffset+mTextureViewYOffset);
            mCamera.setPreviewCallback(new Camera.PreviewCallback() {
                @Override
                public void onPreviewFrame(byte[] data, Camera camera) {
                    previewBytes = data;
                }
            });
            shutterBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    doQrReading();
                }
            });
            switchBtn.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_forbidden));
            switchBtn.setOnClickListener(null);
        }else {
            isQrMode = false;
            mScreenMaskView.stopQRreading();
            mCamera.setPreviewCallback(null);
            shutterBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    shutterClicked();
                }
            });
            switchBtn.setImageDrawable(getResources().getDrawable(R.drawable.switch_camera));
            switchBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    switchCameraClicked();
                }
            });
        }
    }

    /**
     * 开始录制视频按钮
     */
    private void videoClicked() {
        //set textureView params
        ViewGroup.LayoutParams lp = mTextureView.getLayoutParams();
        Point point = new Point();
        wm.getDefaultDisplay().getSize(point);
        lp.width = point.x;
        lp.height = lp.width * 16/9;
        mTextureView.setTranslationY(0);
        mTextureView.setLayoutParams(lp);

        switchBtn.setAlpha(0f);
        switchBtn.setEnabled(false);
        qrModeBtn.setAlpha(0f);
        qrModeBtn.setEnabled(false);
        videoBtn.setAlpha(0f);
        videoBtn.setEnabled(false);
        shutterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopRecording();
                //重新打开摄像头，其实这里跟switchCamera里面一样，但是不改变cameraId。
                mScreenMaskView.setScreenMaskListener(new ScreenMaskListener() {
                    @Override
                    public void fadeComplete() {
                        mCamera.setPreviewCallback(null);
                        mCamera.stopPreview();
                        mCamera.release();
                        openCamera();
                    }

                    @Override
                    public void emergeComplete() {

                    }
                });
                mScreenMaskView.blackMask();
            }
        });
        shutterBtn.setImageDrawable(getDrawable(R.drawable.recording_animation));
        AnimationDrawable ad = (AnimationDrawable) shutterBtn.getDrawable();
        ad.start();

        isRecording = true;

        doRecord();
    }

    /**
     * 停止录制视频
     */
    private void stopRecording() {
        switchBtn.setAlpha(1f);
        switchBtn.setEnabled(true);
        qrModeBtn.setAlpha(1f);
        qrModeBtn.setEnabled(true);
        videoBtn.setAlpha(1f);
        videoBtn.setEnabled(true);
        shutterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shutterClicked();
            }
        });
        shutterBtn.setImageDrawable(getResources().getDrawable(R.drawable.shutter_icon_2));

        isRecording = false;
        mMediaRecorder.stop();
        mMediaRecorder.reset();
        mMediaRecorder.release();
        mCamera.lock();
        FileSystem.addToGallery(thisActivity,videoFileName);
    }

    /**
     * 录制视频
     */
    private void doRecord() {
        //mCameraParameters.setRecordingHint(true);
        //mCamera.setParameters(mCameraParameters);

        mMediaRecorder = new MediaRecorder();
        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        if(CamcorderProfile.hasProfile(cameraId,CamcorderProfile.QUALITY_1080P))
            mMediaRecorder.setProfile(CamcorderProfile.get(cameraId,CamcorderProfile.QUALITY_1080P));
        else mMediaRecorder.setProfile(CamcorderProfile.get(cameraId,CamcorderProfile.QUALITY_480P));
        videoFileName = FileSystem.getMP4FileName();
        mMediaRecorder.setOutputFile(videoFileName);
        //设置orientation。后置摄像头是一样的
        //但是前置摄像头貌似有区别，如果按照照片的设置，前置摄像头拍的视频是倒过来的。
        //这一点很奇怪。
        if(cameraId==0)
            mMediaRecorder.setOrientationHint(orientation);
        else {
            int ori = 360-orientation;
            ori = (ori==360)? 0:ori;
            mMediaRecorder.setOrientationHint(ori);
        }
        mMediaRecorder.setPreviewDisplay(new Surface(mTextureView.getSurfaceTexture()));
        try {
            mMediaRecorder.prepare();
            mMediaRecorder.start();
        } catch (IOException e) {
            e.printStackTrace();
            mMediaRecorder.release();
            Toast.makeText(thisActivity,"Record failed",Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 读取QR CODE信息。
     * <br>
     * <br>因为从摄像头获取的数据是没有旋转过的原始数据，所以界面上的YOffset,在图像数据上
     * 体现为X方向上的Offset。所以在设置切割区域的时候需要把YOffset设置到X方向上。
     */
    private void doQrReading() {
        PlanarYUVLuminanceSource yv =
                new PlanarYUVLuminanceSource(
                        previewBytes,
                        previewSize.width,
                        previewSize.height,
                        previewSize.width/2-qrAreaSize/2+qrAreaYOffset,
                        previewSize.height/2-qrAreaSize/2,
                        qrAreaSize,
                        qrAreaSize,
                        false);
        GlobalHistogramBinarizer bin =
                new GlobalHistogramBinarizer(yv);
        BinaryBitmap binaryBitmap = new BinaryBitmap(bin);
        QRCodeReader reader = new QRCodeReader();
        try {
            final Result result = reader.decode(binaryBitmap);
            android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(thisActivity);
            builder.setMessage(result.getText());
            builder.setPositiveButton("打开", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(result.getText()));
                    if (intent.resolveActivity(getPackageManager()) != null) {
                        startActivity(intent);
                    }

                }
            });
            builder.setNegativeButton("取消",null);
            builder.create().show();
        } catch (NotFoundException e) {
            Toast.makeText(thisActivity,"没有找到",Toast.LENGTH_LONG).show();
        } catch (ChecksumException e) {
            e.printStackTrace();
            Toast.makeText(thisActivity,"checksum failed",Toast.LENGTH_LONG).show();
        } catch (FormatException e) {
            e.printStackTrace();
            Toast.makeText(thisActivity,"format err",Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 转换摄像头按钮
     */
    private void switchCameraClicked() {
        mScreenMaskView.setScreenMaskListener(new ScreenMaskListener() {
            @Override
            public void fadeComplete() {
                mCamera.setPreviewCallback(null);
                cameraId = (cameraId==1)?0:1;
                mCamera.stopPreview();
                mCamera.release();
                openCamera();
            }

            @Override
            public void emergeComplete() {

            }
        });
        mScreenMaskView.blackMask();
    }

    /**
     * 设置输出和预览格式，16:9 or 4:3
     */
    private void setPhotoSize() {
        if(cameraId==0)
            isWidePhoto = widePhotoRear;
        else isWidePhoto = widePhotoFront;
        Point ratio = new Point();
        if(isWidePhoto) {
            ratio.x = 16;
            ratio.y = 9;
        }
        else {
            ratio.x = 4;
            ratio.y = 3;
        }
        for(Camera.Size size : supportedPreviewSizes)
        {
            if(size.height*ratio.x/ratio.y==size.width) {
                previewSize = size;
                break;
            }
        }
        for(Camera.Size size : supportedPhotoSizes)
        {
            if(size.height*ratio.x/ratio.y==size.width) {
                photoSize = size;
                break;
            }
        }

        ViewGroup.LayoutParams lp = mTextureView.getLayoutParams();
        Point point = new Point();
        wm.getDefaultDisplay().getSize(point);
        lp.width = point.x;
        if(isWidePhoto){
            lp.height = lp.width * 16/9;
            mTextureView.setTranslationY(0);
        }
        else {
            lp.height = lp.width * 4/3;
            mTextureView.setTranslationY(mTextureViewYOffset);
        }
        mTextureView.setLayoutParams(lp);
    }

    /**
     * 初始化settings
     */
    private void initSettings() {
        flashMode = mCameraParameters.getFlashMode();
        focusMode = mCameraParameters.getFocusMode();
        sceneMode = mCameraParameters.getSceneMode();
        whiteBalance = mCameraParameters.getWhiteBalance();

        if(flashMode!=null)
            setFlashMode();
    }

    private void startAvalible() {
        getWidgetInstances();
        setBrightnessMax();
        startSensor();
        mScreenMaskView.stopQRreading();
        isQrMode = false;

        if(mTextureView.isAvailable()){
            openCamera();
        }else {
            mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
                    openCamera();
                }

                @Override
                public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

                }

                @Override
                public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
                    return false;
                }

                @Override
                public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

                }
            });
        }
    }


    /**
     * 打开摄像头并开始预览
     */
    private void openCamera() {
        mCamera = Camera.open(cameraId);
        getCameraParameters();
        setPhotoSize();
        initSettings();

        SurfaceTexture mPreviewTexture = mTextureView.getSurfaceTexture();
        try {
            mCamera.setPreviewTexture(mPreviewTexture);
            mCamera.setDisplayOrientation(90);

            if(cameraId==0){
                focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE;
                mCameraParameters.setFocusMode(focusMode);
            }
            mCameraParameters.setPreviewSize(previewSize.width,previewSize.height);
            mCamera.setParameters(mCameraParameters);

        } catch (IOException e) {
            e.printStackTrace();
        }

        mCamera.startPreview();
        isPreviewing = true;
        mCamera.setOneShotPreviewCallback(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] bytes, Camera camera) {
                mScreenMaskView.noMask();
            }
        });
    }

    /**
     * 开始对焦，将对焦模式转换成auto
     * @param pointX 对焦区域中心点x
     * @param pointY 对焦区域中心点y
     */
    private void startFocus(int pointX,int pointY) {
        int areaCenterX = 0;
        int areaCenterY = 0;

        int[] viewLocation = new int[2];
        mTextureView.getLocationOnScreen(viewLocation);
        int locationX = viewLocation[0];
        int locationY = viewLocation[1];
        int centerX = locationX + mTextureView.getWidth()/2;
        int centerY = locationY + mTextureView.getHeight()/2;
        int halfWidth = mTextureView.getWidth()/2;
        int halfHeight = mTextureView.getHeight()/2;
        //camera坐标是基于传感器方向，所以实际操作中，是屏幕逆时针旋转90度以后的坐标
        //所以，camera的Y坐标是屏幕x坐标取负，camera的x坐标是屏幕的y坐标。
        areaCenterY = -(1000*(pointX-centerX)/halfWidth);
        areaCenterX = 1000*(pointY-centerY)/halfHeight;
        //为了防止在取景区域边缘导致的溢出，所以在边缘的对焦固定在边缘方块中。
        if(areaCenterX<-870)areaCenterX = -870;
        if(areaCenterX>870)areaCenterX = 870;
        if(areaCenterY<-870)areaCenterY = -870;
        if(areaCenterY>870)areaCenterY = 870;

        if(mCameraParameters.getMaxNumFocusAreas()>0)
        {
            List<Camera.Area> focusArea = new ArrayList<Camera.Area>();

            Rect focusRect = new Rect(areaCenterX-130,areaCenterY-130,areaCenterX+130,areaCenterY+130);
            focusArea.add(new Camera.Area(focusRect,1000));
            mCameraParameters.setFocusAreas(focusArea);
            mCameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            mCamera.setParameters(mCameraParameters);

            mCamera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean b, Camera camera) {
                    if(b)
                        mScreenMaskView.focusSuccess();
                    else mScreenMaskView.focusFailed();
                }
            });
        }
    }

    /**
     * 设置闪光灯模式，并且设置闪光灯模式按钮的图标
     */
    private void setFlashMode() {
        if(flashMode.equals(Camera.Parameters.FLASH_MODE_OFF))
            flashBtn.setImageDrawable(getResources().getDrawable(R.drawable.flash_off));
        if(flashMode.equals(Camera.Parameters.FLASH_MODE_AUTO))
            flashBtn.setImageDrawable(getResources().getDrawable(R.drawable.flash_auto));
        if(flashMode.equals(Camera.Parameters.FLASH_MODE_ON))
            flashBtn.setImageDrawable(getResources().getDrawable(R.drawable.flash_on));
        if(flashMode.equals(Camera.Parameters.FLASH_MODE_TORCH))
            flashBtn.setImageDrawable(getResources().getDrawable(R.drawable.flash_torch));
        mCameraParameters.setFlashMode(flashMode);
        mCamera.setParameters(mCameraParameters);
    }
}
