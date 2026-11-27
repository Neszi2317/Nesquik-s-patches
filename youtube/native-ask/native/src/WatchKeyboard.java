package app.neszi.asknative;

import android.app.Activity;
import android.os.Build;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.util.Log;
import java.lang.ref.WeakReference;

/** Keyboard handling for the regular watch-page Ask panel on Android 11+. */
public final class WatchKeyboard implements ViewTreeObserver.OnGlobalLayoutListener,
        View.OnAttachStateChangeListener {
    private static WeakReference<WatchKeyboard> active = new WeakReference<>(null);
    private final Activity activity;
    private final View panel;
    private final int[] location = new int[2];
    private int applied;
    private static int generation;

    private WatchKeyboard(Activity activity, View panel) {
        this.activity = activity;
        this.panel = panel;
    }

    public static void stop() {
        generation++;
        WatchKeyboard previous = active.get();
        if (previous != null) previous.remove();
        active.clear();
    }

    static void open() {
        if (Build.VERSION.SDK_INT < 30) return;
        try {
            Activity activity = (Activity) Class.forName("app.morphe.extension.shared.Utils")
                    .getMethod("getActivity").invoke(null);
            if (activity == null) return;
            int id = activity.getResources().getIdentifier("engagement_panel_wrapper", "id",
                    activity.getPackageName());
            if (id == 0) id = activity.getResources().getIdentifier("engagement_panel_wrapper",
                    "id", "com.google.android.youtube");
            if (id != 0) bindWhenShown(activity, id, generation, 30);
        } catch (ReflectiveOperationException | RuntimeException e) {
            Log.e("AskNative", "Watch keyboard setup failed", e);
        }
    }

    private static void bindWhenShown(Activity activity, int id, int session, int attempts) {
        if (session != generation || activity.isDestroyed() || activity.isFinishing()) return;
        View panel = activity.findViewById(id);
        if (panel == null || !panel.isAttachedToWindow() || !panel.isShown()) {
            if (attempts > 0) activity.getWindow().getDecorView().postDelayed(
                    () -> bindWhenShown(activity, id, session, attempts - 1), 50);
            return;
        }
        WatchKeyboard watcher = new WatchKeyboard(activity, panel);
        active = new WeakReference<>(watcher);
        panel.addOnAttachStateChangeListener(watcher);
        panel.getViewTreeObserver().addOnGlobalLayoutListener(watcher);
        watcher.onGlobalLayout();
    }

    static int extraPadding(int windowHeight, int panelBottom, int ime, int nav, int base) {
        // A resized watch page may already end above all or part of the IME.
        return Math.max(0, Math.min(ime - nav, panelBottom - base - (windowHeight - ime)));
    }

    @Override public void onGlobalLayout() {
        if (!panel.isShown()) {
            remove();
            return;
        }
        WindowInsets insets = panel.getRootWindowInsets();
        View focus = panel.findFocus();
        int extra = 0;
        if (focus != null && focus.onCheckIsTextEditor() && insets != null
                && insets.isVisible(WindowInsets.Type.ime())) {
            panel.getLocationInWindow(location);
            int height = activity.getWindowManager().getCurrentWindowMetrics().getBounds().height();
            extra = extraPadding(height, location[1] + panel.getHeight(),
                    insets.getInsets(WindowInsets.Type.ime()).bottom,
                    insets.getInsets(WindowInsets.Type.navigationBars()).bottom,
                    Math.max(0, panel.getPaddingBottom() - applied));
        }
        apply(extra);
    }

    private void apply(int extra) {
        if (extra == applied) return;
        int base = Math.max(0, panel.getPaddingBottom() - applied);
        applied = extra;
        panel.setPadding(panel.getPaddingLeft(), panel.getPaddingTop(), panel.getPaddingRight(), base + extra);
    }

    private void remove() {
        panel.removeOnAttachStateChangeListener(this);
        ViewTreeObserver observer = panel.getViewTreeObserver();
        if (observer.isAlive()) observer.removeOnGlobalLayoutListener(this);
        apply(0);
    }

    @Override public void onViewAttachedToWindow(View v) {}
    @Override public void onViewDetachedFromWindow(View v) { remove(); }
}
