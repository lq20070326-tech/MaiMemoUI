package com.example.maimemo.business.wordpush;

import android.content.Context;
import androidx.annotation.Nullable;
import com.example.maimemo.data.repository.WordRepository;

/** Default provider: due FSRS reviews first, then new words within their own daily limit. */
public final class FsrsWordDeliveryProvider implements WordDeliveryProvider {
    @Nullable @Override public WordDeliveryItem nextItem(Context context, String bookName) {
        WordRepository.Word word = WordRepository.getNextWordFromDatabase(context, bookName);
        return word == null ? null : WordRepository.buildDeliveryItem(context, word, getProviderId());
    }
    @Override public void recordAnswer(Context context, WordRepository.Word word, int rating) {
        WordRepository.markLearned(context, word, rating);
    }
    @Override public String getProviderId() { return "fsrs6_due_first_v1"; }
}
