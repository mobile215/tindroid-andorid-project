package co.tinode.tindroid.widgets;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import co.tinode.tindroid.R;

public class RippleFrameLayout extends FrameLayout {
    View mOverlay;

    public RippleFrameLayout(@NonNull Context context) {
        this(context, null);
    }

    public RippleFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (mOverlay == null) {
            mOverlay = findViewById(R.id.overlay);
        }

        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            Drawable background = mOverlay.getBackground();
            background.setHotspot(ev.getX(), ev.getY());
        }

        return false;
    }
}
