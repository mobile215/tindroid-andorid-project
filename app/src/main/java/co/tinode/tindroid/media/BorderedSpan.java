package co.tinode.tindroid.media;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import androidx.annotation.NonNull;
import android.text.style.ReplacementSpan;

public class BorderedSpan extends ReplacementSpan {
    private static final String TAG = "BorderedSpan";

    private static final float RADIUS_CORNER = 2.5f;
    private static final float SHADOW_SIZE = 2.5f;

    // Minimum button width in '0' characters.
    private static final int MIN_BUTTON_WIDTH = 10;
    private final Paint mPaintBackground;
    private int mWidth;
    private int mWidthActual;
    private int mTextColor;
    private int mMinButtonWidth;
    private float mDipSize;

    BorderedSpan(final Context context, final float charWidth, float dipSize) {
        mPaintBackground = new Paint();
        mPaintBackground.setStyle(Paint.Style.FILL);
        mPaintBackground.setAntiAlias(true);
        mPaintBackground.setColor(Color.rgb(0xEE, 0xEE, 0xFF));
        mPaintBackground.setShadowLayer(SHADOW_SIZE * dipSize,
                SHADOW_SIZE * 0.5f * dipSize,
                SHADOW_SIZE * 0.5f * dipSize,
                Color.argb(0x80, 0, 0, 0));

        int[] attrs = {android.R.attr.textColorLink};
        TypedArray colors = context.obtainStyledAttributes(attrs);
        mTextColor = colors.getColor(0, 0x7bc9c2);
        colors.recycle();

        mMinButtonWidth = (int) (MIN_BUTTON_WIDTH * charWidth / dipSize);
        mDipSize = dipSize;
    }

    @Override
    public int getSize(@NonNull Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
        // Actual text width;
        mWidthActual = (int) paint.measureText(text, start, end);

        // Width of the button.
        int len = text.length();
        // Add ~2 char padding left and right.
        mWidth = mWidthActual * (len+4) / len;
        // Ensure minimum width of the button.
        mWidth = mWidth < mMinButtonWidth ? mMinButtonWidth : mWidth;
        return mWidth;
    }

    @Override
    public void draw(@NonNull Canvas canvas, CharSequence text,
                     int start, int end, float x, int top, int y, int bottom, @NonNull Paint paint) {
        float textHeight = paint.descent() - paint.ascent();
        RectF outline = new RectF(x, top, x + mWidth, bottom);
        outline.inset(SHADOW_SIZE * mDipSize, SHADOW_SIZE * mDipSize);
        // Draw colored background
        canvas.drawRoundRect(outline, RADIUS_CORNER * mDipSize, RADIUS_CORNER * mDipSize, mPaintBackground);

        // Don't underline the text.
        paint.setUnderlineText(false);
        paint.setColor(mTextColor);
        canvas.drawText(text, start, end,
                x + (mWidth - mWidthActual) * 0.5f,
                (top + bottom - paint.ascent()) * 0.5f - 5, // I don't know why -5 is needed but it works.
                paint);
    }
}
