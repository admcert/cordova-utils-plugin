package com.zsoftware;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AppOpsManager;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import android.os.Build;

import static android.content.Context.POWER_SERVICE;

/**
 * 工具类 插件
 */
public class UtilsPlugin extends CordovaPlugin {
    private static final String LOG_TAG = "UtilsPlugin";

    private static String EXITAPP_ACTION = "exitApp";
    private static String ISEMULATOR_ACTION = "isEmulator";
    private static String ISROOT_ACTION = "isDeviceRooted";
    private static String ISXPOSED_ACTION = "isXposed";

    private static String BATTERYOPTIMIZATION_ACTION = "batteryOptimization";

    private static String ISNOTIFICATION_ENABLED = "isNotificationEnabled"; //是否开启推送通知
    private static String GOTO_APPLICATION_SETTING = "goApplicationSetting";//跳转应用设置界面


    private static String CHECK_OP_NO_THROW = "checkOpNoThrow";
    private static String OP_POST_NOTIFICATION = "OP_POST_NOTIFICATION";

    /**
     * Constructor.
     */
    public UtilsPlugin() {
    }


    /**
     * Executes the request and returns PluginResult.
     *
     * @param action          The action to execute.
     * @param args            JSONArray of arguments for the plugin.
     * @param callbackContext The callback context used when calling back into JavaScript.
     * @return True when the action was valid, false otherwise.
     */
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
        if (this.cordova.getActivity().isFinishing()) return true;

