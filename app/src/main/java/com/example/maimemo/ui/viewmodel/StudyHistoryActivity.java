package com.example.maimemo.ui.viewmodel;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.content.ContextCompat;
import com.example.maimemo.R;
import com.example.maimemo.data.database.LocalDatabase;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** 本地学习历史：按天汇总评分记录，并保守估算专注时长。 */
public class StudyHistoryActivity extends AppCompatActivity {
    private int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + .5f); }
    @Override protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.page_bg));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(ContextCompat.getColor(this,R.color.page_bg));
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(16),dp(8),dp(16),dp(20)); scroll.addView(root);
        TextView back = text("‹", 32, R.color.brand_green); back.setGravity(Gravity.CENTER_VERTICAL); back.setOnClickListener(v->finish()); root.addView(back,new LinearLayout.LayoutParams(-1,dp(44)));
        TextView title=text("学习记录",25,R.color.text_primary); title.setTypeface(null,Typeface.BOLD); root.addView(title);
        TextView sub=text("只读取本机记录 · 时长按相邻学习事件保守估算",13,R.color.text_secondary); LinearLayout.LayoutParams slp=lp(); slp.topMargin=dp(5); slp.bottomMargin=dp(14); root.addView(sub,slp);
        long totalSeconds=0; int total=0, reviews=0, correct=0; java.util.LinkedHashMap<String,int[]> days=new java.util.LinkedHashMap<>();
        SQLiteDatabase db=LocalDatabase.get(this);
        try(Cursor c=db.rawQuery("SELECT learned_at,result,is_review FROM app_learning_log ORDER BY learned_at ASC",null)) {
            String previous=null;
            while(c.moveToNext()) {
                String stamp=c.getString(0); int rating=c.getInt(1); int review=c.getInt(2); total++; if(review==1)reviews++; if(rating>=2)correct++;
                String day=stamp!=null&&stamp.length()>=10?stamp.substring(0,10):"未知日期";
                int[] d=days.get(day); if(d==null){d=new int[4];days.put(day,d);} d[0]++; if(review==1)d[1]++; if(rating>=2)d[2]++; else d[3]++;
                long now=parse(stamp); long before=parse(previous); if(now>0&&before>0){long gap=(now-before)/1000; if(gap>0)totalSeconds+=Math.min(gap,180);} previous=stamp;
            }
        }
        LinearLayout summary=card(); summary.addView(text("累计学习",13,R.color.text_secondary)); summary.addView(text(total+" 次",25,R.color.text_primary)); summary.addView(text("复习 "+reviews+" 次  ·  回忆成功率 "+(total==0?0:Math.round(correct*100f/total))+"%",14,R.color.text_secondary)); summary.addView(text("估算专注时长："+duration(totalSeconds),14,R.color.brand_green)); LinearLayout.LayoutParams clp=lp();clp.bottomMargin=dp(12);root.addView(summary,clp);
        TextView heading=text("每日明细",17,R.color.text_primary);heading.setTypeface(null,Typeface.BOLD);LinearLayout.LayoutParams hlp=lp();hlp.bottomMargin=dp(8);root.addView(heading,hlp);
        java.util.ArrayList<String> keys=new java.util.ArrayList<>(days.keySet()); java.util.Collections.reverse(keys);
        if(keys.isEmpty()){LinearLayout empty=card();empty.addView(text("还没有学习记录",16,R.color.text_primary));empty.addView(text("完成一次单词学习后，这里会自动记录每日学习情况。",13,R.color.text_secondary));root.addView(empty);}
        else for(String day:keys){int[] d=days.get(day);LinearLayout c=card();LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setOrientation(LinearLayout.HORIZONTAL);TextView date=text(day,15,R.color.text_primary);date.setTypeface(null,Typeface.BOLD);row.addView(date,new LinearLayout.LayoutParams(0,-2,1));row.addView(text(d[0]+" 次",15,R.color.brand_green));c.addView(row);LinearLayout.LayoutParams rlp=lp();rlp.topMargin=dp(6);c.addView(text("复习 "+d[1]+"  ·  回忆成功 "+d[2]+"  ·  需再巩固 "+d[3],13,R.color.text_secondary),rlp);LinearLayout.LayoutParams one=lp();one.bottomMargin=dp(8);root.addView(c,one);}
        setContentView(scroll);
        com.example.maimemo.util.SystemBarUtils.apply(this, scroll);
    }
    private long parse(String s){try{return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.US).parse(s).getTime();}catch(Exception e){return -1;}}
    private String duration(long s){if(s<60)return s+" 秒";return (s/3600>0?(s/3600)+" 小时 ":"")+((s%3600)/60)+" 分钟";}
    private TextView text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(ContextCompat.getColor(this,color));return t;}
    private LinearLayout card(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(14),dp(13),dp(14),dp(13));c.setBackgroundResource(R.drawable.bg_card);return c;}
    private LinearLayout.LayoutParams lp(){return new LinearLayout.LayoutParams(-1,-2);}
}
