package fr.nesquik.hidden;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class Ids {
    public static final String HEADER = "nesquik-hidden-posts-v1";
    public static final int MAX = 100000;
    public static boolean valid(String s) {
        if (s == null || s.isEmpty() || s.length() > 19 || s.charAt(0) == '0') return false;
        for (int i=0; i<s.length(); i++) if (s.charAt(i)<'0' || s.charAt(i)>'9') return false;
        return s.length()<19 || s.compareTo("9223372036854775807")<=0;
    }
    public static Set<String> read(InputStream input) throws IOException {
        if (input == null) throw new IOException("Fichier inaccessible");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        for (int n; (n=input.read(buf))!=-1;) {
            if (bytes.size()+n>3*1024*1024) throw new IOException("Fichier trop grand");
            bytes.write(buf,0,n);
        }
        String[] lines = bytes.toString("UTF-8").split("\\r?\\n",-1);
        if (!lines[0].equals(HEADER)) throw new IOException("Format inconnu");
        Set<String> result = new HashSet<>();
        for (int i=1;i<lines.length;i++) {
            if(lines[i].isEmpty()) continue;
            if(!valid(lines[i])) throw new IOException("Identifiant invalide, ligne "+(i+1));
            result.add(lines[i]);
            if(result.size()>MAX) throw new IOException("Trop de posts");
        }
        return result;
    }
    public static void write(Set<String> ids,OutputStream out) throws IOException {
        if(out==null) throw new IOException("Fichier inaccessible");
        Writer writer=new OutputStreamWriter(out,StandardCharsets.UTF_8);
        writer.write(HEADER+"\n");
        for(String id:new TreeSet<>(ids)) writer.write(id+"\n");
        writer.flush();
    }
}
