package com.example.maimemo.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.maimemo.R;

/**
 * 遗忘曲线图：对比「理论遗忘曲线」与「我的记忆曲线」。
 * 曲线使用 Catmull-Rom -> 三次贝塞尔平滑，绿线下方带渐变填充。
 *
 * 性能约定：坐标点、Path、渐变、文字位置、网格线位置等全部在
 * 「尺寸变化 / 数据变化」时预计算并复用，onDraw 只做绘制，无对象分配。
 */
public class ForgettingCurveView extends View {

    private static final String[] TIME_LABELS = {"0h", "6h", "1天", "2天", "3天", "5天", "7天"};
    private static final int[] LEVELS = {100, 75, 50, 25, 0};
    private static final String[] LEVEL_LABELS = {"100%", "75%", "50%", "25%", "0%"};

    /** 理论遗忘曲线（记忆保持率 %） */
    private float[] theory = {100f, 58f, 44f, 36f, 33f, 30f, 28f};

    /** 我的记忆曲线（经必须背复习后的保持率 %） */
    private float[] mine = {100f, 78f, 72f, 69f, 74f, 76f, 80f};

    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint axisTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint theoryPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint minePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Path theoryPath = new Path();
    private final Path minePath = new Path();
    private final Path fillPath = new Path();
    private final RectF labelRect = new RectF();

    // 预计算几何
    private final float[] theoryX = new float[TIME_LABELS.length];
    private final float[] theoryY = new float[TIME_LABELS.length];
    private final float[] mineX = new float[TIME_LABELS.length];
    private final float[] mineY = new float[TIME_LABELS.length];
    private final float[] gridY = new float[LEVELS.length];
    private final float[] levelTextLeft = new float[LEVELS.length];
    private final float[] timeTextLeft = new float[TIME_LABELS.length];

    private LinearGradient fillGradient;
    private float dotRadius;
    private float chartLeft;
    private float chartTop;
    private float chartRight;
    private float chartHeight;
    private String bestLabel = "";
    private boolean geometryReady;

    private final int cachedGreenSoft;
    private final int cachedGreenClear;
    private final float density = getResources().getDisplayMetrics().density;
    private final float padLeft = 36f * getResources().getDisplayMetrics().density;
    private final float padRight = 12f * getResources().getDisplayMetrics().density;
    private final float padTop = 30f * getResources().getDisplayMetrics().density;
    private final float padBottom = 24f * getResources().getDisplayMetrics().density;

    public ForgettingCurveView(Context context) {
        this(context, null);
    }

    public ForgettingCurveView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ForgettingCurveView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        float scaledDensity = getResources().getDisplayMetrics().scaledDensity;
        dotRadius = 3.5f * density;

        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(density);
        gridPaint.setColor(color(R.color.divider));

        axisTextPaint.setTextSize(10f * scaledDensity);
        axisTextPaint.setColor(color(R.color.text_hint));

        int green = color(R.color.brand_green);
        int greenSoft = withAlpha(green, 56);
        int greenClear = withAlpha(green, 0);
        theoryPaint.setStyle(Paint.Style.STROKE);
        theoryPaint.setStrokeWidth(1.6f * density);
        theoryPaint.setColor(color(R.color.ring_unknown));
        theoryPaint.setPathEffect(new DashPathEffect(new float[]{5f * density, 4f * density}, 0f));

        minePaint.setStyle(Paint.Style.STROKE);
        minePaint.setStrokeWidth(2.4f * density);
        minePaint.setStrokeCap(Paint.Cap.ROUND);
        minePaint.setStrokeJoin(Paint.Join.ROUND);
        minePaint.setColor(green);

        dotFillPaint.setStyle(Paint.Style.FILL);
        dotFillPaint.setColor(Color.WHITE);

        dotStrokePaint.setStyle(Paint.Style.STROKE);
        dotStrokePaint.setStrokeWidth(2f * density);
        dotStrokePaint.setColor(green);

        fillPaint.setStyle(Paint.Style.FILL);

        labelBgPaint.setStyle(Paint.Style.FILL);
        labelBgPaint.setColor(green);

        labelPaint.setTextSize(11f * scaledDensity);
        labelPaint.setColor(Color.WHITE);

