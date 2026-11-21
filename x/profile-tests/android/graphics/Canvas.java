package android.graphics;
public class Canvas {public String shape;public float[] coords;public int color;public Canvas(){}public Canvas(Bitmap b){}public void drawCircle(float x,float y,float r,Paint p){shape="circle";coords=new float[]{x,y,r};color=p.color;}public void drawRect(float l,float t,float r,float b,Paint p){shape="rect";coords=new float[]{l,t,r,b};color=p.color;}}
