package com.dubtimes.connect;

import android.app.*;
import android.os.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
    final int BG=Color.rgb(8,14,24), SURFACE=Color.rgb(14,24,39), CARD=Color.rgb(18,31,49), CARD2=Color.rgb(25,42,65);
    final int TEXT=Color.rgb(241,245,249), MUTED=Color.rgb(148,163,184), BORDER=Color.rgb(42,57,78);
    final int ACCENT=Color.rgb(112,91,255), GREEN=Color.rgb(72,199,116), YELLOW=Color.rgb(245,183,62), BLUE=Color.rgb(75,148,255);
    LinearLayout body, bottomNav;
    String project="Initial D Legend 1", student="Luna Tisera";
    int currentTab=0;

    int dp(int n){ return (int)(n*getResources().getDisplayMetrics().density+.5f); }
    @Override public void onCreate(Bundle b){ super.onCreate(b); getWindow().setStatusBarColor(SURFACE); getWindow().setNavigationBarColor(BG); showProjects(); }

    GradientDrawable shape(int color,float radius){ GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp((int)radius)); return d; }
    GradientDrawable outlined(int color,float radius,int stroke){ GradientDrawable d=shape(color,radius); d.setStroke(dp(1),stroke); return d; }
    TextView tv(String s,int sp,boolean bold,int color){ TextView v=new TextView(this); v.setText(s); v.setTextColor(color); v.setTextSize(sp); if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD); v.setGravity(Gravity.CENTER_VERTICAL); return v; }
    Space space(int h){ Space s=new Space(this); s.setLayoutParams(new LinearLayout.LayoutParams(1,dp(h))); return s; }

    TextView section(String s){ TextView v=tv(s.toUpperCase(Locale.ROOT),11,true,MUTED); v.setLetterSpacing(.08f); v.setPadding(dp(4),dp(8),0,dp(6)); return v; }
    LinearLayout card(){ LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); l.setPadding(dp(16),dp(13),dp(16),dp(13)); l.setBackground(outlined(CARD,16,BORDER)); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.setMargins(0,0,0,dp(9)); l.setLayoutParams(p); return l; }
    TextView chip(String label,int color){ TextView c=tv(label,11,true,color); c.setPadding(dp(9),dp(4),dp(9),dp(4)); c.setBackground(outlined(Color.argb(28,Color.red(color),Color.green(color),Color.blue(color)),50,Color.argb(90,Color.red(color),Color.green(color),Color.blue(color)))); return c; }
    Button primary(String s){ Button b=new Button(this); b.setText(s); b.setTextColor(Color.WHITE); b.setTextSize(14); b.setTypeface(Typeface.DEFAULT,Typeface.BOLD); b.setAllCaps(false); b.setGravity(Gravity.CENTER); b.setPadding(dp(14),0,dp(14),0); b.setBackground(shape(ACCENT,14)); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(50)); p.setMargins(0,dp(5),0,dp(5)); b.setLayoutParams(p); return b; }
    Button secondary(String s){ Button b=primary(s); b.setTextColor(TEXT); b.setBackground(outlined(SURFACE,14,BORDER)); return b; }

    void base(String heading,String subtitle,int tab){
        currentTab=tab;
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(BG);
        LinearLayout top=new LinearLayout(this); top.setOrientation(LinearLayout.VERTICAL); top.setPadding(dp(18),dp(13),dp(18),dp(12)); top.setBackgroundColor(SURFACE);
        top.addView(tv(heading,20,true,TEXT)); if(subtitle!=null&&!subtitle.isEmpty()){ TextView st=tv(subtitle,12,false,MUTED); st.setPadding(0,dp(2),0,0); top.addView(st); }
        root.addView(top,new LinearLayout.LayoutParams(-1,-2));
        ScrollView sv=new ScrollView(this); sv.setFillViewport(true); body=new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL); body.setPadding(dp(14),dp(12),dp(14),dp(18)); sv.addView(body); root.addView(sv,new LinearLayout.LayoutParams(-1,0,1));
        bottomNav=new LinearLayout(this); bottomNav.setPadding(dp(8),dp(6),dp(8),dp(7)); bottomNav.setBackgroundColor(SURFACE); root.addView(bottomNav,new LinearLayout.LayoutParams(-1,dp(64)));
        addNav("▦","Proyectos",0); addNav("✓","Clases",1); addNav("●","Sesión",2);
        setContentView(root);
    }
    void addNav(String icon,String label,int idx){
        TextView n=tv(icon+"\n"+label,11,idx==currentTab,idx==currentTab?ACCENT:MUTED); n.setGravity(Gravity.CENTER); n.setLines(2); n.setBackground(idx==currentTab?shape(Color.argb(24,112,91,255),12):null);
        n.setOnClickListener(v->{ if(idx==0)showProjects(); else if(idx==1)showClasses(); else showSession(); }); bottomNav.addView(n,new LinearLayout.LayoutParams(0,-1,1));
    }

    void showProjects(){
        base("Dub Times Connect","Proyectos y alumnos",0);
        body.addView(section("Proyecto activo"));
        LinearLayout selector=card(); LinearLayout row=new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL); TextView pn=tv(project,16,true,TEXT); row.addView(pn,new LinearLayout.LayoutParams(0,dp(42),1)); TextView arrow=tv("⌄",22,false,MUTED); arrow.setGravity(Gravity.CENTER); row.addView(arrow,new LinearLayout.LayoutParams(dp(42),dp(42))); selector.addView(row); selector.setOnClickListener(v->chooseProject()); body.addView(selector);
        LinearLayout hdr=new LinearLayout(this); hdr.setGravity(Gravity.CENTER_VERTICAL); TextView sh=section("Estudiantes"); hdr.addView(sh,new LinearLayout.LayoutParams(0,-2,1)); TextView count=chip("5 estudiantes",BLUE); hdr.addView(count); body.addView(hdr);
        addStudent("Luna Tisera","Takumi","En proceso",YELLOW); addStudent("Mateo Rojas","2 personajes","Pendiente",MUTED); addStudent("Agustina Silva","1 personaje","Correcciones",BLUE); addStudent("Santiago Pérez","3 personajes","Pendiente",MUTED); addStudent("Valentina Gómez","1 personaje","Terminado",GREEN);
        body.addView(space(3)); Button sync=primary("↻   Sincronizar Discord"); sync.setOnClickListener(v->discordPreview()); body.addView(sync); Button can=secondary("Ver qué pueden grabar"); can.setOnClickListener(v->recordable()); body.addView(can);
    }
    void addStudent(String name,String role,String status,int color){
        LinearLayout c=card(); LinearLayout top=new LinearLayout(this); top.setGravity(Gravity.CENTER_VERTICAL); TextView n=tv(name,16,true,TEXT); top.addView(n,new LinearLayout.LayoutParams(0,dp(32),1)); top.addView(chip(status,color)); c.addView(top); TextView r=tv(role,13,false,MUTED); r.setPadding(0,dp(3),0,0); c.addView(r); c.setOnClickListener(v->{student=name; detail();}); body.addView(c);
    }
    void chooseProject(){ final String[] ps={"Initial D Legend 1","Initial D Legend 3","One Piece Film Red"}; new AlertDialog.Builder(this).setTitle("Elegir proyecto").setItems(ps,(d,w)->{project=ps[w];showProjects();}).show(); }

    void detail(){ base(student,project,0); LinearLayout c=card(); c.addView(tv("Takumi",19,true,TEXT)); c.addView(space(5)); LinearLayout r=new LinearLayout(this); r.addView(chip("En proceso",YELLOW)); c.addView(r); c.addView(space(9)); c.addView(tv("Última clase registrada",11,true,MUTED)); c.addView(tv("4/4",18,true,TEXT)); body.addView(c); Button b=primary("Ver qué puede grabar"); b.setOnClickListener(v->recordable()); body.addView(b); }

    void showClasses(){
        base("Clases y entregas","Registro y publicación en Discord",1);
        body.addView(section("Estudiante")); LinearLayout who=card(); who.addView(tv(student,17,true,TEXT)); TextView sub=tv(project+"  ·  Takumi",13,false,MUTED); sub.setPadding(0,dp(3),0,0); who.addView(sub); body.addView(who);
        body.addView(section("Última clase registrada")); LinearLayout last=card(); LinearLayout lr=new LinearLayout(this); lr.setGravity(Gravity.CENTER_VERTICAL); lr.addView(tv("4/4",24,true,TEXT),new LinearLayout.LayoutParams(0,dp(40),1)); lr.addView(chip("Comprobante disponible",GREEN)); last.addView(lr); body.addView(last);
        body.addView(section("Vista previa del mensaje")); LinearLayout m=card(); m.addView(tv("ESTUDIANTE:  "+student+"\nPROYECTO:  "+project+"\nPERSONAJE:  Takumi\nCLASE:  4/4",13,false,TEXT)); body.addView(m); Button p=primary("Publicar clase en Discord"); p.setOnClickListener(v->toast("Prototipo: la conexión real se incorporará después de validar la interfaz.")); body.addView(p);
    }

    void showSession(){
        base("Sesión","Seleccioná quiénes están presentes",2); body.addView(section("Estudiantes presentes"));
        for(String s:new String[]{"Luna Tisera","Mateo Rojas","Agustina Silva","Santiago Pérez"}){ LinearLayout c=card(); CheckBox cb=new CheckBox(this); cb.setText(s); cb.setTextColor(TEXT); cb.setTextSize(15); cb.setButtonTintList(new android.content.res.ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{ACCENT,MUTED})); c.addView(cb); body.addView(c); }
        Button b=primary("Ver qué pueden grabar"); b.setOnClickListener(v->recordable()); body.addView(b);
    }
    void recordable(){ base("Qué pueden grabar",student,2); body.addView(section("Personajes individuales")); addRole("Takumi","En proceso",YELLOW); addRole("Ryosuke","Disponible",GREEN); addRole("Keisuke","Disponible",GREEN); body.addView(section("Personajes colectivos")); addRole("Todos","Pendiente",YELLOW); addRole("Hombres","Disponible",GREEN); addRole("Mujeres","Disponible",GREEN); addRole("Walla","Pendiente",YELLOW); }
    void addRole(String n,String st,int color){ LinearLayout c=card(); LinearLayout r=new LinearLayout(this); r.setGravity(Gravity.CENTER_VERTICAL); r.addView(tv(n,16,true,TEXT),new LinearLayout.LayoutParams(0,dp(34),1)); r.addView(chip(st,color)); c.addView(r); body.addView(c); }

    void discordPreview(){ base("Sincronizar Discord","Vista previa antes de sincronizar",0); LinearLayout c=card(); c.addView(tv("ESTUDIANTE",10,true,MUTED)); c.addView(tv(student,14,true,TEXT)); c.addView(space(7)); c.addView(tv("PROYECTO",10,true,MUTED)); c.addView(tv(project,14,true,TEXT)); c.addView(space(7)); c.addView(tv("PERSONAJE",10,true,MUTED)); c.addView(tv("Takumi",14,true,TEXT)); c.addView(space(7)); c.addView(tv("CLASE",10,true,MUTED)); c.addView(tv("4/4 · comprobante vinculado",14,true,TEXT)); body.addView(c); LinearLayout ok=card(); LinearLayout r=new LinearLayout(this); r.setGravity(Gravity.CENTER_VERTICAL); r.addView(chip("✓ Listo",GREEN)); TextView info=tv("  Interfaz móvil preparada para la prueba",13,false,MUTED); r.addView(info); ok.addView(r); body.addView(ok); }
    void toast(String s){ Toast.makeText(this,s,Toast.LENGTH_LONG).show(); }
}
