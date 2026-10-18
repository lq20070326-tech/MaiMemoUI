package com.example.maimemo.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.maimemo.R;

import java.util.Calendar;
import java.util.Collections;
import java.util.Map;

/**
 * 必须背风格的月历控件。
 * 每个日期外圈用多色段环表示当天的单词熟悉情况：
 * 绿色 = 认识，橙色 = 模糊，红色 = 不认识；未来日期不绘制环。
 *
 * 性能约定：颜色 / 字号 / 密度 / 字体度量的对象与数值都在构造期算好，
 * onDraw 里只做绘制，不产生任何对象分配，也不查资源。
 */
public class RingCalendarView extends View {

    private static final String[] WEEK_LABELS = {"日", "一", "二", "三", "四", "五", "六"};
    private static final String[] DAY_LABELS = new String[32];

    static {
        for (int i = 1; i <= 31; i++) {
            DAY_LABELS[i] = String.valueOf(i);
        }
    }

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint todayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint numberPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint numberBoldPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint weekPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Paint.FontMetrics fontMetrics = new Paint.FontMetrics();
    private final RectF arcRect = new RectF();
    private final int[] segmentValues = new int[3];
    private final int[] segmentColors = new int[3];

    private final int colorTrack;
    private final int colorMastered;
    private final int colorFuzzy;
    private final int colorUnknown;
    private final int colorTodayBg;
    private final int colorGreen;
    private final int colorHint;
    private final int colorPrimary;
    private final int colorSecondary;

    private final float ringStroke;
    private final float todayPadding;

    private final Calendar calendar = Calendar.getInstance();

    private int year;
    private int month;
    private int firstWeekday;
    private int daysInMonth;
    private int todayYear;
    private int todayMonth;
    private int todayDay;

    private Map<Integer, DayStat> dayStats = Collections.emptyMap();

    public RingCalendarView(Context context) {
        this(context, null);
    }

    public RingCalendarView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RingCalendarView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        float density = getResources().getDisplayMetrics().density;
        float scaledDensity = getResources().getDisplayMetrics().scaledDensity;
        ringStroke = 2.5f * density;
        todayPadding = 3.5f * density;

        colorTrack = color(R.color.ring_empty);
        colorMastered = color(R.color.ring_mastered);
        colorFuzzy = color(R.color.ring_fuzzy);
        colorUnknown = color(R.color.ring_unknown);
        colorTodayBg = color(R.color.brand_green_light);
        colorGreen = color(R.color.brand_green);
        colorHint = color(R.color.text_hint);
        colorPrimary = color(R.color.text_primary);
        colorSecondary = color(R.color.text_secondary);

        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeWidth(ringStroke);
        trackPaint.setColor(colorTrack);

        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(ringStroke);
        ringPaint.setStrokeCap(Paint.Cap.ROUND);

        todayPaint.setStyle(Paint.Style.FILL);
        todayPaint.setColor(colorTodayBg);

        numberPaint.setTextAlign(Paint.Align.CENTER);
        numberPaint.setTextSize(12.5f * scaledDensity);

        numberBoldPaint.setTextAlign(Paint.Align.CENTER);
        numberBoldPaint.setTextSize(12.5f * scaledDensity);
        numberBoldPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        weekPaint.setTextAlign(Paint.Align.CENTER);
        weekPaint.setTextSize(12f * scaledDensity);
        weekPaint.setColor(colorSecondary);

        segmentColors[0] = colorMastered;
        segmentColors[1] = colorFuzzy;
        segmentColors[2] = colorUnknown;

