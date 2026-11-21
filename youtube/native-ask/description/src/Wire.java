package app.neszi.askdescription;
import java.io.*;import java.nio.charset.StandardCharsets;import java.util.*;
final class Wire {
    interface Edit { byte[] apply(byte[] bytes); }
    static byte[] map(byte[] data, int number, Edit edit) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(); boolean changed = false;
        for (Field f : fields(data)) {
            byte[] value = f.n == number && f.w == 2 ? edit.apply(f.v) : f.v;
            if (value != f.v) { write(out, field(f.n, value)); changed = true; }
            else write(out, f.raw);
        }
        return changed ? out.toByteArray() : data;
    }
    static byte[] first(byte[] data, int n) {
        if (data != null) for (Field f : fields(data)) if (f.n == n && f.w == 2) return f.v;
        return new byte[0];
    }
    static byte[] utf8(String s) { return s.getBytes(StandardCharsets.UTF_8); }
    static String text(byte[] b) { return new String(b, StandardCharsets.UTF_8); }
    static void write(ByteArrayOutputStream out, byte[] b) { out.write(b, 0, b.length); }
    static void varint(ByteArrayOutputStream out, long v) {
        while ((v & ~127L) != 0) { out.write((int)(v & 127) | 128); v >>>= 7; } out.write((int)v);
    }
    static byte[] field(int n, byte[] b) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(); varint(out, ((long)n << 3) | 2); varint(out, b.length); write(out,b); return out.toByteArray();
    }
    static final class Field {
        final int n,w; final byte[] v,raw;
        Field(int n,int w,byte[] v,byte[] raw) { this.n=n;this.w=w;this.v=v;this.raw=raw; }
    }
    static long read(byte[] b,int[] p) {
        long r=0; for(int s=0;s<64;s+=7) { if(p[0]>=b.length)throw new IllegalArgumentException("truncated varint");int v=b[p[0]++]&255;r|=(long)(v&127)<<s;if((v&128)==0)return r; }throw new IllegalArgumentException("overflow");
    }
    static List<Field> fields(byte[] b) {
        ArrayList<Field> result=new ArrayList<>();int[] p={0};
        while(p[0]<b.length) {
            int start=p[0]; long tag=read(b,p);int n=(int)(tag>>>3),w=(int)(tag&7);if(n==0)throw new IllegalArgumentException("zero field");
            int value=p[0];long len;
            switch(w) {
                case 0: read(b,p);break;
                case 1: p[0]+=8;break;
                case 2: len=read(b,p);value=p[0];if(len<0||len>b.length-value)throw new IllegalArgumentException("length");p[0]+=(int)len;break;
                case 5: p[0]+=4;break;
                default:throw new IllegalArgumentException("wire type");
            }
            if(p[0]>b.length)throw new IllegalArgumentException("truncated");
            result.add(new Field(n,w,Arrays.copyOfRange(b,value,p[0]),Arrays.copyOfRange(b,start,p[0])));
        }
        return result;
    }
}
