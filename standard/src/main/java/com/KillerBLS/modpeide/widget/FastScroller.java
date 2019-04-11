/*
 * Licensed to the Light Team Software (Light Team) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The Light Team licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.KillerBLS.modpeide.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import com.KillerBLS.modpeide.R;
import com.KillerBLS.modpeide.utils.commons.OnScrollChangedListener;
import com.KillerBLS.modpeide.utils.text.style.StylePaint;

import butterknife.BindDrawable;
import butterknife.ButterKnife;

/**
 * @author Henry Thompson
 */
public class FastScroller extends View implements OnScrollChangedListener {

    public static final int STATE_EXITING = 3;
    public static final int STATE_DRAGGING = 2;
    public static final int STATE_HIDDEN = 0;
    public static final int STATE_VISIBLE = 1;

    private TextProcessor mEditor;
    private final Runnable hideScroller = () -> setState(STATE_EXITING);

    private Bitmap mBitmapDragging;
    private Bitmap mBitmapNormal;

    private float mScrollMax;
    private float mScrollY;

    private Handler mHandler = new Handler();
    private StylePaint mPaint;
    private int mState = STATE_HIDDEN;

    @BindDrawable(R.drawable.fastscroll_thumb_pressed)
    Drawable mThumbDrawableDragging;
    @BindDrawable(R.drawable.fastscroll_thumb_default)
    Drawable mThumbDrawableNormal;

    private float mThumbTop = 0.0f;
    private int mThumbHeight;
    private int mViewHeight;

    // region CONSTRUCTOR

    public FastScroller(Context context) {
        super(context);
        init();
    }

