package com.example.maimemo.ui.viewmodel;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.maimemo.R;

import java.util.ArrayList;
import java.util.List;

/**
 * 统计页：顶部三个板块（遗忘曲线 / 学习情况 / 记忆持久度）。
 * 点击板块可滑动切换到对应页面，也支持左右手势滑动切换。
 */
public class StatsFragment extends Fragment {

    private final List<TextView> tabs = new ArrayList<>();
    private ViewPager2 pager;

    public StatsFragment() {
        super(R.layout.fragment_stats);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tabs.clear();
        tabs.add(view.findViewById(R.id.tab_forget));
        tabs.add(view.findViewById(R.id.tab_study));
        tabs.add(view.findViewById(R.id.tab_memory));

        pager = view.findViewById(R.id.pager);
        pager.setAdapter(new SectionAdapter(this));
        pager.setOffscreenPageLimit(1);
        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateStrip(position);
            }
        });

        for (int i = 0; i < tabs.size(); i++) {
            final int index = i;
            tabs.get(i).setOnClickListener(v -> pager.setCurrentItem(index, true));
        }

        updateStrip(0);
    }

    /** 顶部板块的选中态：白色胶囊 + 绿色加粗文字 */
    private void updateStrip(int index) {
        for (int i = 0; i < tabs.size(); i++) {
            boolean selected = i == index;
            TextView tab = tabs.get(i);
            tab.setBackgroundResource(selected ? R.drawable.bg_tab_selected : 0);
            tab.setTextColor(ContextCompat.getColor(requireContext(),
                    selected ? R.color.brand_green : R.color.text_secondary));
            tab.setTypeface(selected ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        }
    }

    private static class SectionAdapter extends FragmentStateAdapter {

        SectionAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (position == 1) {
                return new StatsStudyFragment();
            }
            if (position == 2) {
                return new StatsMemoryFragment();
            }
            return new StatsForgettingFragment();
        }

        @Override
        public int getItemCount() {
            return 3;
        }
    }
}