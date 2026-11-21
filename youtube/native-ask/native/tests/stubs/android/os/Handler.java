package android.os;
public class Handler {public Handler(Looper l){} public boolean postDelayed(Runnable r,long d){r.run();return true;} public boolean post(Runnable r){r.run();return true;}}
