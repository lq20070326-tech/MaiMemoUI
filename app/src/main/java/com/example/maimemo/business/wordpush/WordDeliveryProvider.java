package com.example.maimemo.business.wordpush;

import android.content.Context;
import androidx.annotation.Nullable;
import com.example.maimemo.data.repository.WordRepository;

/** Word-push extension point. Return computed push payloads; replace provider to change the strategy. */
public interface WordDeliveryProvider {
    @Nullable WordDeliveryItem nextItem(Context context, String bookName);
    void recordAnswer(Context context, WordRepository.Word word, int rating);
    String getProviderId();
}
