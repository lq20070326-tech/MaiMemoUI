package com.example.maimemo.business.wordpush;

import androidx.annotation.Nullable;
import com.example.maimemo.data.repository.WordRepository;

/** A complete word-push payload, including the reason and current FSRS card state. */
public final class WordDeliveryItem {
    public enum Source { DUE_REVIEW, NEW_WORD, CUSTOM }
    public final WordRepository.Word word;
    public final Source source;
    @Nullable public final String dueAt;
    public final double stabilityDays;
    public final double difficulty;
    public final double retrievability;
    public final int reviewCount;
    public final int lapses;
    public final int[] allowedRatings;
    public final String providerId;

    public WordDeliveryItem(WordRepository.Word word, Source source, @Nullable String dueAt,
                            double stabilityDays, double difficulty, double retrievability,
                            int reviewCount, int lapses, int[] allowedRatings, String providerId) {
        this.word = word; this.source = source; this.dueAt = dueAt;
        this.stabilityDays = stabilityDays; this.difficulty = difficulty;
        this.retrievability = retrievability; this.reviewCount = reviewCount;
        this.lapses = lapses; this.allowedRatings = allowedRatings == null ? new int[]{1, 2, 3, 4} : allowedRatings.clone();
        this.providerId = providerId == null ? "custom" : providerId;
    }
}
