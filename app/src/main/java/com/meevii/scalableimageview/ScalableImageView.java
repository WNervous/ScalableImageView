package com.meevii.scalableimageview;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.support.v4.view.GestureDetectorCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.OverScroller;

/**
 * 绘制一个可以缩放的imageview
 * 思路：
 * 1. 绘制图片
 * 2. 居中
 * 3. 两种放缩方式：----> 根据两种方式计算分别计算scale
 * 4. 双击手势 gesture detector     拦截onToucheEvent;
 * 5. 放大后拖动效果
 * 6. fling 效果
 */

public class ScalableImageView extends View implements Runnable {

    private static final float IMAGE_SIZE = Utils.dpToPixel(300);
    private static final float OVER_SCALE = 1.5f;

    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Bitmap mBitmap;

    private float mBitmapOffsetX;
    private float mBitmapOffsetY;
    private float mCanvasOffSetX;
    private float mCanvasOffSetY;

    private float          mBigImageScale;
    private float          mSmallImageScale;
    private boolean        mIsBig;
    // 缩放动画
    public  float          mScaleFraction;
    private ObjectAnimator mScaleAnimator;

    private GestureDetectorCompat      mGestureDetectorCompat;
    private OverScroller               overScroller;

    public ScalableImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mBitmap = Utils.getAvatar(getResources(), (int) IMAGE_SIZE);
        mGestureDetectorCompat = new GestureDetectorCompat(context, new ScaleGestureListener());
        overScroller = new OverScroller(context);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mBitmapOffsetX = ((float) getWidth() - mBitmap.getWidth()) / 2;
        mBitmapOffsetY = ((float) getHeight() - mBitmap.getWidth()) / 2;

        if ((float) mBitmap.getWidth() / mBitmap.getHeight() > (float) getWidth() / getHeight()) {
            mSmallImageScale = (float) getWidth() / mBitmap.getWidth();
            mBigImageScale = (float) getHeight() / mBitmap.getHeight() * OVER_SCALE;
        } else {
            mSmallImageScale = (float) getHeight() / mBitmap.getHeight();
            mBigImageScale = (float) getWidth() / mBitmap.getWidth() * OVER_SCALE;
        }

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.translate(mCanvasOffSetX * mScaleFraction, mCanvasOffSetY * mScaleFraction);
        float imageScale = mSmallImageScale + mScaleFraction * (mBigImageScale - mSmallImageScale);
        canvas.scale(imageScale, imageScale, getWidth() / 2f, getHeight() / 2f);
        canvas.drawBitmap(mBitmap, mBitmapOffsetX, mBitmapOffsetY, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mGestureDetectorCompat.onTouchEvent(event);
    }

    private class ScaleGestureListener extends GestureDetector.SimpleOnGestureListener {

        /**
         * 除了onDown 的返回值对开发有用处外，其他方法的返回值没有用，不予处理
         */

        @Override
        public boolean onDown(MotionEvent motionEvent) {
            // ACTION_DOWN
            return true;
        }

        @Override
        public void onShowPress(MotionEvent motionEvent) {
            //
        }

        /**
         * // 单击  如果长安没有关闭的情况下  按下时间小于500ms 回调
         */
        @Override
        public boolean onSingleTapUp(MotionEvent motionEvent) {
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent down, MotionEvent motionEvent1, float distanceX, float distanceY) {
            if (mIsBig) {
                mCanvasOffSetX -= distanceX;
                mCanvasOffSetY -= distanceY;
                fixedOffset();
                invalidate();
            }
            return false;
        }


        // 按下时间大于500ms
        @Override
        public void onLongPress(MotionEvent motionEvent) {

        }

        // 手指拨动的动作，   惯性华滑动
        @Override
        public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
            if (mIsBig) {
                overScroller.fling((int) mCanvasOffSetX, (int) mCanvasOffSetY, (int) v, (int) v1, -(int) (mBitmap.getWidth() * mBigImageScale - getWidth()) / 2, (int) (mBitmap.getWidth() * mBigImageScale - getWidth()) / 2, -(int) (mBitmap.getHeight() * mBigImageScale - getHeight()) / 2, (int) (mBitmap.getHeight() * mBigImageScale - getHeight()) / 2);

                postOnAnimation(ScalableImageView.this);
            }
            return false;
        }


        ///////////////////////////////////////////////////////////////////////////////////
        // 重写onDoubleTapListener
        ///////////////////////////////////////////////////////////////////////////////////

        //  单击确认
        @Override
        public boolean onSingleTapConfirmed(MotionEvent motionEvent) {
            return false;
        }

        // 间隔大于40ms  小于300ms
        @Override
        public boolean onDoubleTap(MotionEvent motionEvent) {
            mIsBig = !mIsBig;
            if (mIsBig) {
                mCanvasOffSetX = motionEvent.getX() - getWidth() / 2 - (motionEvent.getX() - getWidth() / 2) * mBigImageScale / mSmallImageScale;
                mCanvasOffSetY = motionEvent.getY() - getHeight() / 2 - (motionEvent.getY() - getHeight() / 2) * mBigImageScale / mSmallImageScale;
                fixedOffset();
                getScaleAnimator().start();
            } else {
                getScaleAnimator().reverse();
            }
            return false;
        }

        // 当触发双击事件时，会一直触发，直到抬起
        @Override
        public boolean onDoubleTapEvent(MotionEvent motionEvent) {
            return false;
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////
    // getter and setter
    ///////////////////////////////////////////////////////////////////////////////////


    private float getMScaleFraction() {
        return mScaleFraction;
    }

    private void setMScaleFraction(float mScaleFraction) {
        this.mScaleFraction = mScaleFraction;
        invalidate();
    }

    private ObjectAnimator getScaleAnimator() {
        if (mScaleAnimator == null) {
            mScaleAnimator = ObjectAnimator.ofFloat(this, "mScaleFraction", 0, 1);
        }
        return mScaleAnimator;
    }


    @Override
    public void run() {
        if (overScroller.computeScrollOffset()) {
            mCanvasOffSetX = overScroller.getCurrX();
            mCanvasOffSetY = overScroller.getCurrY();
            invalidate();
            postOnAnimation(this);
        }
    }

    private void fixedOffset() {
        mCanvasOffSetX = Math.min(mCanvasOffSetX, (mBitmap.getWidth() * mBigImageScale - getWidth()) / 2);
        mCanvasOffSetX = Math.max(mCanvasOffSetX, -(mBitmap.getWidth() * mBigImageScale - getWidth()) / 2);
        mCanvasOffSetY = Math.min(mCanvasOffSetY, (mBitmap.getHeight() * mBigImageScale - getHeight()) / 2);
        mCanvasOffSetY = Math.max(mCanvasOffSetY, -(mBitmap.getHeight() * mBigImageScale - getHeight()) / 2);
    }
}
