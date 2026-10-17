package com.example.maimemo.business.wordpush;

import android.content.Context;
import androidx.annotation.Nullable;
import com.example.maimemo.data.repository.WordRepository;

/** Stable facade/API for word delivery. UI depends on this, not on the selection strategy. */
public final class WordPushService {
    private static volatile WordDeliveryProvider provider = new FsrsWordDeliveryProvider();
    private WordPushService() { }
    public static void setProvider(WordDeliveryProvider replacement) {
        if (replacement == null) throw new IllegalArgumentException("provider cannot be null");
        provider = replacement;
    }
    public static WordDeliveryProvider getProvider() { return provider; }
    @Nullable public static WordDeliveryItem nextItem(Context context, String bookName) {
        return provider.nextItem(context.getApplicationContext(), bookName);
    }
    @Nullable public static WordRepository.Word nextWord(Context context, String bookName) {
        WordDeliveryItem item = nextItem(context, bookName);
        return item == null ? null : item.word;
    }
    public static void recordAnswer(Context context, WordRepository.Word word, int rating) {
        provider.recordAnswer(context.getApplicationContext(), word, rating);
    }
}
