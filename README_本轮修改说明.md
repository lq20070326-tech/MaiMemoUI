# 必须背 App 本轮修改说明

## 本轮内容
- 主要页面及独立 Activity 开启系统窗口适配，减少状态栏遮挡；收紧我的页顶部留白。
- 背诵评分按钮改为“没有印象 / 记忆模糊 / 暂时记住 / 已经记牢”，依次映射 FSRS 1/2/3/4（Again/Hard/Good/Easy）。背诵卡片、评分按钮间距和配色也做了调整。
- FSRS 数据与参数页返回控件仅保留箭头。
- 移除我的页会员横幅。
- 应用名称和主要可见品牌字样调整为“必须背”。
- “学习记录”接入本地学习日志，展示总次数、复习次数、回忆成功率、按天记录及保守估算学习时长。时长按相邻日志时间差估算，每段最多计 180 秒，避免长时间离开应用造成虚高。
- “必须背商店”加入“自由操控时间”券，按估算背诵时长兑换，兑换记录保存在本机。此功能为本地虚拟兑换，不涉及真实支付，也不自动控制系统时间。

## 验证
- XML 文件已用 XML 解析器检查。
- 未能完成 Gradle 编译：环境无法连接 services.gradle.org 下载 Gradle 9.4.1（UnknownHostException）。尚未完成手机实机测试。


## 本轮修改：随机新词与马赛克半透明背景
- 新词推送：在当前自动学习选定的单词书内，从尚未建立学习记录的单词中随机抽取；FSRS 已到期复习词仍优先。
- 全屏学习卡片和悬浮窗：改为程序绘制的柔和彩色马赛克磨砂玻璃背景，保留半透明效果和圆角。
- 系统通知横幅：仍使用 Android 原生 MessagingStyle。横幅背景由系统界面绘制，应用不能强制将系统横幅背景改成马赛克；马赛克效果仅作用于应用自己绘制的全屏卡片和悬浮窗。


## 本轮修改：修复 Shizuku 授权申请未出现
- 增加官方 Shizuku Provider 依赖 `dev.rikka.shizuku:provider:13.1.5`。
- 在 AndroidManifest.xml 的 application 中注册 `rikka.shizuku.ShizukuProvider`，authority 使用 `${applicationId}.shizuku`，并配置官方文档要求的导出和保护权限。
- 保留原有 `Shizuku.requestPermission()` 与结果监听流程；Shizuku 服务运行且 binder 可用时，点击设置项应向 Shizuku 发起授权请求。
- 仍需在 Android Studio 联网同步依赖、重新编译并真机测试；当前环境未完成 Gradle 编译。
