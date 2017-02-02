package com.septem.a5dmarkv;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by septem on 2016/9/19.
 * 用于显示黑屏，屏幕指示效果等
 */
public class ScreenMaskView extends View {
    private boolean isFocusing = false;
    private boolean isQRreading = false;
    private int focusFieldX;
    private int focusFieldY;
    private int radius = 100;
    private int focusRingWidth = 8;
    private int ringColor = Color.LTGRAY;

    private int fadeDuration = 150;
    private int emergeDuration = 150;
    private int shutterDuration = 50;
    private int shutterDelay = 200;
    private int focusRingCleanDelay = 500;
    private Animator fadeAnimator;
    private Animator emergeAnimator;
    private ScreenMaskListener mScreenMaskListener;
    private AnimatorListenerAdapter fadeListener;
    private AnimatorListenerAdapter emergeListener;
    private Handler mHandler;

    private Paint focusPaint;
    private Paint borderPaint;
    private Paint framePaint;
    private Paint tipPaint;
    private Rect readingFrame;
    private String tipOfQR = "将二维码放在方框内，然后按下快门键";
    private Point tipCenter;
    private int frameWidth = 2;
    private int borderAlpha = 150;
    private int tipSize = 40;
    private int tipOffset = 60;
    private List<Rect> borders;

    public ScreenMaskView(Context context, AttributeSet attrs) {
        super(context, attrs);

        setBackgroundResource(R.color.maskBackground);
        getBackground().setAlpha(255);

        initPaints();

        fadeAnimator = ObjectAnimator.ofInt(getBackground(),"alpha",0,255);
        emergeAnimator = ObjectAnimator.ofInt(getBackground(),"alpha",255,0);

        fadeListener = new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if(mScreenMaskListener!=null)
                    mScreenMaskListener.fadeComplete();
                fadeAnimator.removeAllListeners();
            }
        };
        emergeListener = new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if(mScreenMaskListener!=null)
                    mScreenMaskListener.emergeComplete();
                emergeAnimator.removeAllListeners();
            }
        };
        mHandler = new Handler();
    }

    private void initPaints() {
        focusPaint = new Paint();
        focusPaint.setStyle(Paint.Style.STROKE);
        focusPaint.setStrokeWidth(focusRingWidth);
        borderPaint = new Paint();
        borderPaint.setColor(Color.BLACK);
        borderPaint.setAlpha(borderAlpha);
        framePaint = new Paint();
        framePaint.setStyle(Paint.Style.STROKE);
        framePaint.setStrokeWidth(frameWidth);
        framePaint.setColor(Color.GREEN);
        framePaint.setAlpha(borderAlpha);
        tipPaint = new Paint();
        tipPaint.setColor(Color.WHITE);
        tipPaint.setTextAlign(Paint.Align.CENTER);
        tipPaint.setTextSize(tipSize);
        tipPaint.setAlpha(borderAlpha);
        tipPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        tipPaint.setTypeface(Typeface.SANS_SERIF);
    }

    private void initQRInterface(int areaSize,int areaYOffset) {
        int height = getHeight();
        int width = getWidth();
        borders = new ArrayList<>();
        Rect rect = new Rect(0,0,width,height/2-areaSize/2+areaYOffset);
        borders.add(rect);
        rect = new Rect(0,height/2+areaSize/2+areaYOffset,width,height);
        borders.add(rect);
        rect = new Rect(0,
                height/2-areaSize/2+areaYOffset,
                width/2-areaSize/2,
                height/2+areaSize/2+areaYOffset);
        borders.add(rect);
        rect = new Rect(width/2+areaSize/2,
                height/2-areaSize/2+areaYOffset,
                width,
                height/2+areaSize/2+areaYOffset);
        borders.add(rect);
        rect = null;
        readingFrame = new Rect(width/2-areaSize/2,
                height/2-areaSize/2+areaYOffset,
                width/2+areaSize/2,
                height/2+areaSize/2+areaYOffset);
        tipCenter = new Point(width/2,height/2+areaSize/2+areaYOffset+tipOffset);
    }

    public void blackMask() {
        clean();
        fadeAnimator.setDuration(fadeDuration);
        fadeAnimator.addListener(fadeListener);
        fadeAnimator.start();
    }
    public void noMask() {
        clean();
        emergeAnimator.setDuration(emergeDuration);
        emergeAnimator.addListener(emergeListener);
        emergeAnimator.start();
    }
    public void setScreenMaskListener(ScreenMaskListener sml) {
        mScreenMaskListener = sml;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        focusPaint.setColor(ringColor);
        if(isFocusing)
            canvas.drawCircle(focusFieldX,focusFieldY,radius,focusPaint);
        if(isQRreading) {
            for (Rect r : borders)
                canvas.drawRect(r, borderPaint);
            canvas.drawRect(readingFrame,framePaint);
            canvas.drawText(tipOfQR, tipCenter.x, tipCenter.y,tipPaint);
        }
    }

    public void startQRreading(int areaSize,int areaYOffset) {
        initQRInterface(areaSize,areaYOffset);
        isQRreading = true;
        invalidate();
    }

    public void stopQRreading() {
        isQRreading = false;
        invalidate();
    }

    public void focusSuccess() {
        ringColor = Color.GREEN;
        invalidate();
        postDelayed(new Runnable() {
            @Override
            public void run() {
                clean();
            }
        },focusRingCleanDelay);
    }
    public void focusFailed() {
        ringColor = Color.RED;
        invalidate();
        postDelayed(new Runnable() {
            @Override
            public void run() {
                clean();
            }
        },focusRingCleanDelay);
    }
    public void startFocus(float x,float y) {
        focusFieldX = (int)x;
        focusFieldY = (int)y;
        isFocusing = true;
        ringColor = Color.LTGRAY;
        invalidate();
    }
    public void clean() {
        isFocusing = false;
        invalidate();
    }
    public void shutterClicked() {
        clean();
        fadeAnimator.setDuration(shutterDuration).start();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                emergeAnimator.setDuration(shutterDuration).start();
            }
        },shutterDelay);
    }
}

