package fr.nesquik.hidden;

import android.app.*;
import android.content.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import app.morphe.extension.crimera.settings.CustomScreenFragment;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public final class Settings extends CustomScreenFragment {
    private static final int OPEN=6711, SAVE=6712;

    private ArrayAdapter<String> adapter;
    private TextView status;
    private Button count;
    private Switch toggle;
    @Override public View onCreateView(LayoutInflater inf,ViewGroup parent,Bundle state){
        Activity a=getActivity();
        LinearLayout box=new LinearLayout(a); box.setOrientation(1);
        toggle=new Switch(a);toggle.setText("Activer le masquage des posts");box.addView(toggle);
        toggle.setChecked(Store.enabled());
        toggle.setOnCheckedChangeListener((v,on)->job(()->{Store.enabled(on);return "Réglage enregistré";}));
        button(box,"Importer une sauvegarde",v->pick(OPEN));
        button(box,"Exporter les identifiants",v->pick(SAVE));
        count=button(box,"",v->new AlertDialog.Builder(a).setMessage("Retirer tous les posts de la liste ?")
            .setNegativeButton("Annuler",null).setPositiveButton("Vider",(d,w)->job(()->{Store.edit(Collections.emptySet(),null,true);return "Liste vidée";})).show());
        status=new TextView(a);box.addView(status);
        ListView list=new ListView(a);box.addView(list,new LinearLayout.LayoutParams(-1,0,1));
        adapter=new ArrayAdapter<>(a,android.R.layout.simple_list_item_1,new ArrayList<>());
        list.setAdapter(adapter);
        list.setOnItemClickListener((p,v,pos,id)->{
            String selected=adapter.getItem(pos);
            new AlertDialog.Builder(a).setMessage("Rendre visible le post "+selected+" ?")
                .setNegativeButton("Annuler",null).setPositiveButton("Retirer",(d,w)->job(()->{
                    Store.edit(Collections.emptySet(),selected,false);return "Post retiré";
                })).show();
        });
        refresh();return box;
    }
    private void refresh(){
        adapter.clear();adapter.addAll(new TreeSet<>(Store.ids()));
        count.setText("Vider la liste ("+adapter.getCount()+")");
    }
    private Button button(LinearLayout box,String text,View.OnClickListener click){
        Button b=new Button(getActivity());b.setText(text);box.addView(b);b.setOnClickListener(click);return b;
    }
    private void pick(int request){
        Intent i=new Intent(request==OPEN?Intent.ACTION_OPEN_DOCUMENT:Intent.ACTION_CREATE_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE).setType("text/plain");
        if(request==SAVE)i.putExtra(Intent.EXTRA_TITLE,"nesquik-hidden-posts.txt");
        try{startActivityForResult(i,request);}catch(ActivityNotFoundException e){status.setText("Sélecteur de fichiers indisponible");}
    }
    private void job(Callable<String> action){
        Activity a=getActivity();
        Store.IO.execute(()->{
            String result;
            try{result=action.call();}catch(Exception e){result="Erreur : "+e.getMessage();}
            String message=result;
            a.runOnUiThread(()->{if(isAdded() && getView()!=null){refresh();status.setText(message);}});
        });
    }
    @Override public void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(result!=Activity.RESULT_OK || data==null || data.getData()==null)return;
        ContentResolver resolver=getActivity().getContentResolver();
        if(request==OPEN)job(()->{
            Set<String> parsed;
            try(InputStream in=resolver.openInputStream(data.getData())){parsed=Ids.read(in);}
            Store.edit(parsed,null,false);return "Import terminé";
        });
        if(request==SAVE)job(()->{
            try(OutputStream out=resolver.openOutputStream(data.getData(),"wt")){Ids.write(Store.ids(),out);}
            return "Export terminé";
        });
    }
}
