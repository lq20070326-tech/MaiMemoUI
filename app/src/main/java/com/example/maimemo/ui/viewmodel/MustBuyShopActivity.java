package com.example.maimemo.ui.viewmodel;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.content.ContextCompat;
import com.example.maimemo.R;
import com.example.maimemo.data.database.LocalDatabase;
import java.text.SimpleDateFormat;
import java.util.Locale;

/** 必须背本地兑换商店：拼购式商品卡片；学习时长是应用内积分，不涉及真实支付。 */
public class MustBuyShopActivity extends AppCompatActivity {
    private long studySeconds;
    private TextView balance, owned;
    private final String[] names={"自由时间券 · 5 分钟","自由时间券 · 15 分钟","自由时间券 · 30 分钟"};
    private final String[] subtitles={"轻松休息一下","热门兑换选择","长时自由安排"};
    private final int[] costs={10,25,45}, rewards={5,15,30};
    private final int[] banners={0xFFE4F5E9,0xFFFFEBD7,0xFFFFE3E3};
    private int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}

    @Override protected void onCreate(@Nullable Bundle state){
        super.onCreate(state);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);
        studySeconds=estimateSeconds();
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true);
        scroll.setBackgroundColor(ContextCompat.getColor(this,R.color.page_bg));
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16),dp(8),dp(16),dp(24)); scroll.addView(root);
        // Insets are applied once after setContentView to avoid duplicated top padding. 

        TextView back=txt("‹",34,R.color.brand_green); back.setGravity(Gravity.CENTER_VERTICAL);
        back.setOnClickListener(v->finish()); root.addView(back,new LinearLayout.LayoutParams(-1,dp(42)));
        TextView title=txt("必须背商店",26,R.color.text_primary); title.setTypeface(null,Typeface.BOLD); root.addView(title);
        TextView intro=txt("用学习时长兑换你的专属自由时间",14,R.color.text_secondary);
        LinearLayout.LayoutParams ilp=lp(); ilp.topMargin=dp(4); ilp.bottomMargin=dp(14); root.addView(intro,ilp);

        LinearLayout wallet=card(); wallet.setOrientation(LinearLayout.VERTICAL);
        LinearLayout walletTop=new LinearLayout(this); walletTop.setOrientation(LinearLayout.HORIZONTAL); walletTop.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout walletLeft=new LinearLayout(this); walletLeft.setOrientation(LinearLayout.VERTICAL);
        walletLeft.addView(txt("我的学习时长",13,R.color.text_secondary));
        balance=txt(format(studySeconds),25,R.color.brand_green); balance.setTypeface(null,Typeface.BOLD);
        LinearLayout.LayoutParams balp=lp(); balp.topMargin=dp(3); walletLeft.addView(balance,balp);
        walletTop.addView(walletLeft,new LinearLayout.LayoutParams(0,-2,1));
        TextView walletIcon=txt("⏱",34,R.color.brand_green); walletIcon.setGravity(Gravity.CENTER);
        walletTop.addView(walletIcon,new LinearLayout.LayoutParams(dp(58),dp(58))); wallet.addView(walletTop);
        TextView note=txt("按学习记录估算，每段相邻记录最多计入 3 分钟；不涉及真实支付。",12,R.color.text_secondary);
        LinearLayout.LayoutParams notelp=lp(); notelp.topMargin=dp(8); wallet.addView(note,notelp);
        LinearLayout.LayoutParams wlp=lp(); wlp.bottomMargin=dp(18); root.addView(wallet,wlp);

        LinearLayout section=new LinearLayout(this); section.setGravity(Gravity.CENTER_VERTICAL);
        TextView sectionTitle=txt("限时兑换",20,R.color.text_primary); sectionTitle.setTypeface(null,Typeface.BOLD); section.addView(sectionTitle);
        TextView sectionHint=txt("人气好物",12,0xFFE34C4C); sectionHint.setGravity(Gravity.CENTER);
        GradientDrawable hintBg=new GradientDrawable(); hintBg.setColor(0xFFFFE8E8); hintBg.setCornerRadius(dp(8)); sectionHint.setBackground(hintBg); sectionHint.setPadding(dp(8),dp(4),dp(8),dp(4));
        LinearLayout.LayoutParams hintLp=new LinearLayout.LayoutParams(-2,-2); hintLp.leftMargin=dp(8); section.addView(sectionHint,hintLp);
        LinearLayout.LayoutParams slp=lp(); slp.bottomMargin=dp(10); root.addView(section,slp);

        // 两列商品网格，采用电商常见的商品图块、价格、标签和醒目兑换按钮。
        LinearLayout row1=new LinearLayout(this); row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.addView(productCard(0),new LinearLayout.LayoutParams(0,-2,1));
        LinearLayout.LayoutParams second=new LinearLayout.LayoutParams(0,-2,1); second.leftMargin=dp(10); row1.addView(productCard(1),second);
        root.addView(row1,lp());
        LinearLayout row2=new LinearLayout(this); row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.addView(productCard(2),new LinearLayout.LayoutParams(0,-2,1));
        View empty=new View(this); row2.addView(empty,new LinearLayout.LayoutParams(0,1,1));
        LinearLayout.LayoutParams r2lp=lp(); r2lp.topMargin=dp(10); root.addView(row2,r2lp);

        owned=txt("已兑换自由时间券："+getPreferences(0).getInt("free_time_minutes",0)+" 分钟",14,R.color.text_secondary);
        LinearLayout.LayoutParams olp=lp(); olp.topMargin=dp(16); root.addView(owned,olp);
        setContentView(scroll);
        com.example.maimemo.util.SystemBarUtils.apply(this, scroll);
    }

    private View productCard(int i){
        LinearLayout card=card(); card.setPadding(dp(10),dp(10),dp(10),dp(12));
        LinearLayout image=new LinearLayout(this); image.setOrientation(LinearLayout.VERTICAL); image.setGravity(Gravity.CENTER);
        GradientDrawable imageBg=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{banners[i],0xFFFFFFFF}); imageBg.setCornerRadius(dp(10)); image.setBackground(imageBg);
        TextView icon=txt(i==0?"☕":i==1?"⌛":"🌟",34,R.color.text_primary); icon.setGravity(Gravity.CENTER); image.addView(icon,new LinearLayout.LayoutParams(-1,dp(70)));
        TextView badge=txt(i==1?"热卖推荐":i==2?"超值时长":"新人优选",10,i==2?0xFFB53A3A:R.color.brand_green); badge.setGravity(Gravity.CENTER);
        image.addView(badge); card.addView(image,new LinearLayout.LayoutParams(-1,dp(94)));
        TextView name=txt(names[i],14,R.color.text_primary); name.setTypeface(null,Typeface.BOLD); name.setMaxLines(2);
        LinearLayout.LayoutParams nlp=lp(); nlp.topMargin=dp(9); card.addView(name,nlp);
        TextView sub=txt(subtitles[i],11,R.color.text_secondary); LinearLayout.LayoutParams sublp=lp(); sublp.topMargin=dp(3); card.addView(sub,sublp);
        LinearLayout priceRow=new LinearLayout(this); priceRow.setGravity(Gravity.CENTER_VERTICAL); priceRow.setOrientation(LinearLayout.HORIZONTAL);
        TextView price=txt(String.valueOf(costs[i]),22,0xFFE34C4C); price.setTypeface(null,Typeface.BOLD); priceRow.addView(price);
        TextView unit=txt(" 分钟",11,0xFFE34C4C); unit.setGravity(Gravity.BOTTOM); priceRow.addView(unit);
        LinearLayout.LayoutParams plp=lp(); plp.topMargin=dp(8); card.addView(priceRow,plp);
        TextView reward=txt("兑换后获得 "+rewards[i]+" 分钟",11,R.color.text_secondary); LinearLayout.LayoutParams rlp=lp(); rlp.topMargin=dp(1); card.addView(reward,rlp);
        Button buy=new Button(this); buy.setText("立即兑换"); buy.setAllCaps(false); buy.setTextSize(12); buy.setTypeface(null,Typeface.BOLD);
        buy.setTextColor(ContextCompat.getColor(this,R.color.white)); buy.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFE34C4C));
        LinearLayout.LayoutParams blp=new LinearLayout.LayoutParams(-1,dp(40)); blp.topMargin=dp(10); card.addView(buy,blp);
        buy.setOnClickListener(v->purchase(i));
        return card;
    }

    private void purchase(int i){
        long available=studySeconds/60;
        if(available<costs[i]){Toast.makeText(this,"学习时长不足，还需要 "+(costs[i]-available)+" 分钟",Toast.LENGTH_LONG).show();return;}
        SharedPreferences p=getPreferences(0);
        long spent=p.getLong("spent_seconds",0)+costs[i]*60L;
        int total=p.getInt("free_time_minutes",0)+rewards[i];
        p.edit().putLong("spent_seconds",spent).putInt("free_time_minutes",total).apply();
        studySeconds=Math.max(0,studySeconds-costs[i]*60L); balance.setText(format(studySeconds));
        if(owned!=null)owned.setText("已兑换自由时间券："+total+" 分钟");
        Toast.makeText(this,"兑换成功：已获得 "+rewards[i]+" 分钟自由时间券",Toast.LENGTH_LONG).show();
    }
    private long estimateSeconds(){
        long seconds=0; String prev=null; SQLiteDatabase db=LocalDatabase.get(this);
        try(Cursor c=db.rawQuery("SELECT learned_at FROM app_learning_log ORDER BY learned_at ASC",null)){
            while(c.moveToNext()){String now=c.getString(0);long a=parse(now),b=parse(prev);if(a>0&&b>0&&a>b)seconds+=Math.min((a-b)/1000,180);prev=now;}
        }catch(Exception ignored){}
        long spent=getPreferences(0).getLong("spent_seconds",0);return Math.max(0,seconds-spent);
    }
    private long parse(String s){try{return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.US).parse(s).getTime();}catch(Exception e){return -1;}}
    private String format(long s){long m=s/60;return (m/60>0?(m/60)+" 小时 ":"")+(m%60)+" 分钟";}
    private TextView txt(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color < 0 ? color : ContextCompat.getColor(this,color));return t;}
    private LinearLayout card(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(14),dp(14),dp(14),dp(14));c.setBackgroundResource(R.drawable.bg_card);return c;}
    private LinearLayout.LayoutParams lp(){return new LinearLayout.LayoutParams(-1,-2);}
}
