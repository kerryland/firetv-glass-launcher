package com.ivantrj.glasslauncher;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;

/**
 * Pauses the Home-button redirect so the user can stay on Amazon's own UI after
 * choosing Exit. The suspension is tied to the current boot (Settings.Global
 * BOOT_COUNT), so a reboot always brings Glass back as the launcher; within a
 * boot it lasts until Glass is shown again.
 */
final class HomeRedirect {

    private static final String PREFS = "focus";
    private static final String SUSPENDED = "redirectSuspended";
    private static final String SUSPENDED_BOOT = "redirectSuspendedBoot";

    static void suspend(Context context) {
        SharedPreferences p = prefs(context);
        p.edit().putBoolean(SUSPENDED, true).putInt(SUSPENDED_BOOT, bootCount(context)).apply();
    }

    static void resume(Context context) {
        SharedPreferences p = prefs(context);
        if (p.getBoolean(SUSPENDED, false)) p.edit().putBoolean(SUSPENDED, false).apply();
    }

    static boolean suspended(Context context) {
        SharedPreferences p = prefs(context);
        return p.getBoolean(SUSPENDED, false) && p.getInt(SUSPENDED_BOOT, -1) == bootCount(context);
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static int bootCount(Context context) {
        try {
            return Settings.Global.getInt(context.getContentResolver(), Settings.Global.BOOT_COUNT);
        } catch (Exception e) {
            return -1;
        }
    }

    private HomeRedirect() {}
}