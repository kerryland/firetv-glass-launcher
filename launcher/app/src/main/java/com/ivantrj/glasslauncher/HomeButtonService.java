package com.ivantrj.glasslauncher;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;

/**
 * Fire OS protects its launcher, so it can't be disabled and stays the system
 * home. This service makes Glass the effective home screen instead:
 *  - the remote's Home button is consumed and opens Glass;
 *  - whenever the Amazon launcher comes to the front anyway (after boot, or when
 *    an app exits), Glass is brought straight back on top;
 *  - a black overlay hides the Amazon launcher's flash until Glass is ready.
 * Event-driven only: it receives window changes from the Amazon launcher's
 * package and key events, and does nothing in between.
 */
public class HomeButtonService extends AccessibilityService {

    private static final String AMAZON_LAUNCHER = "com.amazon.tv.launcher";
    private static final long SHADE_SAFETY_MS = 6000; // never leave the screen blacked out forever

    private final Handler main = new Handler(Looper.getMainLooper());
    private long lastLaunch;

    private WindowManager windowManager;
    private View shade;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        if (event.getKeyCode() != KeyEvent.KEYCODE_HOME) return false;
        if (event.getAction() == KeyEvent.ACTION_UP && !event.isCanceled()) openGlass();
        return true; // swallow both down and up so Fire OS never sees Home
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;
        if (AMAZON_LAUNCHER.contentEquals(event.getPackageName())) {
            showShade();
            openGlass();
        } else if (getPackageName().contentEquals(event.getPackageName())) {
            // Our own launcher is now the foreground window: the flash is over
            removeShade();
        }
    }

    private void showShade() {
        if (shade != null || windowManager == null) return;
        View v = new View(this);
        v.setBackgroundColor(Color.BLACK);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.OPAQUE);
        lp.gravity = Gravity.TOP | Gravity.START;
        try {
            windowManager.addView(v, lp);
            shade = v;
            main.removeCallbacks(shadeTimeout);
            main.postDelayed(shadeTimeout, SHADE_SAFETY_MS);
        } catch (Exception ignored) {
        }
    }

    private final Runnable shadeTimeout = this::removeShade;

    private void removeShade() {
        main.removeCallbacks(shadeTimeout);
        if (shade == null) return;
        try {
            windowManager.removeView(shade);
        } catch (Exception ignored) {
        }
        shade = null;
    }

    private void openGlass() {
        long now = SystemClock.uptimeMillis();
        if (now - lastLaunch < 600) return; // one launch per Home press / launcher flash
        lastLaunch = now;
        startActivity(new Intent(this, HomeActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
    }

    @Override public void onInterrupt() {}

    @Override public void onDestroy() {
        removeShade();
        super.onDestroy();
    }
}