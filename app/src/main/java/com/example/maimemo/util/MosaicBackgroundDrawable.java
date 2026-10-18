package com.example.maimemo.util;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/** Soft translucent mosaic surface; draws directly without external image assets. */
public final class MosaicBackgroundDrawable extends Drawable {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float radius;
    private final int[] colors = {
            0x99D9F2E5, 0x99D8E8F8, 0x99F6E4D5, 0x99E4DDF5,
            0x99D2ECEB, 0x99F4EBCF, 0x99E9E3D9, 0x99D5E3F0
    };
    public MosaicBackgroundDrawable(float cornerRadiusPx) { radius = cornerRadiusPx; }
    @Override public void draw(Canvas canvas) {
        RectF b = new RectF(getBounds());
        if (b.isEmpty()) return;
        int save = canvas.save();
        Path clip = new Path(); clip.addRoundRect(b, radius, radius, Path.Direction.CW); canvas.clipPath(clip);
        final float tile = Math.max(24f, Math.min(b.width(), b.height()) / 5.5f);
        int row = 0;
        for (float y=b.top; y<b.bottom; y+=tile, row++) {
            int col = 0;
            for (float x=b.left; x<b.right; x+=tile, col++) {
                paint.setColor(colors[Math.floorMod(row * 5 + col * 3 + ((row + col) % 3), colors.length)]);
                canvas.drawRect(x, y, Math.min(x+tile+1,b.right), Math.min(y+tile+1,b.bottom), paint);
            }
        }
        // Frosted-glass veil preserves the translucent look and keeps text readable.
        paint.setColor(0x66FFFFFF); canvas.drawRect(b, paint);
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(1.2f); paint.setColor(0x5533A879);
        canvas.drawRoundRect(b, radius, radius, paint); paint.setStyle(Paint.Style.FILL);
        canvas.restoreToCount(save);
    }
    @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); invalidateSelf(); }
    @Override public void setColorFilter(android.graphics.ColorFilter colorFilter) { paint.setColorFilter(colorFilter); invalidateSelf(); }
    @Override public int getOpacity() { return android.graphics.PixelFormat.TRANSLUCENT; }
}
