package app.morphe.extension.newx.anonymous;
import android.app.*;import android.content.*;import android.os.Bundle;import android.view.*;import android.widget.*;
import app.morphe.extension.crimera.settings.CustomScreenFragment;
public final class AnonymousNamesFragment extends CustomScreenFragment {
 @Override public View onCreateView(LayoutInflater inflater,ViewGroup container,Bundle state){
  Context c=getActivity();ScrollView scroll=new ScrollView(c);LinearLayout body=new LinearLayout(c);body.setOrientation(LinearLayout.VERTICAL);int pad=(int)(16*c.getResources().getDisplayMetrics().density);body.setPadding(pad,pad,pad,pad);scroll.addView(body);
  TextView title=new TextView(c);title.setText("Anonymous Names");title.setTextSize(22);body.addView(title);
  Names.Config config=Names.config();toggle(body,"Activer l’anonymisation","enabled",config.enabled);toggle(body,"Anonymiser les noms affichés","names",config.names);toggle(body,"Anonymiser les @identifiants","handles",config.handles);
  Switch reveal=new Switch(c);reveal.setText("Révéler les vrais noms sur les profils");reveal.setChecked(true);reveal.setEnabled(false);body.addView(reveal);
  TextView info=new TextView(c);info.setText("Les profils et la recherche de comptes gardent toujours leurs vraies identités. Les publications restent anonymisées, même dans un profil.\n\nLes changements agissent au prochain rendu. Les cartes déjà affichées peuvent rester inchangées : ferme puis rouvre X pour une mise à jour complète. Aucun besoin de réinstaller.\n\nLes avatars, mentions et textes des publications restent inchangés. Les pseudonymes sont stables sur cette installation. Effacer les données de X change leur attribution.");info.setPadding(0,pad,0,0);body.addView(info);return scroll;
 }
 private void toggle(LinearLayout body,String text,String key,boolean checked){Switch view=new Switch(body.getContext());view.setText(text);view.setChecked(checked);body.addView(view);view.setOnCheckedChangeListener((b,on)->Names.set(key,on));}
}
