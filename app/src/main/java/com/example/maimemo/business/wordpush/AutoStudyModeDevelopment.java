package com.example.maimemo.business.wordpush;

import android.content.Context;

import androidx.annotation.Nullable;

import com.example.maimemo.data.repository.WordRepository;

/**
 * 后期开发占位文件：学习设置中的“全自动”模式目前只保留选项，不在本类启用自动评分或自动跳词。
 *
 * <h3>单词推送数据接口（后续开发时参考）</h3>
 * 1. 获取下一张卡片：WordDeliveryItem item = WordPushService.nextItem(context, bookName);
 * 2. 读取单词：item.word.headWord / meaning / phonetic / sentence / sentenceCn。
 * 3. 读取推送来源：item.source（DUE_REVIEW / NEW_WORD / CUSTOM）。
 * 4. 读取排程状态：item.dueAt、stabilityDays、difficulty、retrievability、reviewCount、lapses。
 * 5. 提交评分：WordPushService.recordAnswer(context, item.word, rating)，rating 遵循 FSRS-6：
 *    1=Again、2=Hard、3=Good、4=Easy。
 * 6. 替换推送策略：WordPushService.setProvider(实现 WordDeliveryProvider 的新策略)。
 *
 * 后续实现全自动流程时，应明确“自动显示释义”与“自动评分/自动跳过”是不同能力；
 * 不要因为自动模式而伪造用户评分。统计数据必须通过 recordAnswer / WordRepository.markLearned
 * 形成真实学习事件后再写入 SQLite。
 */
public final class AutoStudyModeDevelopment {
    private AutoStudyModeDevelopment() { }

    /** 示例接口签名，当前不接入 StudyActivity，不执行实际推送。 */
    @Nullable
    public static WordDeliveryItem getNextWordForFutureAutoMode(Context context, String bookName) {
        // TODO 后期开发：根据全自动策略生成/选择推送数据。
        // return WordPushService.nextItem(context, bookName);
        return null;
    }

    /** 示例评分入口，当前不调用；只有用户真实作答后才能提交评分。 */
    public static void submitRealAnswerForFutureAutoMode(Context context, WordRepository.Word word, int fsrsRating) {
        // TODO 后期开发：接入自动模式的真实交互后，调用统一接口保存 FSRS-6 状态和统计日志。
        // WordPushService.recordAnswer(context, word, fsrsRating);
    }
}
