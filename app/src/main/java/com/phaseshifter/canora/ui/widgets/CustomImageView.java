package com.phaseshifter.canora.ui.widgets;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.util.AttributeSet;
import android.widget.ImageView;
import com.phaseshifter.canora.R;

/**
 * Custom imageview which allows for setting rounded cornes, automatic animation and ratio lock.
 */
@SuppressLint("AppCompatCustomView")
//Tinting is broken in AppCompatImageView. Calling setImageTintList(null) does not reliably clear the tint.
public class CustomImageView extends ImageView {
    private float radius = 18.0f;
    private boolean snapToWidth = false;
    private boolean animated = false;
    private Path path;

    private RectF rectF = null;

    public CustomImageView(Context context) {
        super(context);
        init();
    }

    public CustomImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public CustomImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    public float getRadius() {
        return radius;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (radius > 0f && rectF != null) {
            path.reset();
            path.addRoundRect(rectF, radius, radius, Path.Direction.CW);
            canvas.clipPath(path);
        }
        super.onDraw(canvas);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (snapToWidth)
            setMeasuredDimension(getMeasuredWidth(), getMeasuredWidth()); //Snap to width
        else
            setMeasuredDimension(getMeasuredWidth(), getMeasuredHeight());
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (w != 0 && h != 0) {
            if (snapToWidth)
                rectF = new RectF(0, 0, w, w);
            else
                rectF = new RectF(0, 0, w, h);
        }
        super.onSizeChanged(w, h, oldw, oldh);
    }

    private void init() {
        path = new Path();
    }

    private void init(Context context, AttributeSet attrs) {
        path = new Path();
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CustomImageView);
        radius = a.getFloat(R.styleable.CustomImageView_corners, 0f);
        snapToWidth = a.getBoolean(R.styleable.CustomImageView_ratioLock, false);
        animated = a.getBoolean(R.styleable.CustomImageView_automaticAnimation, false);
        a.recycle();
        if (animated) {
            if (getDrawable() instanceof AnimationDrawable) {
                final AnimationDrawable frameAnimation = (AnimationDrawable) getDrawable();
                frameAnimation.setEnterFadeDuration(0);
                frameAnimation.setExitFadeDuration(3000);
                post(frameAnimation::start);
            } else if (getDrawable() instanceof TransitionDrawable) {
            } else if (getDrawable() instanceof AnimatedVectorDrawable) {
                final AnimatedVectorDrawable frameAnimation = (AnimatedVectorDrawable) getDrawable();
                post(frameAnimation::start);
            }
        }
    }
}