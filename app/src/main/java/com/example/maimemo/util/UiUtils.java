package com.example.maimemo.util;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.example.maimemo.R;

/**
 * 页面通用小工具：统一处理「返回 + 标题」顶栏、设置项绑定，以及统一的 Toast 提示。
 */
public final class UiUtils {

    private UiUtils() {
    }

    /** 绑定页面顶栏：返回按钮关闭当前页，并设置标题 */
    public static void setupTopBar(AppCompatActivity activity, String title) {
        View back = activity.findViewById(R.id.btn_back);
        if (back != null) {
            back.setOnClickListener(v -> activity.finish());
        }
        TextView tvTitle = activity.findViewById(R.id.tv_title);
        if (tvTitle != null) {
            tvTitle.setText(title);
        }
    }

    /** 绑定 view_setting_row.xml 的标题、图标与右侧说明文字 */
    public static void bindRow(View root, int rowId, int iconRes, String title, String value) {
        View row = root.findViewById(rowId);
        if (row == null) {
            return;
        }
        ImageView icon = row.findViewById(R.id.iv_row_icon);
        if (icon != null && iconRes != 0) {
            icon.setImageResource(iconRes);
        }
        TextView tvTitle = row.findViewById(R.id.tv_row_title);
        if (tvTitle != null) {
            tvTitle.setText(title);
        }
        TextView tvValue = row.findViewById(R.id.tv_row_value);
        if (tvValue != null) {
            tvValue.setText(value == null ? "" : value);
        }
    }

    public static void bindRow(Activity activity, int rowId, int iconRes, String title, String value) {
        bindRow(activity.getWindow().getDecorView(), rowId, iconRes, title, value);
    }

    /** 绑定 view_setting_switch_row.xml 的标题、图标与开关状态 */
    public static void bindSwitchRow(View root, int rowId, int iconRes, String title, boolean checked) {
        View row = root.findViewById(rowId);
        if (row == null) {
            return;
        }
        ImageView icon = row.findViewById(R.id.iv_row_icon);
        if (icon != null && iconRes != 0) {
            icon.setImageResource(iconRes);
        }
        TextView tvTitle = row.findViewById(R.id.tv_row_title);
        if (tvTitle != null) {
            tvTitle.setText(title);
        }
        SwitchCompat switchCompat = row.findViewById(R.id.sw_row);
        if (switchCompat != null) {
            switchCompat.setChecked(checked);
        }
    }

    public static void bindSwitchRow(Activity activity, int rowId, int iconRes, String title, boolean checked) {
        bindSwitchRow(activity.getWindow().getDecorView(), rowId, iconRes, title, checked);
    }

    public static void toast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
}