        // 渐变只与绿色透明度有关，创建一次即可
        cachedGreenSoft = greenSoft;
        cachedGreenClear = greenClear;
    }

    public void setData(float[] theory, float[] mine) {
        if (theory != null && theory.length == TIME_LABELS.length) {
            this.theory = theory;
        }
        if (mine != null && mine.length == TIME_LABELS.length) {
            this.mine = mine;
        }
        rebuildGeometry();
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        rebuildGeometry();
    }

    /** 只在尺寸或数据变化时执行一次，之后 onDraw 全部复用 */
    private void rebuildGeometry() {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            geometryReady = false;
            return;
        }

        chartLeft = padLeft;
        chartTop = padTop;
        chartRight = w - padRight;
        chartHeight = h - padTop - padBottom;
        float chartWidth = chartRight - chartLeft;
        if (chartWidth <= 0f || chartHeight <= 0f) {
            geometryReady = false;
            return;
        }

        int n = TIME_LABELS.length;
        for (int i = 0; i < n; i++) {
            float x = chartLeft + chartWidth * i / (n - 1);
            theoryX[i] = x;
            theoryY[i] = chartTop + chartHeight * (1f - theory[i] / 100f);
            mineX[i] = x;
            mineY[i] = chartTop + chartHeight * (1f - mine[i] / 100f);
        }

        for (int i = 0; i < LEVELS.length; i++) {
            gridY[i] = chartTop + chartHeight * (1f - LEVELS[i] / 100f);
            levelTextLeft[i] = chartLeft - 6f * density - axisTextPaint.measureText(LEVEL_LABELS[i]);
        }
        for (int i = 0; i < n; i++) {
            timeTextLeft[i] = theoryX[i] - axisTextPaint.measureText(TIME_LABELS[i]) / 2f;
        }

        buildSmoothPath(theoryX, theoryY, theoryPath);

        buildSmoothPath(mineX, mineY, minePath);
        fillPath.set(minePath);
        fillPath.lineTo(mineX[n - 1], chartTop + chartHeight);
        fillPath.lineTo(mineX[0], chartTop + chartHeight);
        fillPath.close();

        fillGradient = new LinearGradient(0f, chartTop, 0f, chartTop + chartHeight,
                cachedGreenSoft, cachedGreenClear, Shader.TileMode.CLAMP);

        // 最佳记忆点标记
        int bestIndex = 0;
        for (int i = 1; i < n; i++) {
            if (mine[i] > mine[bestIndex]) {
                bestIndex = i;
            }
        }
        bestLabel = "最佳 " + (int) mine[bestIndex] + "%";
        float textWidth = labelPaint.measureText(bestLabel);
        float boxWidth = textWidth + 14f * density;
        float boxHeight = 20f * density;
        float boxLeft = Math.max(chartLeft, Math.min(mineX[bestIndex] - boxWidth / 2f, chartRight - boxWidth));
        float boxTop = Math.max(0f, mineY[bestIndex] - 18f * density - boxHeight);
        labelRect.set(boxLeft, boxTop, boxLeft + boxWidth, boxTop + boxHeight);

        geometryReady = true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!geometryReady) {
            return;
        }

        // 网格 + y 轴刻度
        for (int i = 0; i < LEVELS.length; i++) {
            canvas.drawLine(chartLeft, gridY[i], chartRight, gridY[i], gridPaint);
            canvas.drawText(LEVEL_LABELS[i], levelTextLeft[i], gridY[i] + 3.5f * density, axisTextPaint);
        }

        // x 轴刻度
        float timeBaseline = getHeight() - 8f * density;
        for (int i = 0; i < TIME_LABELS.length; i++) {
            canvas.drawText(TIME_LABELS[i], timeTextLeft[i], timeBaseline, axisTextPaint);
        }

        canvas.drawPath(theoryPath, theoryPaint);

        fillPaint.setShader(fillGradient);
        canvas.drawPath(fillPath, fillPaint);
        canvas.drawPath(minePath, minePaint);

        for (int i = 0; i < mineX.length; i++) {
            canvas.drawCircle(mineX[i], mineY[i], dotRadius, dotFillPaint);
            canvas.drawCircle(mineX[i], mineY[i], dotRadius, dotStrokePaint);
        }

        float radius = labelRect.height() / 2f;
        canvas.drawRoundRect(labelRect, radius, radius, labelBgPaint);
        canvas.drawText(bestLabel,
                labelRect.centerX() - labelPaint.measureText(bestLabel) / 2f,
                labelRect.centerY() + 4f * density,
                labelPaint);
    }

    private void buildSmoothPath(float[] xs, float[] ys, Path path) {
        path.reset();
        int n = xs.length;
        path.moveTo(xs[0], ys[0]);
        for (int i = 0; i < n - 1; i++) {
            int i0 = i == 0 ? 0 : i - 1;
            int i3 = i + 2 < n ? i + 2 : i + 1;
            path.cubicTo(
                    xs[i] + (xs[i + 1] - xs[i0]) / 6f, ys[i] + (ys[i + 1] - ys[i0]) / 6f,
                    xs[i + 1] - (xs[i3] - xs[i]) / 6f, ys[i + 1] - (ys[i3] - ys[i]) / 6f,
                    xs[i + 1], ys[i + 1]);
        }
    }

    private int withAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    private int color(int resId) {
        return ContextCompat.getColor(getContext(), resId);
    }
}