        if (action.equalsIgnoreCase(GOTO_APPLICATION_SETTING)) { //跳转应用设置界面
            cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    gotoApplicationSetting();
                    callbackContext.success();
                }
            });
            return true;
        }


        if (action.equalsIgnoreCase(ISNOTIFICATION_ENABLED)) { //是否开启推送权限
            if (Build.VERSION.SDK_INT >= 19) {
                final Context context = this.cordova.getActivity();
                cordova.getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Boolean retVal = isNotificationEnabled(context);
                        Log.i(LOG_TAG, "retVal:" + retVal);
                        callbackContext.success(String.valueOf(retVal).toLowerCase());
                    }
                });
            }
            return true;
        }

        if (action.equalsIgnoreCase(EXITAPP_ACTION)) {
            this.cordova.getActivity().finish();
            System.exit(0);
            return true;
        }
        if (action.equalsIgnoreCase(ISEMULATOR_ACTION)) {

            cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    boolean emulator = isEmulator();
                    callbackContext.success(Boolean.toString(emulator).toLowerCase());
                }
            });
            return true;
        }
        if (action.equalsIgnoreCase(ISROOT_ACTION)) {
            boolean isRooted = isDeviceRooted();
            callbackContext.success(Boolean.toString(isRooted).toLowerCase());
            return true;
        }

        if (action.equalsIgnoreCase(ISXPOSED_ACTION)) {

            cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    boolean isRooted = isHookByPackageName(cordova.getActivity().getApplicationContext());
                    callbackContext.success(Boolean.toString(isRooted).toLowerCase());
                }
            });

            return true;
        }

        if (action.equalsIgnoreCase(BATTERYOPTIMIZATION_ACTION)) {
            if (Build.VERSION.SDK_INT >= 23) {
                cordova.getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ignoreBatteryOptimization();
                        callbackContext.success();
                    }
                });
            }
            return true;
        }

        // Only alert and confirm are async.
        callbackContext.success();
        return true;
    }


    /**
     * Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
     * 19及以上
     *
     * @param context
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static boolean isEnableV19(Context context) {
        AppOpsManager mAppOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        ApplicationInfo appInfo = context.getApplicationInfo();
        String pkg = context.getApplicationContext().getPackageName();
        int uid = appInfo.uid;
        Class appOpsClass;
        try {
            appOpsClass = Class.forName(AppOpsManager.class.getName());
            Method checkOpNoThrowMethod = appOpsClass.getMethod(CHECK_OP_NO_THROW, Integer.TYPE, Integer.TYPE,
                    String.class);
            Field opPostNotificationValue = appOpsClass.getDeclaredField(OP_POST_NOTIFICATION);

            int value = (Integer) opPostNotificationValue.get(Integer.class);
            return ((Integer) checkOpNoThrowMethod.invoke(mAppOps, value, uid, pkg) == AppOpsManager.MODE_ALLOWED);

        } catch (ClassNotFoundException e) {
            Log.e(LOG_TAG, "", e);
        } catch (NoSuchMethodException e) {
            Log.e(LOG_TAG, "", e);
        } catch (NoSuchFieldException e) {
            Log.e(LOG_TAG, "", e);
        } catch (InvocationTargetException e) {
            Log.e(LOG_TAG, "", e);
        } catch (IllegalAccessException e) {
            Log.e(LOG_TAG, "", e);
        }
        return false;
    }

    public static Boolean isEnableV26(Context context) {
        try {
            NotificationManager notificationManager = (NotificationManager)
                    context.getSystemService(Context.NOTIFICATION_SERVICE);
            Method sServiceField = notificationManager.getClass().getDeclaredMethod("getService");
            sServiceField.setAccessible(true);
            Object sService = sServiceField.invoke(notificationManager);

            ApplicationInfo appInfo = context.getApplicationInfo();
            String pkg = context.getApplicationContext().getPackageName();
            int uid = appInfo.uid;

            Method method = sService.getClass().getDeclaredMethod("areNotificationsEnabledForPackage"
                    , String.class, Integer.TYPE);
            method.setAccessible(true);
            return (Boolean) method.invoke(sService, pkg, uid);
        } catch (Exception e) {
            Log.e(LOG_TAG, "", e);
        }
        return false;
    }


    /**
     * 跳转应用设置界面
     */
    private void gotoApplicationSetting() {
        Intent intent = new Intent();
        final Context context = this.cordova.getActivity();
        if (Build.VERSION.SDK_INT >= 26) {
            // android 8.0引导
            intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
            intent.putExtra("android.provider.extra.APP_PACKAGE", context.getPackageName());
        } else if (Build.VERSION.SDK_INT >= 21) {
            // android 5.0-7.0
            intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
            intent.putExtra("app_package", context.getPackageName());
            intent.putExtra("app_uid", context.getApplicationInfo().uid);
        } else {
            // 其他
            intent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
            intent.setData(Uri.fromParts("package", context.getPackageName(), null));
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }


    public static boolean isNotificationEnabled(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return isEnableV26(context);
        } else {
            return isEnableV19(context);
        }
    }


    @TargetApi(Build.VERSION_CODES.M)
    public void ignoreBatteryOptimization() {
        Activity activity = this.cordova.getActivity();
        PowerManager powerManager = (PowerManager) activity.getSystemService(POWER_SERVICE);
        boolean hasIgnored = powerManager.isIgnoringBatteryOptimizations(activity.getPackageName());
        //  判断当前APP是否有加入电池优化的白名单，如果没有，弹出加入电池优化的白名单的设置对话框。
        if (!hasIgnored) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + activity.getPackageName()));

            try {
                activity.startActivity(intent);
            } catch (ActivityNotFoundException e) {
                e.printStackTrace();
            }
        }

    }


    public static boolean isHookByPackageName(Context context) {
        boolean isHook = false;
        PackageManager packageManager = context.getPackageManager();
        List<ApplicationInfo> applicationInfoList = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
        if (null == applicationInfoList) {
            return isHook;
        }
        for (ApplicationInfo applicationInfo : applicationInfoList) {
            if (applicationInfo.packageName.equals("de.robv.android.xposed.installer")) {
                Log.wtf("HookDetection", "Xposed found on the system.");
                isHook = true;
            }
            if (applicationInfo.packageName.equals("com.saurik.substrate")) {
                isHook = true;
                Log.wtf("HookDetection", "Substrate found on the system.");
            }
        }
        return isHook;
    }

    private static String getSystemProperty(String name) throws Exception {
        Class systemPropertyClazz = Class.forName("android.os.SystemProperties");
        return (String) systemPropertyClazz.getMethod("get", new Class[]{String.class})
                .invoke(systemPropertyClazz, new Object[]{name});
    }

    private static boolean isEmulator() {
        try {
            boolean goldfish = getSystemProperty("ro.hardware").contains("goldfish");
            boolean emu = getSystemProperty("ro.kernel.qemu").length() > 0;
            boolean sdk = getSystemProperty("ro.product.model").equals("sdk");
            if (emu || goldfish || sdk) {
                return true;
            }
        } catch (Exception e) {
        }
        return false;
    }

    public static boolean isDeviceRooted() {
//        if (checkDeviceDebuggable()){return true;}//check buildTags
        if (checkSuperuserApk()) {
            return true;
        }//Superuser.apk
        //if (checkRootPathSU()){return true;}//find su in some path
        //if (checkRootWhichSU()){return true;}//find su use 'which'
        if (checkBusybox()) {
            return true;
        }//find su use 'which'
        if (checkAccessRootData()) {
            return true;
        }//find su use 'which'
//        if (checkGetRootAuth()){return true;}//exec su

        return false;
    }


    private static boolean checkSuperuserApk() {
        try {
            File file = new File("/system/app/Superuser.apk");
            if (file.exists()) {
                Log.i(LOG_TAG, "/system/app/Superuser.apk exist");
                return true;
            }
        } catch (Exception e) {
        }
        return false;
    }

    private static boolean checkRootPathSU() {
        File f = null;
        final String kSuSearchPaths[] = {"/system/bin/", "/system/xbin/", "/system/sbin/", "/sbin/", "/vendor/bin/"};
        try {
            for (int i = 0; i < kSuSearchPaths.length; i++) {
                f = new File(kSuSearchPaths[i] + "su");
                if (f != null && f.exists()) {
                    Log.i(LOG_TAG, "find su in : " + kSuSearchPaths[i]);
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }


    private static boolean checkRootWhichSU() {
        String[] strCmd = new String[]{"/system/xbin/which", "su"};
        ArrayList<String> execResult = executeCommand(strCmd);
        if (execResult != null) {
            Log.i(LOG_TAG, "execResult=" + execResult.toString());
            return true;
        } else {
            Log.i(LOG_TAG, "execResult=null");
            return false;
        }
    }

    private static ArrayList<String> executeCommand(String[] shellCmd) {
        String line = null;
        ArrayList<String> fullResponse = new ArrayList<String>();
        Process localProcess = null;
        try {
            Log.i(LOG_TAG, "to shell exec which for find su :");
            localProcess = Runtime.getRuntime().exec(shellCmd);
        } catch (Exception e) {
            return null;
        }
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(localProcess.getOutputStream()));
        BufferedReader in = new BufferedReader(new InputStreamReader(localProcess.getInputStream()));
        try {
            while ((line = in.readLine()) != null) {
                Log.i(LOG_TAG, "–> Line received: " + line);
                fullResponse.add(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.i(LOG_TAG, "–> Full response was: " + fullResponse);
        return fullResponse;
    }

    public static synchronized boolean checkGetRootAuth() {
        Process process = null;
        DataOutputStream os = null;
        try {
            Log.i(LOG_TAG, "to exec su");
            process = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(process.getOutputStream());
            os.writeBytes("exit\n");
            os.flush();
            int exitValue = process.waitFor();
            Log.i(LOG_TAG, "exitValue=" + exitValue);
            if (exitValue == 0) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            Log.i(LOG_TAG, "Unexpected error - Here is what I know: "
                    + e.getMessage());
            return false;
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
                process.destroy();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static synchronized boolean checkBusybox() {
        try {
            Log.i(LOG_TAG, "to exec busybox df");
            String[] strCmd = new String[]{"busybox", "df"};
            ArrayList<String> execResult = executeCommand(strCmd);
            if (execResult != null) {
                Log.i(LOG_TAG, "execResult=" + execResult.toString());
                return true;
            } else {
                Log.i(LOG_TAG, "execResult=null");
                return false;
            }
        } catch (Exception e) {
            Log.i(LOG_TAG, "Unexpected error - Here is what I know: "
                    + e.getMessage());
            return false;
        }
    }


    public static synchronized boolean checkAccessRootData() {
        try {
            Log.i(LOG_TAG, "to write /data");
            String fileContent = "test_ok";
            Boolean writeFlag = writeFile("/data/su_test", fileContent);
            if (writeFlag) {
                Log.i(LOG_TAG, "write ok");
            } else {
                Log.i(LOG_TAG, "write failed");
            }

            Log.i(LOG_TAG, "to read /data");
            String strRead = readFile("/data/su_test");
            Log.i(LOG_TAG, "strRead=" + strRead);
            if (fileContent.equals(strRead)) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            Log.i(LOG_TAG, "Unexpected error - Here is what I know: "
                    + e.getMessage());
            return false;
        }
    }

    //写文件
    public static Boolean writeFile(String fileName, String message) {
        try {
            FileOutputStream fout = new FileOutputStream(fileName);
            byte[] bytes = message.getBytes();
            fout.write(bytes);
            fout.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    //读文件
    public static String readFile(String fileName) {
        File file = new File(fileName);
        try {
            FileInputStream fis = new FileInputStream(file);
            byte[] bytes = new byte[1024];
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            int len;
            while ((len = fis.read(bytes)) > 0) {
                bos.write(bytes, 0, len);
            }
            String result = new String(bos.toByteArray());
            Log.i(LOG_TAG, result);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
