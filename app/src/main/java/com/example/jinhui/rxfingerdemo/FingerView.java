package com.example.jinhui.rxfingerdemo;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;

/**
 * Created by jinhui on 2017/12/27.
 * Email:1004260403@qq.com
 */

public class FingerView extends View {

    private static final String TAG = FingerView.class.getSimpleName();
    // 未扫描
    public final static int STATE_NO_SCANING = 0;
    // 已扫描
    public final static int STATE_SCANING = 1;
    // 错误密码
    public final static int STATE_ERROR_PSD = 2;
    // 正确密码
    public final static int STATE_CORRECT_PSD = 3;

    // 当前状态
    public int mCurrentState = STATE_NO_SCANING;

    public Resources mResources;

    public static int DEFAULT_DURATION = 700;
    private float mFraction = 0f, mFraction2 = 1f;
    //判断是否要继续动画
    private boolean isAnim = true;
    // 判断是否要缩放
    private boolean isScale = false;
    private Animation mShakeAnim = null; // 抖动的动画

    // bitmap
    private Bitmap mFingerRed, mFingerGreen, mFingerGrey;
    // 宽、高
    private int mBitWidth, mBitHeight;
    private int mWidth, mHeight;

    private Rect mSrcRect, mDestRect;

    float scale = 1.0f;
    // 扫描的次数
    private int scaningCount = 0;
    // 画笔
    private Paint mBitPaint;

    public FingerView(Context context) {
//        super(context);
        this(context,null);
    }

    public FingerView(Context context, @Nullable AttributeSet attrs) {
//        super(context, attrs);
        this(context, attrs,0);
    }

    public FingerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mResources = getResources();
//        mShakeAnim = AnimationUtils.loadAnimation(context, R.anim.anim_lockpattern_shake_x);
        initBitmap();
        Log.e(TAG, "initBitmap 方法执行");
        initPaint();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public FingerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    private void initPaint() {
        mBitPaint = new Paint();
        // 防抖动
        mBitPaint.setDither(true);
        // 开启图像过滤
        mBitPaint.setFilterBitmap(true);
    }

    private void initBitmap() {
        mFingerRed = ((BitmapDrawable) mResources.getDrawable(R.drawable.finger_red)).getBitmap();
        mFingerGreen = ((BitmapDrawable) mResources.getDrawable(R.drawable.finger_green)).getBitmap();
        mFingerGrey = ((BitmapDrawable) mResources.getDrawable(R.drawable.finger_grey)).getBitmap();
        mBitWidth = mFingerRed.getWidth();
        Log.e(TAG, "mBitWidth =" + mBitWidth);
        mBitHeight = mFingerRed.getHeight();
        Log.e(TAG, "mBitHeight =" + mBitHeight);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        Log.e(TAG, "onSizeChanged 方法执行");
        mWidth = w;
        mHeight = h;
        mFingerRed = setScale(mFingerRed);
        mFingerGreen = setScale(mFingerGreen);
        mFingerGrey = setScale(mFingerGrey);
        mBitWidth = mFingerRed.getWidth();
        mBitHeight = mFingerRed.getHeight();
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        mBitPaint.setAlpha(255);
        mDestRect = new Rect((int) (mBitHeight * (1 - mFraction2)), (int) (mBitHeight * (1 - mFraction2)), (int) (mBitHeight * mFraction2), (int) (mBitHeight * mFraction2));
        mSrcRect = new Rect(0, 0, mBitWidth, mBitHeight);
        canvas.drawBitmap(mFingerGrey, mSrcRect, mDestRect, mBitPaint);

        if (scaningCount == 0) {
            mDestRect = new Rect(0, 0, mBitWidth, (int) (mBitHeight * mFraction));
            mSrcRect = new Rect(0, 0, mBitWidth, (int) (mBitHeight * mFraction));
            canvas.drawBitmap(mFingerGreen, mSrcRect, mDestRect, mBitPaint);
        } else if (scaningCount % 2 == 0) {
            if (mFraction <= 0.5) {
                mBitPaint.setAlpha((int) (255 * (1 - mFraction)));
                canvas.drawBitmap(mFingerRed, mSrcRect, mDestRect, mBitPaint);
            } else {
                mBitPaint.setAlpha((int) (255 * mFraction));
                canvas.drawBitmap(mFingerGreen, mSrcRect, mDestRect, mBitPaint);
            }
        } else {
            if (mFraction <= 0.5) {
                mBitPaint.setAlpha((int) (255 * (1 - mFraction)));
                canvas.drawBitmap(mFingerGreen, mSrcRect, mDestRect, mBitPaint);
            } else {
                mBitPaint.setAlpha((int) (255 * mFraction));
                canvas.drawBitmap(mFingerRed, mSrcRect, mDestRect, mBitPaint);
            }

        }

        if (isScale) {
            if (mCurrentState == STATE_ERROR_PSD) {
                canvas.drawBitmap(mFingerRed, mSrcRect, mDestRect, mBitPaint);
            }
            if (mCurrentState == STATE_CORRECT_PSD) {
                canvas.drawBitmap(mFingerGreen, mSrcRect, mDestRect, mBitPaint);
            }
        }

    }

