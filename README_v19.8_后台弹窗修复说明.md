# 必须背 v19.8：App 外推送弹窗修复

## 修复点
- 修复 v19.7 的 `AutoStudyOverlay.show()` 仅调用 `startActivity()` 却直接返回成功的问题。Android 后台启动限制可能拦截 Activity，导致服务误判弹窗已显示并不再走回退逻辑。
- 当前 overlay fallback 明确返回失败，让服务继续显示单词通知。
- 单词通知增加 `setFullScreenIntent()`，Android 系统策略允许时直接打开与 App 内相同的 `AutoStudyActivity`；不允许全屏展示时仍保留高优先级 heads-up 通知，点击后进入同一界面。
- 分组状态与词条 source 仍通过原有 `putWordExtras()` 传递。

## 验证状态
源码修改完成，ZIP 完整性和 XML 解析需通过打包前检查。当前环境未完成 Android Gradle 编译与真机验证。Android 14+ 是否允许全屏通知由系统权限/用途策略决定；若被系统限制，仍会显示通知横幅，而不是保证强制弹出 Activity。
