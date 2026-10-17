package com.example.maimemo.business.autostudy;

/** Shared actions and extras for the automatic study session. */
public final class AutoStudyContract {
    private AutoStudyContract() { }
    public static final String ACTION_START = "com.example.maimemo.autostudy.START";
    public static final String ACTION_STOP = "com.example.maimemo.autostudy.STOP";
    public static final String ACTION_PRESENTED = "com.example.maimemo.autostudy.PRESENTED";
    public static final String ACTION_NOT_VISIBLE = "com.example.maimemo.autostudy.NOT_VISIBLE";
    public static final String ACTION_ANSWERED = "com.example.maimemo.autostudy.ANSWERED";
    public static final String ACTION_DISMISS = "com.example.maimemo.autostudy.DISMISS";
    public static final String EXTRA_BOOK = "book_name";
    public static final String EXTRA_RANK = "word_rank";
    public static final String EXTRA_WORD = "word";
    public static final String EXTRA_MEANING = "meaning";
    public static final String EXTRA_PHONETIC = "phonetic";
    public static final String EXTRA_POS = "pos";
    public static final String EXTRA_SENTENCE = "sentence";
    public static final String EXTRA_SENTENCE_CN = "sentence_cn";
    public static final String EXTRA_SOURCE = "source";
    public static final String EXTRA_RATING = "rating";
    public static final String CHANNEL_ID = "must_back_auto_study";
    public static final String WORD_CHANNEL_ID = "must_back_word_popup_v3";
    public static final int NOTIFICATION_ID = 7821;
    /** One replaceable notification for the currently pending word. */
    public static final int WORD_NOTIFICATION_ID = 7822;
}