        updateToday();
        year = todayYear;
        month = todayMonth;
        updateMonth();
    }

    public void setMonth(int year, int month) {
        this.year = year;
        this.month = month;
        updateToday();
        updateMonth();
        invalidate();
    }

    public void shiftMonth(int delta) {
        int m = month + delta;
        int y = year;
        if (m < 1) {
            m += 12;
            y -= 1;
        } else if (m > 12) {
            m -= 12;
            y += 1;
        }
        month = m;
        year = y;
        updateToday();
        updateMonth();
        invalidate();
    }

    public int currentYear() {
        return year;
    }

    public int currentMonth() {
        return month;
    }

    public void setDayStats(Map<Integer, DayStat> stats) {
        dayStats = stats == null ? Collections.<Integer, DayStat>emptyMap() : stats;
        invalidate();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        updateToday();
        updateMonth();
    }

    private void updateToday() {
        calendar.setTimeInMillis(System.currentTimeMillis());
        todayYear = calendar.get(Calendar.YEAR);
        todayMonth = calendar.get(Calendar.MONTH) + 1;
        todayDay = calendar.get(Calendar.DAY_OF_MONTH);
    }

    /** 只在切换月份时重算一次，避免每次绘制都创建 Calendar */
    private void updateMonth() {
        calendar.clear();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month - 1);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        firstWeekday = calendar.get(Calendar.DAY_OF_WEEK) - 1;
        daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        int desired = (int) (w / 7f * 7.05f);
        setMeasuredDimension(w, resolveSize(desired, heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        if (w <= 0f || h <= 0f) {
            return;
        }

        float cellW = w / 7f;
        float headerH = cellW * 0.85f;
        float cellH = (h - headerH) / 6f;

        // 星期表头
        float weekBaseline = headerH * 0.72f;
        for (int i = 0; i < 7; i++) {
            canvas.drawText(WEEK_LABELS[i], cellW * i + cellW / 2f, weekBaseline, weekPaint);
        }

        // 数字垂直居中所需偏移，整屏只取一次字体度量
        numberPaint.getFontMetrics(fontMetrics);
        float textOffsetY = -(fontMetrics.ascent + fontMetrics.descent) / 2f;

        float radius = (cellW < cellH ? cellW : cellH) * 0.35f;
        for (int day = 1; day <= daysInMonth; day++) {
            int index = firstWeekday + day - 1;
            float cx = (index % 7) * cellW + cellW / 2f;
            float cy = headerH + (index / 7) * cellH + cellH / 2f;
            drawDay(canvas, cx, cy, radius, day, textOffsetY);
        }
    }

    private void drawDay(Canvas canvas, float cx, float cy, float r, int day, float textOffsetY) {
        boolean isToday = year == todayYear && month == todayMonth && day == todayDay;
        boolean isFuture = isFuture(day);
        DayStat stat = dayStats.get(day);

        if (isToday) {
            canvas.drawCircle(cx, cy, r + todayPadding, todayPaint);
        }

        if (!isFuture) {
            arcRect.set(cx - r, cy - r, cx + r, cy + r);
            if (stat == null || stat.getTotal() == 0) {
                canvas.drawArc(arcRect, 0f, 360f, false, trackPaint);
            } else {
                drawSegments(canvas, stat);
            }
        }

        Paint paint = isToday ? numberBoldPaint : numberPaint;
        if (isToday) {
            paint.setColor(colorGreen);
        } else if (isFuture) {
            paint.setColor(colorHint);
        } else if (stat != null && stat.getTotal() > 0) {
            paint.setColor(colorPrimary);
        } else {
            paint.setColor(colorSecondary);
        }
        canvas.drawText(DAY_LABELS[day], cx, cy + textOffsetY, paint);
    }

    /** 按 认识 / 模糊 / 不认识 的比例画多色段环，段与段之间留白 */
    private void drawSegments(Canvas canvas, DayStat stat) {
        segmentValues[0] = stat.getMastered();
        segmentValues[1] = stat.getFuzzy();
        segmentValues[2] = stat.getUnknown();

        int visibleCount = 0;
        float total = 0f;
        for (int i = 0; i < 3; i++) {
            int value = segmentValues[i];
            if (value > 0) {
                visibleCount++;
                total += value;
            }
        }
        if (visibleCount == 0) {
            return;
        }

        float gap = 12f;
        float available = 360f - gap * visibleCount;
        float start = -90f + gap / 2f;
        for (int i = 0; i < 3; i++) {
            int value = segmentValues[i];
            if (value <= 0) {
                continue;
            }
            float sweep = available * (value / total);
            ringPaint.setColor(segmentColors[i]);
            canvas.drawArc(arcRect, start, sweep, false, ringPaint);
            start += sweep + gap;
        }
    }

    private boolean isFuture(int day) {
        if (year != todayYear) {
            return year > todayYear;
        }
        if (month != todayMonth) {
            return month > todayMonth;
        }
        return day > todayDay;
    }

    private int color(int resId) {
        return ContextCompat.getColor(getContext(), resId);
    }
}