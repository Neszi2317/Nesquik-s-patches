package app.nesquikos.playlist;
import android.app.*;import android.content.*;import android.graphics.*;import android.graphics.drawable.*;import android.net.Uri;import android.os.*;import android.text.*;import android.view.*;import android.widget.*;
import java.lang.ref.WeakReference;import java.io.IOException;import java.util.*;
/** Independent dialog and fragment-local magnifier. Global search is untouched. */
public final class PlaylistSearch {
 static final Handler MAIN=new Handler(Looper.getMainLooper());
 static WeakReference<Object> creating=new WeakReference<Object>(null);
 static final WeakHashMap<Object,WeakReference<ImageButton>> buttons=new WeakHashMap<Object,WeakReference<ImageButton>>();
 static final WeakHashMap<View,Boolean> roots=new WeakHashMap<View,Boolean>();
 public static void onFragment(Object f){creating=new WeakReference<Object>(f);}
 public static void changed(final Object f){MAIN.post(new Runnable(){public void run(){WeakReference<ImageButton> r=buttons.get(f);ImageButton b=r==null?null:r.get();if(b!=null)b.setVisibility(id(f).isEmpty()?View.GONE:View.VISIBLE);}});}
 static String id(Object f){try{String s=f==null?null:NativeBridge.browseId(f);return s!=null&&s.startsWith("VL")&&s.length()>2?s.substring(2):"";}catch(Exception e){return "";}}
 public static void onRoot(final View root){try{
  Object f=creating.get();creating.clear();if(f==null||root==null||roots.containsKey(root))return;FrameLayout host=findFrame(root);if(host==null)return;roots.put(root,true);
  final WeakReference<Object> ref=new WeakReference<Object>(f);ImageButton b=new ImageButton(root.getContext());b.setImageDrawable(new SearchIcon());b.setContentDescription("Rechercher dans cette playlist");GradientDrawable bg=new GradientDrawable();bg.setColor(0xff2264b5);bg.setShape(GradientDrawable.OVAL);b.setBackground(bg);
  FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(dp(root,44),dp(root,44),Gravity.TOP|Gravity.RIGHT);lp.setMargins(dp(root,8),dp(root,8),dp(root,12),0);host.addView(b,lp);buttons.put(f,new WeakReference<ImageButton>(b));b.setVisibility(id(f).isEmpty()?View.GONE:View.VISIBLE);
  b.setOnClickListener(new View.OnClickListener(){public void onClick(View v){String scope=id(ref.get());Activity a=activity(root.getContext());if(!scope.isEmpty()&&a!=null&&!a.isFinishing())new Controller(a,scope,root,ref).show();}});
 }catch(Exception ignored){}}
 static FrameLayout findFrame(View v){
  if(v instanceof FrameLayout)return (FrameLayout)v;
  if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){FrameLayout f=findFrame(g.getChildAt(i));if(f!=null)return f;}}
  return null;
 }
 static int dp(View v,int n){return(int)(v.getResources().getDisplayMetrics().density*n+.5f);}
 static Activity activity(Context c){for(int i=0;i<12;i++){if(c instanceof Activity)return(Activity)c;if(!(c instanceof ContextWrapper))break;Context n=((ContextWrapper)c).getBaseContext();if(n==c)break;c=n;}return null;}
 static final class SearchIcon extends Drawable {
  final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
  public void draw(Canvas c){Rect b=getBounds();float s=Math.min(b.width(),b.height()),x=b.centerX()-s*.08f,y=b.centerY()-s*.08f,r=s*.19f;p.setColor(Color.WHITE);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(s*.06f);c.drawCircle(x,y,r,p);c.drawLine(x+r*.7f,y+r*.7f,x+r*1.65f,y+r*1.65f,p);}
  public void setAlpha(int a){p.setAlpha(a);}public void setColorFilter(ColorFilter f){p.setColorFilter(f);}public int getOpacity(){return PixelFormat.TRANSLUCENT;}
 }
 static final class Controller {
  final Activity a;final String scope;final View root;final WeakReference<Object> fragment;
  final LinkedHashMap<String,PlaylistIndex.Video> index=new LinkedHashMap<String,PlaylistIndex.Video>();final ArrayList<PlaylistIndex.Video> visible=new ArrayList<PlaylistIndex.Video>();
  AlertDialog dialog;EditText query;TextView status;ArrayAdapter<String> adapter;Button stop;volatile PlaylistClient client;volatile int run;boolean closed,complete,loading;int pages,unavailable;String error="";PlaylistSession.Snapshot session;
  final Runnable check=new Runnable(){public void run(){if(closed)return;if(!scope.equals(id(fragment.get()))){dialog.dismiss();return;}if(session!=null&&!PlaylistSession.valid(session)){cancel();index.clear();error="Session modifiée. Appuie sur Actualiser.";render();session=null;}MAIN.postDelayed(this,1000);}};
  final View.OnAttachStateChangeListener attachment=new View.OnAttachStateChangeListener(){public void onViewAttachedToWindow(View v){}public void onViewDetachedFromWindow(View v){if(dialog!=null&&dialog.isShowing())dialog.dismiss();}};
  Controller(Activity a,String s,View r,WeakReference<Object> f){this.a=a;scope=s;root=r;fragment=f;}
  void show(){
   LinearLayout layout=new LinearLayout(a);layout.setOrientation(LinearLayout.VERTICAL);layout.setPadding(dp(root,16),dp(root,8),dp(root,16),0);
   query=new EditText(a);query.setSingleLine(true);query.setHint("Titre ou nom de chaîne");query.setInputType(1);layout.addView(query);
   status=new TextView(a);status.setTextSize(13);status.setPadding(0,dp(root,8),0,dp(root,8));layout.addView(status);
   LinearLayout actions=new LinearLayout(a);stop=new Button(a);stop.setText("Arrêter");Button refresh=new Button(a);refresh.setText("Actualiser");actions.addView(stop);actions.addView(refresh);layout.addView(actions);
   ListView list=new ListView(a);adapter=new ArrayAdapter<String>(a,android.R.layout.simple_list_item_1,new ArrayList<String>());list.setAdapter(adapter);layout.addView(list,new LinearLayout.LayoutParams(-1,dp(root,300)));
   dialog=new AlertDialog.Builder(a).setTitle("Rechercher · "+(scope.equals("WL")?"À regarder plus tard":"Playlist")).setView(layout).setNegativeButton("Fermer",null).create();
   dialog.setOnDismissListener(new DialogInterface.OnDismissListener(){public void onDismiss(DialogInterface d){closed=true;cancel();MAIN.removeCallbacks(check);root.removeOnAttachStateChangeListener(attachment);index.clear();visible.clear();}});
   query.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int af){}public void onTextChanged(CharSequence s,int st,int be,int c){render();}public void afterTextChanged(Editable e){}});
   stop.setOnClickListener(new View.OnClickListener(){public void onClick(View v){cancel();complete=false;error="Chargement arrêté — résultats partiels. Actualiser pour recommencer.";render();}});
   refresh.setOnClickListener(new View.OnClickListener(){public void onClick(View v){start();}});
   list.setOnItemClickListener(new AdapterView.OnItemClickListener(){public void onItemClick(AdapterView<?> p,View v,int pos,long row){if(pos<0||pos>=visible.size()||session==null||!PlaylistSession.valid(session))return;PlaylistIndex.Video video=visible.get(pos);Uri uri=Uri.parse("https://www.youtube.com/watch").buildUpon().appendQueryParameter("v",video.id).appendQueryParameter("list",scope).build();try{a.startActivity(new Intent(Intent.ACTION_VIEW,uri).setPackage(a.getPackageName()));dialog.dismiss();}catch(ActivityNotFoundException e){Toast.makeText(a,"Impossible d’ouvrir cette vidéo dans YouTube.",Toast.LENGTH_SHORT).show();}}});
   dialog.show();root.addOnAttachStateChangeListener(attachment);MAIN.postDelayed(check,1000);start();
  }
  void cancel(){run++;loading=false;PlaylistClient c=client;if(c!=null)c.cancel();}
  void start(){cancel();index.clear();complete=false;pages=unavailable=0;error="";session=PlaylistSession.snapshot();
   if(scope.equals("WL")&&!session.headers.containsKey("Authorization")){error="Session YouTube non disponible. Ferme, recharge la playlist puis réessaie.";render();return;}
   final int task=run;final PlaylistSession.Snapshot auth=session;final PlaylistClient request=new PlaylistClient();client=request;loading=true;render();
   new Thread(new Runnable(){public void run(){String token="",target="";Set<String> seen=new HashSet<String>();int total=0;
    try{for(int page=0;page<1000;page++){if(request.cancelled()||task!=run)return;
     final PlaylistIndex.Page result=PlaylistIndex.parse(request.fetch(scope,token,auth),scope,!token.isEmpty(),target);if(!result.target.isEmpty())target=result.target;total+=result.videos.size();final boolean last=result.next.isEmpty();
     MAIN.post(new Runnable(){public void run(){if(closed||task!=run||!PlaylistSession.valid(auth))return;pages++;unavailable+=result.unavailable;for(PlaylistIndex.Video v:result.videos)if(!index.containsKey(v.id))index.put(v.id,v);complete=last;loading=!last;render();}});
     if(last)return;if(total>20000)throw new IOException("Limite de 20 000 vidéos — résultats partiels.");if(!seen.add(result.next))throw new IOException("Continuation répétée — résultats partiels.");token=result.next;
    }throw new IOException("Limite de pagination — résultats partiels.");}catch(final Exception e){MAIN.post(new Runnable(){public void run(){if(closed||task!=run)return;loading=complete=false;error=e.getMessage()==null?"Chargement impossible.":e.getMessage();render();}});}
   }},"PlaylistSearch").start();
  }
  void render(){if(closed||adapter==null)return;visible.clear();visible.addAll(PlaylistIndex.filter(index.values(),query.getText().toString()));ArrayList<String> rows=new ArrayList<String>();for(PlaylistIndex.Video v:visible)rows.add(v.title+(v.channel.isEmpty()?"":"\n"+v.channel));adapter.clear();adapter.addAll(rows);adapter.notifyDataSetChanged();
   String s=visible.size()+" résultat(s) / "+index.size()+" vidéos indexées · "+pages+" page(s)\n"+(complete?"Parcours terminé à cet instant.":"Résultats partiels"+(loading?" — chargement en cours…":"."));if(unavailable>0)s+="\n"+unavailable+" vidéo(s) sans identifiant.";if(!error.isEmpty())s+="\n"+error;status.setText(s);stop.setEnabled(loading);
  }
 }
}
