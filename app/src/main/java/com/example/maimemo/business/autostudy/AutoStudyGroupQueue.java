package com.example.maimemo.business.autostudy;

import android.content.Context;
import android.content.SharedPreferences;
import com.example.maimemo.data.repository.WordRepository;
import com.example.maimemo.storage.AppSettings;
import org.json.JSONArray;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Persistent state machine for automatic study: familiarity -> recall -> one group quiz. */
public final class AutoStudyGroupQueue {
    public static final String FAMILIARITY = "FAMILIARITY", RECALL = "RECALL", QUIZ = "QUIZ";
    private static final String PREFS="auto_study_group_progress", BOOK="book", WORDS="words", ALL_WORDS="all_words", QUIZ_WORDS="quiz_words";
    private static final String INDEX="index", GROUP="group_no", PHASE="phase", ROUND="round", OFFSET="offset", QUIZ_INDEX="quiz_index", ORDER_VERSION="order_version";
    private AutoStudyGroupQueue() {}
    private static SharedPreferences prefs(Context c){return c.getApplicationContext().getSharedPreferences(PREFS,Context.MODE_PRIVATE);}
    private static JSONArray array(SharedPreferences p,String key){try{return new JSONArray(p.getString(key,"[]"));}catch(Exception e){return new JSONArray();}}
    private static int[] ranks(JSONArray a){int[] r=new int[a.length()];for(int i=0;i<r.length;i++)r[i]=a.optInt(i,-1);return r;}

    /**
     * Explicitly abandon the active group when the selected wordbook changes.
     * Ratings already submitted by the learner are persisted by WordRepository;
     * untouched words are deliberately left unlearned. The next current() call
     * creates a fresh randomized group from the newly selected wordbook.
     */
    public static synchronized void resetForBookSwitch(Context c,String newBook){
        SharedPreferences p=prefs(c);
        String oldBook=p.getString(BOOK,"");
        if(newBook==null || newBook.trim().isEmpty() || newBook.equals(oldBook)) return;
        p.edit()
          .putString(BOOK, "")
          .putString(WORDS, "[]")
          .putString(ALL_WORDS, "[]")
          .putString(QUIZ_WORDS, "[]")
          .putString(PHASE, FAMILIARITY)
          .putInt(INDEX, 0).putInt(QUIZ_INDEX, 0)
          .putInt(ROUND, 0).putInt(OFFSET, 0).putInt(GROUP, 0)
          .putInt(ORDER_VERSION, 0).commit();
    }

    public static synchronized WordRepository.Word current(Context c,String book){
        if(book==null||book.trim().isEmpty())return null;
        SharedPreferences p=prefs(c); JSONArray words=array(p,WORDS); String saved=p.getString(BOOK,"");
        if(!book.equals(saved)||words.length()==0){if(!createGroup(c,book,p,0))return null;words=array(p,WORDS);}
        String phase=p.getString(PHASE,FAMILIARITY); int index=p.getInt(INDEX,0);
        JSONArray list=QUIZ.equals(phase)?array(p,QUIZ_WORDS):words;
        if(QUIZ.equals(phase)) index=p.getInt(QUIZ_INDEX,0);
        if(list.length()==0 || index>=list.length()){
            if(QUIZ.equals(phase)){ // quiz queue empty means group passed
                int offset=p.getInt(OFFSET,0)+words.length();
                if(!createGroup(c,book,p,offset))return null;
                words=array(p,WORDS); list=words; index=0;
            } else if(index>=list.length()) return null;
        }
        int rank=list.optInt(index,-1); return rank<0?null:WordRepository.getWordByRank(c,book,rank);
    }
    private static boolean createGroup(Context c,String book,SharedPreferences p,int offset){
        JSONArray all=array(p,ALL_WORDS);
        if(!book.equals(p.getString(BOOK,""))||all.length()==0){
            List<WordRepository.Word> candidates=WordRepository.getWords(c,book,10000,true);
            if(candidates.isEmpty())return false;
            // Randomize the entire unseen pool once and persist that order so groups are
            // random but stable across process restarts. onlyUnlearned excludes learned words.
            Collections.shuffle(candidates);
            all=new JSONArray();for(WordRepository.Word w:candidates)all.put(w.rank);
            p.edit().putString(BOOK,book).putString(ALL_WORDS,all.toString()).putInt(GROUP,0).commit();
        }
        if(offset>=all.length())return false;
        int size=Math.max(1,Math.min(50,AppSettings.getInt(c,AppSettings.AUTO_GROUP_SIZE,10)));
        // Upgrade older sequential queues without disturbing the group currently in progress.
        // Shuffle only future groups, then persist the randomized order across restarts.
        if(p.getInt(ORDER_VERSION,0)<2){
            int preserveUntil=Math.min(all.length(),offset+array(p,WORDS).length());
            List<Integer> tail=new ArrayList<>();
            for(int i=preserveUntil;i<all.length();i++)tail.add(all.optInt(i,-1));
            Collections.shuffle(tail);
            JSONArray randomized=new JSONArray();
            for(int i=0;i<preserveUntil;i++)randomized.put(all.optInt(i,-1));
            for(Integer rank:tail)randomized.put(rank);
            all=randomized;
            p.edit().putString(ALL_WORDS,all.toString()).putInt(ORDER_VERSION,2).commit();
        }
        JSONArray a=new JSONArray();for(int i=offset;i<Math.min(all.length(),offset+size);i++)a.put(all.optInt(i,-1));
        p.edit().putString(BOOK,book).putString(WORDS,a.toString()).putString(QUIZ_WORDS,"[]")
          .putInt(OFFSET,offset).putInt(INDEX,0).putInt(ROUND,0).putInt(QUIZ_INDEX,0)
          .putString(PHASE,FAMILIARITY).putInt(GROUP,p.getInt(GROUP,0)+1).commit();return a.length()>0;
    }
    public static synchronized String phase(Context c){return prefs(c).getString(PHASE,FAMILIARITY);}
    public static synchronized int round(Context c){return prefs(c).getInt(ROUND,0)+1;}
    public static synchronized int getGroupNumber(Context c){return prefs(c).getInt(GROUP,0);}
    public static synchronized int getPosition(Context c){return prefs(c).getInt(QUIZ.equals(phase(c))?QUIZ_INDEX:INDEX,0);}
    public static synchronized int getSize(Context c){return array(prefs(c),WORDS).length();}
    public static synchronized int quizRemaining(Context c){return array(prefs(c),QUIZ_WORDS).length();}
    public static synchronized String quizType(Context c){return AppSettings.getString(c,AppSettings.AUTO_GROUP_QUIZ_TYPE,"英文选择题");}

