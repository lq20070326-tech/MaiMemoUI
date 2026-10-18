# 必须背 v19.10：统一 App 内外学习模式

## 修复内容
- 自动推送首次尝试打开 `AutoStudyActivity`，保留 v19 分组学习状态机。
- 如果 Android 阻止后台直接启动 Activity，降级为通知/全屏通知入口；点击入口打开同一个 `AutoStudyActivity` 并携带同一单词与来源参数。
- 不再调用旧版 `AutoStudyOverlay.show()` 作为学习卡片。旧 Overlay 内有独立的简化评分和单词小测流程，会绕过 v19 分组熟悉、组内回忆、组末测试及持久化进度，因此从自动推送路径中禁用。
- 保留 v19 `AutoStudyActivity`、`AutoStudyGroupQueue`、FSRS 和进度持久化代码。

## 验证限制
已完成源码修改与静态检查；尚未完成 Gradle 编译和 Android 真机验证。Android 是否允许后台弹出 Activity/全屏通知仍受系统版本、通知权限、全屏通知权限和通知渠道设置影响。若系统拦截，用户需点击通知进入同一学习页面。
