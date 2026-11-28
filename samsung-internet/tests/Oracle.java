// SPDX-License-Identifier: MPL-2.0
import java.io.*;
import java.lang.reflect.*;
import java.util.*;

class Oracle {
    static Method callback;
    static boolean stock;
    static int stockCalls;

    static void check(String url, boolean expect) throws Exception {
        boolean actual = (boolean) callback.invoke(null, url);
        if (expect != actual) throw new AssertionError(url + " -> " + actual);
    }

    public static void main(String[] args) throws Exception {
        stock = Boolean.parseBoolean(args[0]);
        String terrace = "com.sec.terrace.browser.net.TerraceUrlFilter";
        Class<?> filter = Class.forName(terrace);
        Class<?> delegate = Class.forName(terrace + "$TerraceUrlFilterDelegate$Delegate");
        Class<?> factory = Class.forName(terrace + "$TerraceUrlFilterDelegate$DelegateFactory");
        ClassLoader loader = Oracle.class.getClassLoader();
        Object stub = Proxy.newProxyInstance(loader, new Class[]{delegate}, (p, m, a) -> {
            if (m.getName().equals("isUrlFilterSupported")) return stock;
            stockCalls++;
            return "https://stock.test/".equals(a[0]);
        });
        Object provider = Proxy.newProxyInstance(loader, new Class[]{factory}, (p,m,a) -> stub);
        filter.getMethod("setDelegateFactory", factory).invoke(null, provider);
        Class<?> bridge = Class.forName("com.sec.terrace.browser.browsing_data.TinUrlFilterBridge");
        Object instance = bridge.getMethod("create", long.class).invoke(null, 0L);
        boolean enabled = (boolean)bridge.getMethod("isUrlFilterSupported").invoke(instance);
        callback = filter.getDeclaredMethod("isBlockedUrl", String.class);
        callback.setAccessible(true);
        if (args[1].equals("empty")) {
            if (enabled != stock) throw new AssertionError("Empty list changed gate");
            check("https://example.org/", false);
            return;
        }
        if (!enabled) throw new AssertionError("Native gate disabled");
        check("https://stock.test/", stock);
        if (stockCalls != (stock ? 1 : 0)) throw new AssertionError("Stock decision");
        int calls = stockCalls;
        check("https://example.org/", true);
        if (calls != stockCalls) throw new AssertionError("Unexpected stock check");
        if (args[1].equals("stream")) {
            var input = new BufferedReader(new InputStreamReader(System.in));
            for (String line; (line = input.readLine()) != null;)
                System.out.println(callback.invoke(null, line));
            return;
        }
        String[] deny = {"https://a.b.EXAMPLE.org.:443/x", "http://user:pw@example.org/",
            "http://example.net/else", "https://old.reddit.com/R/EXAMPLE/comments/1",
            "https://reddit.com/r/second", "https://example.com/BLOCKED?x", "https://example.com/blocked/a",
            "https://blocked.test/", "http://foo.blocked.test/a", "http://[::1]:8080/x",
            "https://127.0.0.1/", "https://query.test/path?x=1&b=2"};
        String[] allow = {"https://example.org.allowed.test/", "https://notexample.org/",
            "https://example.org@allowed.test/", "https://example.com/blocked-other",
            "https://www.reddit.com/r/example_other", "https://query.test/path?x=12",
            "https://allowed.test/?x=example.org", "about:blank", "file:///example.org", null};
        for (String u : deny) check(u, true);
        for (String u : allow) check(u, false);
        System.out.println("Pass: native gate, stock delegation and " + (deny.length + allow.length) + " URL cases.");
    }
}
