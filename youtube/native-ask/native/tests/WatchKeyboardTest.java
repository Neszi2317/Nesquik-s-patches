package app.neszi.asknative;

public final class WatchKeyboardTest {
    public static void main(String[] args) {
        // Keyboard begins at y=940 in the reported portrait screenshot.
        check(940, 1536, 0, 596);
        check(872, 1536, 68, 596);
        check(720, 1100, 80, 300);
        check(0, 500, 200, 400);
        System.out.println("PASS available height including window offsets and small windows");
    }

    private static void check(int expected, int height, int top, int ime) {
        int actual = WatchKeyboard.availableHeight(height, top, ime);
        if (expected != actual) throw new AssertionError("Expected " + expected + ", got " + actual);
    }
}
