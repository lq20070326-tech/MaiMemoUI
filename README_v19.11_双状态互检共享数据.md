# v19.11 双状态互检与共享数据修复

## 设计
- App 内使用 AutoStudyActivity；App 外在系统允许悬浮窗时使用 v15 风格 AutoStudyOverlay。
- 两个展示端不同时出现：Activity 的 PRESENTED 回执会取消外部悬浮窗和单词通知；悬浮窗出现后回执给前台服务，取消重复通知并标记当前单词正在展示。
- 两端使用同一 WordRepository 数据源。外部弹窗的评分仍调用 WordRepository.markLearned，并通过 ACTION_ANSWERED 通知同一个 AutoStudyForegroundService；新词组进度由同一 AutoStudyGroupQueue.advance 维护。
- 若没有悬浮窗权限或 WindowManager 添加窗口失败，回退到带有同一单词参数的通知/AutoStudyActivity。

## 重要限制
Android 系统可能限制后台 Activity 或悬浮窗启动。App 外悬浮弹窗需要用户已授予“显示在其他应用上层”权限；若权限未开启，使用通知回退。此源码包尚未完成 Gradle 编译和真机测试。
