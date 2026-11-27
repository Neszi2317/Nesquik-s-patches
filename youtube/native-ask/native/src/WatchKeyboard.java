package app.neszi.asknative;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import java.lang.ref.WeakReference;

/** Gives the watch page the window space above the IME while the Ask panel is open. */
public final class WatchKeyboard implements ViewTreeObserver.OnPreDrawListener,
        View.OnAttachStateChangeListener {
    private static WeakReference<WatchKeyboard> active = new WeakReference<>(null);
    private final Activity activity;
    private final View content;
    private final int[] location = new int[2];
    private int originalHeight;
    private int appliedHeight = -1;

    private WatchKeyboard(Activity activity, View content) {
        this.activity = activity;
        this.content = content;
    }

    public static void stop() {
        WatchKeyboard previous = active.get();
        if (previous != null) previous.remove();
        active.clear();
    }

    static void open() {
        if (Build.VERSION.SDK_INT < 30) return;
        try {
            Activity activity = (Activity) Class.forName("app.morphe.extension.shared.Utils")
                    .getMethod("getActivity").invoke(null);
            if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
            View content = activity.findViewById(android.R.id.content);
            if (content == null || !content.isAttachedToWindow()) return;
            WatchKeyboard previous = active.get();
            if (previous != null && previous.content == content) return;
            stop();
            WatchKeyboard watcher = new WatchKeyboard(activity, content);
            active = new WeakReference<>(watcher);
            content.addOnAttachStateChangeListener(watcher);
            content.getViewTreeObserver().addOnPreDrawListener(watcher);
        } catch (ReflectiveOperationException | RuntimeException e) {
            Log.e("AskNative", "Could not attach watch keyboard handler", e);
        }
    }

    static int availableHeight(int windowHeight, int contentTop, int imeBottom) {
        return Math.max(0, windowHeight - imeBottom - contentTop);
    }

    @Override public boolean onPreDraw() {
        if (content.getResources().getConfiguration().orientation != Configuration.ORIENTATION_PORTRAIT) {
            remove();
            return true;
        }
        WindowInsets insets = content.getRootWindowInsets();
        if (insets == null || !insets.isVisible(WindowInsets.Type.ime())) {
            restore();
            return true;
        }
        int ime = insets.getInsets(WindowInsets.Type.ime()).bottom;
        if (ime <= insets.getInsets(WindowInsets.Type.navigationBars()).bottom) {
            restore();
            return true;
        }
        ViewGroup.LayoutParams params = content.getLayoutParams();
        if (params == null) return true;
        if (appliedHeight != -1 && params.height != appliedHeight) appliedHeight = -1;
        content.getLocationInWindow(location);
        int windowHeight = activity.getWindowManager().getCurrentWindowMetrics().getBounds().height();
        int target = availableHeight(windowHeight, location[1], ime);
        if (target == 0 || (appliedHeight == -1 && content.getHeight() <= target)) return true;
        if (appliedHeight == -1) originalHeight = params.height;
        if (originalHeight >= 0) target = Math.min(target, originalHeight);
        if (target != appliedHeight) {
            appliedHeight = target;
            params.height = target;
            content.setLayoutParams(params);
        }
        return true;
    }

    private void restore() {
        if (appliedHeight == -1) return;
        ViewGroup.LayoutParams params = content.getLayoutParams();
        if (params != null && params.height == appliedHeight) {
            params.height = originalHeight;
            content.setLayoutParams(params);
        }
        appliedHeight = -1;
    }

    private void remove() {
        content.removeOnAttachStateChangeListener(this);
        ViewTreeObserver tree = content.getViewTreeObserver();
        if (tree.isAlive()) tree.removeOnPreDrawListener(this);
        restore();
        if (active.get() == this) active.clear();
    }

    @Override public void onViewAttachedToWindow(View view) {}
    @Override public void onViewDetachedFromWindow(View view) { remove(); }
}
