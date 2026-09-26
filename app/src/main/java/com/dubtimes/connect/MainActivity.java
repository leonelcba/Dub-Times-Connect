package com.dubtimes.connect;

import android.app.*;
import android.os.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.*;
import android.widget.*;
import android.graphics.drawable.GradientDrawable;
import java.util.*;

public class MainActivity extends Activity {
    final int BG=Color.rgb(9,15,26), CARD=Color.rgb(18,31,49), CARD2=Color.rgb(25,42,65);
    final int TEXT=Color.rgb(238,242,248), MUTED=Color.rgb(157,174,195), ACCENT=Color.rgb(101,87,255);
    LinearLayout body;
    TextView title;
    String project="Initial D Legend 1";
    String student="Luna Tisera";

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        showMain();
    }

    TextView text(String s,int sp,boolean bold) {
        TextView v=new TextView(this); v.setText(s); v.setTextColor(TEXT); v.setTextSize(sp);
        v.setPadding(12,10,12,10); if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD); return v;
    }
    GradientDrawable bg(int color,int radius) {
        GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(radius); return d;
    }
    Button button(String s) {
        Button b=new Button(this); b.setText(s); b.setTextColor(Color.WHITE); b.setTextSize(14);
        b.setAllCaps(false); b.setBackground(bg(ACCENT,18)); return b;
    }
    LinearLayout card() {
        LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(12,8,12,8); l.setBackground(bg(CARD,18));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.setMargins(12,6,12,6); l.setLayoutParams(p); return l;
    }
    void base(String heading) {
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(BG);
        title=text("Dub Times Connect",20,true); title.setGravity(Gravity.CENTER_VERTICAL); title.setBackgroundColor(Color.rgb(11,22,38));
        root.addView(title,new LinearLayout.LayoutParams(-1,64));
        LinearLayout nav=new LinearLayout(this);
        String[] ns={"Proyectos y alumnos","Clases y entregas","Sesión"};
        for(String n:ns){ Button x=button(n); x.setBackgroundColor(Color.TRANSPARENT);
            x.setOnClickListener(v->{ if(n.startsWith("Proyectos")) projects(); else if(n.startsWith("Clases")) classes(); else session();});
            nav.addView(x,new LinearLayout.LayoutParams(0,54,1));}
        root.addView(nav);
        ScrollView sv=new ScrollView(this); body=new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL); body.setPadding(8,8,8,24);
        sv.addView(body); root.addView(sv,new LinearLayout.LayoutParams(-1,0,1)); setContentView(root);
        title.setText(heading);
    }
    void showMain(){ projects(); }

    void projects() {
        base("Dub Times Connect");
        body.addView(text("PROYECTO",12,true));
        Spinner sp=new Spinner(this);
        String[] ps={"Initial D Legend 1","Initial D Legend 3","One Piece Film Red"};
        ArrayAdapter<String> a=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,ps);
        sp.setAdapter(a); sp.setBackground(bg(CARD2,14)); body.addView(sp,new LinearLayout.LayoutParams(-1,58));
        sp.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
            public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){project=ps[pos];}
            public void onNothingSelected(android.widget.AdapterView<?> p){}
        });
        body.addView(text("ESTUDIANTES",12,true));
        String[] students={"Luna Tisera","Mateo Rojas","Agustina Silva","Santiago Pérez","Valentina Gómez"};
        for(String s:students){
            LinearLayout c=card(); TextView n=text(s,16,true); c.addView(n);
            TextView sub=text(s.equals("Luna Tisera")?"Takumi · 🟡 En proceso":"Personajes asignados",13,false); sub.setTextColor(MUTED); c.addView(sub);
            c.setOnClickListener(v->{student=s; detail();}); body.addView(c);
        }
        Button sync=button("⟳  Sincronizar Discord"); sync.setOnClickListener(v->discordPreview()); body.addView(sync);
        Button can=button("👥  Ver qué pueden grabar"); can.setOnClickListener(v->recordable()); body.addView(can);
    }

    void detail(){
        base(student);
        LinearLayout c=card(); c.addView(text(student,20,true)); c.addView(text(project,14,false));
        c.addView(text("Personaje     Takumi",15,false)); c.addView(text("Estado          🟡 En proceso",15,false));
        c.addView(text("Última clase   4/4",15,false)); body.addView(c);
        Button b=button("Ver qué puede grabar"); b.setOnClickListener(v->recordable()); body.addView(b);
    }

    void classes(){
        base("Clases y entregas");
        LinearLayout c=card(); c.addView(text(student,19,true)); c.addView(text(project+" · Takumi",14,false)); body.addView(c);
        body.addView(text("ÚLTIMA CLASE REGISTRADA",12,true));
        body.addView(text("4/4 · Comprobante disponible",16,true));
        body.addView(text("VISTA PREVIA DEL MENSAJE",12,true));
        LinearLayout m=card();
        m.addView(text("ESTUDIANTE: "+student+"\nPROYECTO: "+project+"\nPERSONAJE: Takumi\nCLASE: 4/4",15,false));
        body.addView(m);
        Button p=button("Publicar clase en Discord"); p.setOnClickListener(v->toast("Prototipo: publicación real se conectará en la siguiente etapa.")); body.addView(p);
    }

    void session(){
        base("Sesión");
        body.addView(text("ESTUDIANTES PRESENTES",12,true));
        for(String s:new String[]{"Luna Tisera","Mateo Rojas","Agustina Silva"}){
            CheckBox cb=new CheckBox(this); cb.setText(s); cb.setTextColor(TEXT); cb.setTextSize(16); cb.setPadding(12,12,12,12); body.addView(cb);
        }
        Button b=button("Ver qué pueden grabar"); b.setOnClickListener(v->recordable()); body.addView(b);
    }

    void recordable(){
        base("Ver qué pueden grabar");
        body.addView(text(student,19,true)); body.addView(text("PERSONAJES INDIVIDUALES",12,true));
        addRole("Takumi","🟡 Pendiente"); addRole("Ryosuke","⚪ Disponible"); addRole("Keisuke","⚪ Disponible");
        body.addView(text("PERSONAJES COLECTIVOS",12,true));
        addRole("Todos","🟡 Pendiente"); addRole("Hombres","⚪ Disponible"); addRole("Mujeres","⚪ Disponible"); addRole("Walla","🟡 Pendiente");
    }
    void addRole(String n,String st){ LinearLayout c=card(); c.addView(text(n,16,true)); TextView x=text(st,13,false); x.setTextColor(Color.rgb(93,220,135)); c.addView(x); body.addView(c); }

    void discordPreview(){
        base("Sincronizar Discord");
        body.addView(text("Vista previa del formato que enviará Dub Times Connect",14,false));
        LinearLayout c=card();
        TextView t=text("**ESTUDIANTE:** "+student+"\n**PROYECTO:** "+project+"\n**PERSONAJE:** Takumi\n**CLASE:** [4/4](enlace-al-comprobante)",15,false);
        c.addView(t); body.addView(c);
        ProgressBar p=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal); p.setProgress(100); body.addView(p);
        body.addView(text("✓ Interfaz móvil lista para prueba\n\nLa conexión real a Discord se incorporará después de validar este flujo Android.",14,false));
    }
    void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
}
