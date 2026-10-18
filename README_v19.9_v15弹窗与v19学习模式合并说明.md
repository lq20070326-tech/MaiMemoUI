# 必须背 v19.9：v15 弹窗实现 + v19 学习模式

## 本次合并原则
- 基于 v19.8 项目源码继续开发。
- 仅将 v15 的 `AutoStudyOverlay.java` 恢复到项目中；没有用 v15 的 `AutoStudyActivity.java` 覆盖 v19 版本。
- 保留 v19 的 `AutoStudyGroupQueue.java`、`AutoStudyForegroundService.java`、`AutoStudyActivity.java`，以保留分组熟悉、组内背诵、组末测试、到期复习优先级和进度持久化。
- 保留 v19 的评分闪退保护、自动发音、随机新词选择及后台弹窗/通知回退代码。

## 验证状态
- 已确认 v19 的分组状态机源码仍在项目中。
- 已确认弹窗类使用 v15 文件内容。
- 当前环境之前无法连接 `services.gradle.org` 下载 Gradle 9.4.1，因此尚未完成 Gradle 编译与真机弹窗验证。Android 系统是否允许后台显示全屏弹窗仍取决于系统版本、通知权限及全屏通知权限。
