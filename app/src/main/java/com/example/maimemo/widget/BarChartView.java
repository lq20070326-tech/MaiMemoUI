package com.example.maimemo.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.maimemo.R;

/**
 * 轻量柱状图：一次绘制画完整张图，替代「每根柱子一个 View」的写法。
 * 颜色、字号、密度、文字度量都在构造期算好，onDraw 无对象分配。
 */
public class BarChartView extends View {

    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint.FontMetrics fontMetrics = new Paint.FontMetrics();
    private final RectF barRect = new RectF();
    private final Path barPath = new Path();

    private final float density;
    private final float maxBarWidth;
    private final float cornerRadius;
    private final float valueAreaHeight;
    private final float labelAreaHeight;
    private final float textDescent;

    private float[] values = new float[0];
    private String[] labels = new String[0];
    private String[] valueTexts = new String[0];
    private float maxValue = 1f;

    public BarChartView(Context context) {
        this(context, null);
    }

    public BarChartView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BarChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        density = getResources().getDisplayMetrics().density;
        float scaledDensity = getResources().getDisplayMetrics().scaledDensity;
        maxBarWidth = 18f * density;
        cornerRadius = 5f * density;
        valueAreaHeight = 20f * density;
        labelAreaHeight = 22f * density;

        barPaint.setStyle(Paint.Style.FILL);
        barPaint.setColor(color(R.color.brand_green));

        valuePaint.setTextAlign(Paint.Align.CENTER);
        valuePaint.setTextSize(10f * scaledDensity);
        valuePaint.setColor(color(R.color.text_secondary));

        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setTextSize(11f * scaledDensity);
        labelPaint.setColor(color(R.color.text_secondary));

        labelPaint.getFontMetrics(fontMetrics);
        textDescent = fontMetrics.descent;
    }

    /** 设置柱状图数据：柱值 + 每根柱子的标签 */
    public void setData(float[] values, String[] labels) {
        this.values = values == null ? new float[0] : values;
        this.labels = labels == null ? new String[0] : labels;
        valueTexts = new String[this.values.length];

        float max = 1f;
        for (int i = 0; i < this.values.length; i++) {
            valueTexts[i] = String.valueOf((int) this.values[i]);
            if (this.values[i] > max) {
                max = this.values[i];
            }
        }
        maxValue = max;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int count = values.length;
        if (count == 0) {
            return;
        }

        float columnWidth = getWidth() / (float) count;
        float barWidth = Math.min(maxBarWidth, columnWidth * 0.42f);
        float barBottom = getHeight() - labelAreaHeight;
        float available = barBottom - valueAreaHeight;
        if (available <= 0f) {
            return;
        }

        float labelBaseline = getHeight() - textDescent - 2f * density;
        float valueOffset = textDescent + 4f * density;

        for (int i = 0; i < count; i++) {
            float centerX = columnWidth * i + columnWidth / 2f;
            float barHeight = available * (values[i] / maxValue);
            float left = centerX - barWidth / 2f;
            float top = barBottom - barHeight;

            barRect.set(left, top, left + barWidth, barBottom);
            barPath.reset();
            barPath.addRoundRect(barRect, cornerRadius, cornerRadius, Path.Direction.CW);
            canvas.drawPath(barPath, barPaint);

            if (i < valueTexts.length) {
                canvas.drawText(valueTexts[i], centerX, top - valueOffset, valuePaint);
            }
            if (i < labels.length) {
                canvas.drawText(labels[i], centerX, labelBaseline, labelPaint);
            }
        }
    }

    private int color(int resId) {
        return ContextCompat.getColor(getContext(), resId);
    }
}