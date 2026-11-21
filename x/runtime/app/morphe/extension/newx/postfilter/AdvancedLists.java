package app.morphe.extension.newx.postfilter;
import java.io.*;import java.nio.charset.StandardCharsets;import java.util.*;import org.json.*;
public final class AdvancedLists {
    public static String read(InputStream in)throws IOException {if(in==null)throw new IOException("Fichier inaccessible");ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;
        while((n=in.read(b))!=-1){if(out.size()+n>2097152)throw new IOException("Fichier supérieur à 2 Mo");out.write(b,0,n);}return new String(out.toByteArray(),StandardCharsets.UTF_8).replace("\ufeff","");}
    public static List<String> parse(String text,String format)throws JSONException {
        if(text.length()>2097152)throw new IllegalArgumentException("Fichier trop grand");List<String> out=new ArrayList<>();
        if(format.equals("json")||text.trim().startsWith("[")||text.trim().startsWith("{")){
            Object v=new JSONTokener(text).nextValue();JSONArray a=v instanceof JSONArray?(JSONArray)v:((JSONObject)v).getJSONArray("keywords");
            for(int i=0;i<a.length();i++){Object x=a.get(i);if(!(x instanceof String))throw new IllegalArgumentException("JSON : uniquement des chaînes");out.add((String)x);}return out;}
        if(format.equals("csv")){StringBuilder cell=new StringBuilder();boolean quote=false;
            for(int i=0;i<text.length();i++){char c=text.charAt(i);if(c=='"'){if(quote&&i+1<text.length()&&text.charAt(i+1)=='"'){cell.append('"');i++;}else quote=!quote;}
                else if(!quote&&(c==','||c==';'||c=='\n'||c=='\r')){out.add(cell.toString());cell.setLength(0);}else cell.append(c);}
            if(quote)throw new IllegalArgumentException("CSV : guillemet non fermé");out.add(cell.toString());return out;}
        return Arrays.asList(text.split("\\R",-1));
    }
}