    /** Advance a completed familiarity exposure; no FSRS rating is recorded. */
    public static synchronized void familiarityDone(Context c,String book,int rank){
        SharedPreferences p=prefs(c);if(!book.equals(p.getString(BOOK,""))||!FAMILIARITY.equals(p.getString(PHASE,FAMILIARITY)))return;
        JSONArray a=array(p,WORDS);int i=p.getInt(INDEX,0);if(i>=a.length()||a.optInt(i,-1)!=rank)return;
        i++;int rounds=Math.max(1,Math.min(2,AppSettings.getInt(c,AppSettings.AUTO_FAMILIARITY_ROUNDS,1)));int round=p.getInt(ROUND,0);
        if(i>=a.length()){round++;if(round>=rounds)p.edit().putString(PHASE,RECALL).putInt(INDEX,0).putInt(ROUND,round).commit();else p.edit().putInt(INDEX,0).putInt(ROUND,round).commit();}
        else p.edit().putInt(INDEX,i).commit();
    }
    /** Called after a recall rating has been committed to FSRS. */
    public static synchronized void recallDone(Context c,String book,int rank){
        SharedPreferences p=prefs(c);if(!book.equals(p.getString(BOOK,""))||!RECALL.equals(p.getString(PHASE,RECALL)))return;
        JSONArray a=array(p,WORDS);int i=p.getInt(INDEX,0);if(i>=a.length()||a.optInt(i,-1)!=rank)return;i++;
        if(i>=a.length()){p.edit().putString(PHASE,QUIZ).putInt(INDEX,0).putInt(QUIZ_INDEX,0).putString(QUIZ_WORDS,a.toString()).commit();}
        else p.edit().putInt(INDEX,i).commit();
    }
    /** A quiz answer advances once; incorrect ranks remain in a retry queue. */
    public static synchronized void quizAnswer(Context c,String book,int rank,boolean correct){
        SharedPreferences p=prefs(c);if(!book.equals(p.getString(BOOK,""))||!QUIZ.equals(p.getString(PHASE,QUIZ)))return;
        JSONArray q=array(p,QUIZ_WORDS);int i=p.getInt(QUIZ_INDEX,0);if(i>=q.length()||q.optInt(i,-1)!=rank)return;
        JSONArray next=new JSONArray();
        // Rotate the current item to the end when wrong; remove it when correct.
        for(int n=i+1;n<q.length();n++)next.put(q.optInt(n,-1));
        for(int n=0;n<i;n++)next.put(q.optInt(n,-1));
        if(!correct)next.put(rank);
        p.edit().putString(QUIZ_WORDS,next.toString()).putInt(QUIZ_INDEX,0).commit();
    }
    /** Legacy API retained for callers from older presentation paths. */
    public static synchronized void advance(Context c,String book,int rank){if(FAMILIARITY.equals(phase(c)))familiarityDone(c,book,rank);else if(RECALL.equals(phase(c)))recallDone(c,book,rank);}
}
