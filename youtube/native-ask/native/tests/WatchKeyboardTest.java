package app.neszi.asknative;

public final class WatchKeyboardTest {
    private static void check(int expected, int panelBottom, int ime, int nav, int base) {
        int actual = WatchKeyboard.extraPadding(1500, panelBottom, ime, nav, base);
        if (expected != actual) throw new AssertionError("Expected " + expected + ", got " + actual);
    }

    public static void main(String[] args) {
        check(550, 1500, 600, 50, 50);  // Edge-to-edge window, gesture navigation.
        check(300, 1200, 600, 50, 0);   // Partially resized host.
        check(0, 900, 600, 50, 0);      // Already above the IME.
        check(0, 1500, 0, 50, 50);      // Keyboard dismissed or floating.
        check(500, 1500, 600, 50, 100); // Existing content padding.
        System.out.println("PASS watch keyboard overlap for full, partial and pre-resized panels");
    }
}
