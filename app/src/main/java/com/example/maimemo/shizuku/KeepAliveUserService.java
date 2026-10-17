package com.example.maimemo.shizuku;

import android.os.RemoteException;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/** Runs a narrowly-scoped device-idle whitelist command with Shizuku's shell identity. */
public final class KeepAliveUserService extends IKeepAliveService.Stub {
    public KeepAliveUserService() { }

    @Override
    public String addDeviceIdleWhitelist(String packageName) throws RemoteException {
        if (packageName == null || !packageName.matches("[A-Za-z0-9_.]+")) {
            return "包名格式无效，未执行命令。";
        }
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(new String[]{"sh", "-c", "cmd deviceidle whitelist +" + packageName});
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                 BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) output.append(line).append('\n');
                while ((line = errorReader.readLine()) != null) output.append(line).append('\n');
            }
            int exitCode = process.waitFor();
            if (exitCode == 0) return "已向系统请求加入设备空闲白名单。\n" + output.toString().trim();
            return "系统未接受白名单命令（退出码 " + exitCode + "）。\n" + output.toString().trim();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "执行被中断。";
        } catch (Throwable e) {
            return "执行失败：" + e.getClass().getSimpleName() + (e.getMessage() == null ? "" : "：" + e.getMessage());
        } finally {
            if (process != null) process.destroy();
        }
    }
}