    /**
     * 开始扫描
     */
    private void startScaning() {
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(0.f, 100.f);
        valueAnimator.setDuration(DEFAULT_DURATION);
        valueAnimator.setInterpolator(new LinearInterpolator());
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mFraction = animation.getAnimatedFraction();
                invalidate();
            }
        });

        valueAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                if (isScale) {
                    isScale = false;
                }
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mFraction = 0;
                scaningCount++;
                if (mCurrentState == STATE_ERROR_PSD && scaningCount % 2 == 1) {
                    isScale = true;
                    isAnim = false;
                    startScale();
                }
                if(mCurrentState == STATE_CORRECT_PSD && scaningCount%2==0){
                    isScale = true;
                    isAnim = false;
                    startScale();
                }
                if(isAnim){
                    startScaning();
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });

        if (!valueAnimator.isRunning()) {
            valueAnimator.start();
        }
    }


    /**
     * 开始缩放
     */
    private void startScale() {
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(0.f, 100.f);
        valueAnimator.setDuration(500);
        valueAnimator.setInterpolator(new OvershootInterpolator(1.2f));
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                mFraction2 = 0.85f + 0.15f * valueAnimator.getAnimatedFraction();
                invalidate();
            }
        });
        valueAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (listener != null) {
                    listener.onChange(mCurrentState);
                }
            }
        });
        if (!valueAnimator.isRunning()) {
            valueAnimator.start();
        }
    }


    private void startReset() {
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(0.f, 100.f);
        valueAnimator.setDuration(DEFAULT_DURATION);
        valueAnimator.setInterpolator(new LinearInterpolator());
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                mFraction = 1-valueAnimator.getAnimatedFraction();
                invalidate();
            }
        });
        valueAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);

                startScaning();
            }
        });
        if (!valueAnimator.isRunning()) {
            valueAnimator.start();
        }

    }

    // 设置指纹识别的状态
    public void setState(int state) {
        mCurrentState = state;
        switch (state) {
            case STATE_SCANING:
                startScaning();
                break;
            case STATE_ERROR_PSD:
                break;
            case STATE_CORRECT_PSD:
                break;
            case STATE_NO_SCANING:
                resetConfig();
                break;
        }
    }

    // 获取当前状态
    public int getState() {
        return mCurrentState;
    }

    /**
     * 处理图片缩放
     */
    private Bitmap setScale(Bitmap a) {
        Log.e(TAG, "a =" + a);  // a = null
        scale = ((float) (mWidth) / mBitWidth);
        Matrix mMatrix = new Matrix();
        mMatrix.postScale(scale, scale);
        Bitmap bmp = Bitmap.createBitmap(a, 0, 0, a.getWidth(), a.getHeight(),
                mMatrix, true);
        return bmp;
    }


    // 重置状态
    private void resetConfig() {
        mCurrentState = STATE_NO_SCANING;
        startReset();
        mFraction = 0f;
        mFraction2 = 1f;
        scaningCount = 0;
        scale =1.0f;
        isAnim=true;
        isScale=false;
    }


    // 回调接口
    public interface OnStateChangedListener {
        public void onChange(int state);
    }

    private OnStateChangedListener listener;

    public void setOnStateChangedListener(OnStateChangedListener listener) {
        this.listener = listener;
    }
}
