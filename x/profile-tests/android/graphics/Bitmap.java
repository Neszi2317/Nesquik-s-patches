package android.graphics;
public class Bitmap {public enum Config{ARGB_8888}public static int allocations;public final int width,height;public int color;private Bitmap(int w,int h){width=w;height=h;allocations++;}public static Bitmap createBitmap(int w,int h,Config c){return new Bitmap(w,h);}public void eraseColor(int c){color=c;}}
