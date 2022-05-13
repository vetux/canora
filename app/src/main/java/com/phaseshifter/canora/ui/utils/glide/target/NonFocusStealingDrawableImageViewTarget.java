package com.phaseshifter.canora.ui.utils.glide.target;

import android.graphics.drawable.Drawable;
import android.widget.ImageView;
import androidx.annotation.Nullable;
import com.bumptech.glide.request.target.DrawableImageViewTarget;

public class NonFocusStealingDrawableImageViewTarget extends DrawableImageViewTarget {
    public NonFocusStealingDrawableImageViewTarget(ImageView view) {
        super(view);
    }

    /**
     * super.onLoadFailed calls view.setImageDrawable(null) which causes the view to gain focus for whatever reason only when
     * called inside this function body.
     * <p>
     * Calling view.setImageDrawable(null) anywhere else does not result in the view gaining focus.
     */
    @Override
    public void onLoadFailed(@Nullable Drawable errorDrawable) {
        view.setImageDrawable(errorDrawable);
    }
}