    public FastScroller(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FastScroller(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public FastScroller(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    // endregion CONSTRUCTOR

    private void init() {
        if(!isInEditMode()) {
            ButterKnife.bind(this);
            TypedValue colorAccent = new TypedValue();
            getContext().getTheme()
                    .resolveAttribute(R.attr.colorAccent, colorAccent, true);

            mThumbDrawableNormal.mutate().setColorFilter(colorAccent.data, PorterDuff.Mode.SRC_IN);
            mThumbDrawableDragging.mutate().setColorFilter(colorAccent.data, PorterDuff.Mode.SRC_IN);
            mThumbHeight = mThumbDrawableNormal.getIntrinsicHeight();

            mPaint = new StylePaint(true, false);
            mPaint.setAlpha(225);
        }
    }

    public void link(TextProcessor editor) {
        if(editor != null) {
            mEditor = editor;
            mEditor.addOnScrollChangedListener(this);
        }
    }

    @Override
    public void onScrollChanged(int x, int y, int oldx, int oldy) {
        if(mState != STATE_DRAGGING) {
            getMeasurements();
            setState(STATE_VISIBLE);
            mHandler.postDelayed(hideScroller, 2000);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(mEditor == null || mState == STATE_HIDDEN) {
            return false;
        }
        getMeasurements();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if(!isPointInThumb(event.getX(), event.getY())) {
                    return false;
                }
                mEditor.abortFling();
                setState(STATE_DRAGGING);
                setPressed(true);
                return true;
            case MotionEvent.ACTION_UP:
                setState(STATE_VISIBLE);
                setPressed(false);
                mHandler.postDelayed(hideScroller, 2000);
                return false;
            case MotionEvent.ACTION_MOVE:
                if(mState != STATE_DRAGGING) {
                    return false;
                }
                setPressed(true);
                mEditor.abortFling();
                int newThumbTop = ((int) event.getY()) - (mThumbHeight / 2);
                if(newThumbTop < 0) {
                    newThumbTop = 0;
                } else if(mThumbHeight + newThumbTop > mViewHeight) {
                    newThumbTop = mViewHeight - mThumbHeight;
                }
                mThumbTop = newThumbTop;
                scrollView();
                invalidate();
                return true;
            default:
                return false;
        }
    }

    private void scrollView() {
        float scrollToAsFraction = mThumbTop / (mViewHeight - mThumbHeight);
        mEditor.scrollTo(mEditor.getScrollX(), ((int) (mScrollMax * scrollToAsFraction)) - ((int) (scrollToAsFraction * (mEditor.getHeight() - mEditor.getLineHeight()))));
    }

    private int getThumbTop() {
        int absoluteThumbTop = Math.round((mViewHeight - mThumbHeight) * (mScrollY / ((mScrollMax - mEditor.getHeight()) + mEditor.getLineHeight())));
        if(absoluteThumbTop > getHeight() - mThumbHeight) {
            return getHeight() - mThumbHeight;
        }
        return absoluteThumbTop;
    }

    private boolean isPointInThumb(float x, float y) {
        return x >= 0.0f && x <= getWidth() && y >= mThumbTop && y <= mThumbTop + mThumbHeight;
    }

    private void getMeasurements() {
        if(mEditor != null && mEditor.getLayout() != null) {
            mViewHeight = getHeight();
            mScrollMax = mEditor.getLayout().getHeight();
            mScrollY = mEditor.getScrollY();
            mEditor.getHeight();
            mEditor.getLayout().getHeight();
            mThumbTop = getThumbTop();
        }
    }

    private boolean isShowScrollerJustified() {
        return (mScrollMax / mEditor.getHeight()) >= 1.5d;
    }

    public void setState(int state) {
        switch (state) {
            case STATE_HIDDEN:
                mHandler.removeCallbacks(hideScroller);
                mState = STATE_HIDDEN;
                invalidate();
                return;
            case STATE_VISIBLE:
                if(isShowScrollerJustified()) {
                    mHandler.removeCallbacks(hideScroller);
                    mState = STATE_VISIBLE;
                    invalidate();
                    return;
                }
                return;
            case STATE_DRAGGING:
                mHandler.removeCallbacks(hideScroller);
                mState = STATE_DRAGGING;
                invalidate();
                return;
            case STATE_EXITING:
                mHandler.removeCallbacks(hideScroller);
                mState = STATE_EXITING;
                invalidate();
                return;
            default:
        }
    }

    public int getState() {
        return mState;
    }

    @Override
    public void onDraw(Canvas canvas) {
        if(mEditor != null && getState() != STATE_HIDDEN) {
            if(mBitmapNormal == null) {
                mThumbDrawableNormal.setBounds(new Rect(0, 0, getWidth(), mThumbHeight));
                mBitmapNormal = Bitmap.createBitmap(getWidth(), mThumbHeight, Bitmap.Config.ARGB_8888);
                mThumbDrawableNormal.draw(new Canvas(mBitmapNormal));
            }
            if(mBitmapDragging == null) {
                mThumbDrawableDragging.setBounds(new Rect(0, 0, getWidth(), mThumbHeight));
                mBitmapDragging = Bitmap.createBitmap(getWidth(), mThumbHeight, Bitmap.Config.ARGB_8888);
                mThumbDrawableDragging.draw(new Canvas(mBitmapDragging));
            }
            super.onDraw(canvas);
            if(getState() == STATE_VISIBLE || getState() == STATE_DRAGGING) {
                mPaint.setAlpha(225);
                if(getState() == STATE_VISIBLE) {
                    canvas.drawBitmap(mBitmapNormal, 0.0f, mThumbTop, mPaint);
                } else {
                    canvas.drawBitmap(mBitmapDragging, 0.0f, mThumbTop, mPaint);
                }
            } else if(getState() != STATE_EXITING) {
                //nothing
            } else {
                if(mPaint.getAlpha() > 25) {
                    mPaint.setAlpha(mPaint.getAlpha() - 25);
                    canvas.drawBitmap(mBitmapNormal, 0.0f, mThumbTop, mPaint);
                    mHandler.postDelayed(hideScroller, 17);
                    return;
                }
                mPaint.setAlpha(0);
                setState(STATE_HIDDEN);
            }
        }
    }
}