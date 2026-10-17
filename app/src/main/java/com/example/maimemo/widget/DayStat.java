package com.example.maimemo.widget;

/**
 * 某一天的单词熟悉度数据：认识 / 模糊 / 不认识 三段，用于绘制日历上的多色段环。
 */
public class DayStat {

    private final int mastered;
    private final int fuzzy;
    private final int unknown;

    public DayStat(int mastered, int fuzzy, int unknown) {
        this.mastered = mastered;
        this.fuzzy = fuzzy;
        this.unknown = unknown;
    }

    public int getMastered() {
        return mastered;
    }

    public int getFuzzy() {
        return fuzzy;
    }

    public int getUnknown() {
        return unknown;
    }

    public int getTotal() {
        return mastered + fuzzy + unknown;
    }
}