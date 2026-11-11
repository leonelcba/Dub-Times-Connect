package com.dubtimes.connectviewer;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.security.keystore.*;
import android.text.*;
import android.view.*;
import android.widget.*;
import org.json.*;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
 private final int BG=Color.rgb(16,16,35),CARD=Color.rgb(31,29,55),FG=Color.rgb(238,236,250),MUTED=Color.rgb(183,179,211),ACCENT=Color.rgb(169,151,255);
 private final ExecutorService executor=Executors.newSingleThreadExecutor();
 private final Handler handler=new Handler(Looper.getMainLooper());
 private JSONObject pairing=null,snapshot=null;
 private LinearLayout list,root;
 private TextView status,mode;
 private EditText search;
 private Button update;
 private int tab=0;
 private volatile boolean busy=false;
 private String notice="";
 private long generation=0;
 private final Runnable periodic=new Runnable(){public void run(){refresh(false);handler.postDelayed(this,60000);}};
 @Override public void onCreate(Bundle state){
  super.onCreate(state);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
  root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(14),dp(8),dp(14),dp(4));root.setBackgroundColor(BG);root.setFitsSystemWindows(true);setContentView(root);
  if(Build.VERSION.SDK_INT>=35){root.setOnApplyWindowInsetsListener((v,insets)->{android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars());root.setPadding(dp(14)+bars.left,dp(8)+bars.top,dp(14)+bars.right,dp(4)+bars.bottom);return insets;});}
  root.addView(label("Dub Times Connect",24,FG,true));mode=label("CONSULTA · SOLO LECTURA",12,ACCENT,true);root.addView(mode);
  status=label("Sin vincular",12,MUTED,false);root.addView(status);
  LinearLayout actions=new LinearLayout(this);Button pair=button("Vincular");update=button("Actualizar");actions.addView(pair,new LinearLayout.LayoutParams(0,dp(48),1));Button changes=button("Cambios");changes.setOnClickListener(v->changesDialog());actions.addView(changes,new LinearLayout.LayoutParams(0,dp(48),1));actions.addView(update,new LinearLayout.LayoutParams(0,dp(48),1));root.addView(actions);
  pair.setOnClickListener(v->pairMenu());update.setOnClickListener(v->refresh(true));
  search=new EditText(this);search.setTextColor(FG);search.setHintTextColor(MUTED);search.setHint("Buscar alumno, proyecto o personaje…");search.setSingleLine(true);root.addView(search);
  ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);list=new LinearLayout(this);list.setOrientation(1);scroll.addView(list);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
  HorizontalScrollView tabScroll=new HorizontalScrollView(this);tabScroll.setHorizontalScrollBarEnabled(false);LinearLayout tabs=new LinearLayout(this);String[] names={"Proyectos","Alumnos · Hoy","Clases","Tareas","Evolución","Editar"};
  for(int i=0;i<names.length;i++){final int index=i;Button b=button(names[i]);b.setTextSize(11);b.setPadding(0,0,0,0);tabs.addView(b,new LinearLayout.LayoutParams(dp(i==1?136:104),dp(50)));b.setOnClickListener(v->{tab=index;search.setText("");render();});}tabScroll.addView(tabs);root.addView(tabScroll);
  search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int f){}public void onTextChanged(CharSequence s,int a,int b,int c){render();}public void afterTextChanged(Editable e){}});
  try{String stored=getPreferences(0).getString("pairing",null);if(stored!=null)pairing=new JSONObject(decrypt(stored));}catch(Exception e){notice="No se pudo recuperar la vinculación. Vuelve a vincular.";}
  try{File f=new File(getFilesDir(),"snapshot.json");if(f.exists()){snapshot=new JSONObject(readAll(new FileInputStream(f),8388608));validateSnapshot(snapshot);}}catch(Exception e){snapshot=null;notice="La copia local no es válida. Actualiza para recuperarla.";}
  try{File f=new File(getFilesDir(),"changes.json");if(f.exists())outbox=new JSONArray(readAll(new FileInputStream(f),8388608));}catch(Exception e){notice="No se pudieron recuperar los cambios locales.";}
  render();
 }
 @Override protected void onResume(){super.onResume();handler.removeCallbacks(periodic);handler.post(periodic);}
 @Override protected void onPause(){handler.removeCallbacks(periodic);super.onPause();}
 @Override protected void onDestroy(){handler.removeCallbacks(periodic);executor.shutdownNow();super.onDestroy();}
 private int dp(int x){return (int)(getResources().getDisplayMetrics().density*x+.5f);}
 private TextView label(String text,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(text);t.setTextSize(size);t.setTextColor(color);t.setPadding(dp(4),dp(4),dp(4),dp(4));if(bold)t.setTypeface(null,Typeface.BOLD);return t;}
 private Button button(String text){Button b=new Button(this);b.setText(text);b.setAllCaps(false);b.setTextColor(FG);return b;}
 private void card(String title,String body,Runnable click){
  LinearLayout box=new LinearLayout(this);box.setOrientation(1);box.setPadding(dp(10),dp(8),dp(10),dp(8));GradientDrawable bg=new GradientDrawable();bg.setColor(CARD);bg.setCornerRadius(dp(12));box.setBackground(bg);
  box.addView(label(title,17,FG,true));box.addView(label(body,14,MUTED,false));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(6),0,dp(6));list.addView(box,lp);if(click!=null){box.setOnClickListener(v->click.run());box.setContentDescription(title+". Abrir detalle de consulta");}
 }
 private boolean match(String value){return value.toLowerCase(Locale.ROOT).contains(search.getText().toString().trim().toLowerCase(Locale.ROOT));}
 private String text(JSONObject o,String key){Object v=o.opt(key);return v==null||v==JSONObject.NULL?"—":String.valueOf(v);}
 private String pct(JSONObject o,String key){return o.isNull(key)||!o.has(key)?"Sin datos":String.format(Locale.getDefault(),"%.1f%%",o.optDouble(key));}
 private JSONArray arr(JSONObject o,String key){JSONArray a=o.optJSONArray(key);return a==null?new JSONArray():a;}
 private String date(String iso){try{java.time.OffsetDateTime d=java.time.OffsetDateTime.parse(iso.replace(" ","T"));return d.atZoneSameInstant(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));}catch(Exception e){return iso.length()>16?iso.substring(0,16).replace('T',' '):iso;}}
 private void render(){
  if(list==null)return;mode.setText(canEdit()?"EDICIÓN VINCULADA CON WINDOWS":"CONSULTA · SOLO LECTURA");list.removeAllViews();update.setEnabled(pairing!=null&&!busy);
  status.setText((snapshot!=null?"Copia de Windows: "+date(snapshot.optString("generated_at","Sin fecha")):"Todavía no hay una copia descargada")+(notice.isEmpty()?"":"\n"+notice)+(pendingCount()>0?"\nCambios pendientes: "+pendingCount():""));
  if(snapshot==null){card(pairing==null?"Vincula tu computadora":"Listo para consultar",pairing==null?"En Windows: Android → Sincronización en la nube. Importa el archivo de vinculación para Android.":"Pulsa Actualizar después de subir la primera copia desde Windows.",null);return;}
  if(tab==0)projects();else if(tab==1)studentsToday();else if(tab==2)classes();else if(tab==3)tasks();else if(tab==4)learning();else editors();
  if(list.getChildCount()==0)card("Sin resultados","No hay datos que coincidan con la búsqueda en la última copia.",null);
 }
 private void projects(){
  for(int i=0;i<arr(snapshot,"projects").length();i++){
   JSONObject p=arr(snapshot,"projects").optJSONObject(i);if(p==null||!match(p.toString()))continue;
   JSONArray versions=arr(p,"versions");int total=0;StringBuilder details=new StringBuilder();
   for(int j=0;j<versions.length();j++){JSONObject v=versions.optJSONObject(j);if(v==null)continue;details.append(v.optString("name")).append("\n");JSONArray chars=arr(v,"characters");
    for(int k=0;k<chars.length();k++){JSONObject c=chars.optJSONObject(k);if(c==null)continue;total++;details.append(text(c,"state")).append(" ").append(text(c,"name")).append(" · ").append(text(c,"actor")).append(" · ").append(c.optInt("entries")).append(" entradas\n");}details.append("\n");}
   final String body=details.toString();card(p.optString("name"),total+" personajes · "+versions.length()+" versiones\nToca para consultar personajes y asignaciones.",()->detail(p.optString("name"),body,p.optString("url")));
  }
 }
 private static class PendingRole {
  String student,project,version,character,kind,state;int entries;
  PendingRole(String a,String p,String v,String c,String k,String st,int e){student=a;project=p;version=v;character=c;kind=k;state=st;entries=e;}
 }
 private String normStudent(String raw){return raw==null?"":raw.trim().toLowerCase(Locale.ROOT);}
 private boolean isDone(String state){return "🟢".equals(state)||"TERMINADO".equalsIgnoreCase(state);}
 private String stateLabel(String code){if("⚪".equals(code))return "Pendiente";if("🟡".equals(code))return "En proceso";if("🔵".equals(code))return "Correcciones";if("⛔".equals(code))return "Sin asignar";if("🟢".equals(code))return "Terminado";return code;}
 private boolean collective(String name,String actors){String n=normStudent(name);return Arrays.asList("todos","hombres","mujeres","walla","wallas","gente","otros","otras","niños","niñas","extras").contains(n)||(actors!=null&&actors.contains(","));}
 private List<PendingRole> pendingRoles(){
  List<PendingRole> result=new ArrayList<>();if(snapshot==null)return result;
  Set<String> known=new HashSet<>();JSONArray students=arr(snapshot,"students");for(int i=0;i<students.length();i++){JSONObject st=students.optJSONObject(i);if(st!=null)known.add(normStudent(st.optString("name")));}
  JSONArray projects=arr(snapshot,"projects");for(int a=0;a<projects.length();a++){
   JSONObject project=projects.optJSONObject(a);if(project==null)continue;JSONArray versions=arr(project,"versions");
   for(int b=0;b<versions.length();b++){JSONObject version=versions.optJSONObject(b);if(version==null)continue;JSONArray chars=arr(version,"characters");
    for(int c=0;c<chars.length();c++){JSONObject ch=chars.optJSONObject(c);if(ch==null||isDone(ch.optString("state")))continue;
     String actor=ch.optString("actor",""),character=ch.optString("name",""),st=ch.optString("state","⚪");int entries=ch.optInt("entries",0);
     if(collective(character,actor)){
      JSONArray members=arr(ch,"members");if(members.length()>0){for(int d=0;d<members.length();d++){JSONObject m=members.optJSONObject(d);if(m==null||isDone(m.optString("state")))continue;String name=m.optString("student_name","").trim();if(known.contains(normStudent(name)))result.add(new PendingRole(name,project.optString("name"),version.optString("name"),character,"Colectivo",m.optString("state","⚪"),entries));}}
      else{ // Copias antiguas: los colectivos carecen de estados individuales.
       for(String raw:actor.split("[,;]")){String name=raw.trim();if(known.contains(normStudent(name)))result.add(new PendingRole(name,project.optString("name"),version.optString("name"),character,"Colectivo (estado global)",st,entries));}}
     }else{
      String[] candidates=actor.split("/");for(String raw:candidates){String name=raw.trim();if(known.contains(normStudent(name)))result.add(new PendingRole(name,project.optString("name"),version.optString("name"),character,candidates.length>1?"Candidato":"Asignado",st,entries));}
     }
    }
   }
  }
  Collections.sort(result,(a,b)->{int x=a.student.compareToIgnoreCase(b.student);if(x!=0)return x;int y=a.project.compareToIgnoreCase(b.project);if(y!=0)return y;return a.character.compareToIgnoreCase(b.character);});return result;
 }
 private void studentsToday(){
  JSONArray students=arr(snapshot,"students");List<PendingRole> roles=pendingRoles();String query=search.getText().toString().trim().toLowerCase(Locale.ROOT);int visible=0;
  card("Qué pueden grabar hoy","Disponibilidad según estados y asignaciones de la última copia de Windows. No representa horarios reservados ni una agenda diaria.\nActualizado: "+date(snapshot.optString("generated_at","Sin fecha")),null);
  for(int i=0;i<students.length();i++){JSONObject st=students.optJSONObject(i);if(st==null)continue;String name=st.optString("name","");List<PendingRole> mine=new ArrayList<>();for(PendingRole r:roles)if(normStudent(r.student).equals(normStudent(name)))mine.add(r);
   boolean matchQuery=name.toLowerCase(Locale.ROOT).contains(query);if(!matchQuery){for(PendingRole r:mine)if((r.project+" "+r.version+" "+r.character).toLowerCase(Locale.ROOT).contains(query)){matchQuery=true;break;}}if(!matchQuery)continue;
   visible++;String summary=mine.isEmpty()?"Sin asignaciones pendientes registradas":mine.size()+" asignación(es) disponibles · "+mine.get(0).project+(mine.size()>1?" y más proyectos":"");
   card(name,summary,()->studentPendingDetail(name,mine));
  }
  if(visible==0)card("Sin alumnos","No se encontraron alumnos para esta búsqueda.",null);
 }
 private void studentPendingDetail(String name,List<PendingRole> roles){
  ScrollView sc=new ScrollView(this);LinearLayout body=new LinearLayout(this);body.setOrientation(1);body.setPadding(dp(14),dp(8),dp(14),dp(8));sc.addView(body);
  body.addView(label(roles.size()+" trabajo(s) pendientes según Windows",15,ACCENT,true));
  if(roles.isEmpty())body.addView(label("Este alumno no tiene personajes pendientes registrados en la última copia.",14,MUTED,false));
  for(PendingRole r:roles){LinearLayout group=new LinearLayout(this);group.setOrientation(1);group.setPadding(dp(8),dp(8),dp(8),dp(8));GradientDrawable bg=new GradientDrawable();bg.setColor(CARD);bg.setCornerRadius(dp(10));group.setBackground(bg);
   group.addView(label(r.character+" · "+stateLabel(r.state),16,FG,true));group.addView(label(r.project+" / "+r.version+"\n"+r.kind+" · "+r.entries+" entradas en el personaje",13,MUTED,false));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(4),0,dp(4));body.addView(group,lp);
  }
  new AlertDialog.Builder(this).setTitle(name+" · Qué puede grabar").setView(sc).setPositiveButton("Cerrar",null).show();
 }
 private void classes(){
  JSONArray all=arr(snapshot,"classes");Map<String,Set<String>> debts=new HashMap<>();
  for(int i=0;i<all.length();i++){JSONObject c=all.optJSONObject(i);if(c!=null&&!c.optBoolean("paid_status",c.optInt("paid_status",1)!=0)){
   String student=c.optString("student_id"),day=c.optString("created_at");if(day.length()>10)day=day.substring(0,10);String key=c.optString("package_id")+":"+Math.abs(c.optInt("class_number"))+":"+day;debts.computeIfAbsent(student,k->new HashSet<>()).add(key);}}
  for(int i=0;i<all.length();i++){
   JSONObject c=all.optJSONObject(i);if(c==null||!match(c.toString()))continue;boolean paid=c.optInt("paid_status",1)!=0;
   String title=c.optString("student")+" · Clase "+c.optInt("class_number")+"/"+c.optInt("total",4);
   String body=c.optString("project_name")+" · "+c.optString("character_name")+"\n"+date(c.optString("created_at"))+" · "+(paid?"Pagada":"Sin pagar")+"\nMinutos pendientes: "+c.optInt("pending_minutes")+" · Clases sin pagar del alumno: "+debts.getOrDefault(c.optString("student_id"),Collections.emptySet()).size();
   card(title,body,()->classDetail(title,body,c));
  }
 }
 private void tasks(){
  JSONArray students=arr(snapshot,"activities");for(int i=0;i<students.length();i++){
   JSONObject s=students.optJSONObject(i);if(s==null||!match(s.optString("student")))continue;JSONArray countries=arr(s,"countries"),analysis=arr(s,"analysis");int done=0;for(int j=0;j<countries.length();j++){JSONObject v=countries.optJSONObject(j);if(v!=null&&v.optBoolean("done"))done++;}
   card(s.optString("student"),"Países: "+done+" realizados / "+countries.length()+"\nAnálisis: "+analysis.length()+" actividades\nToca para ver estados y comentarios.",()->taskDetail(s));
  }
 }
 private void learning(){JSONArray rows=arr(snapshot,"learning");for(int i=0;i<rows.length();i++){
  JSONObject r=rows.optJSONObject(i);if(r==null||!match(r.optString("name")))continue;
  card(r.optString("name"),"Reconocimiento auditivo: "+pct(r,"auditory")+"\nAspectos identificados: "+pct(r,"coverage")+"\nArgumentación del profesor: "+pct(r,"argument")+"\nPuntos de análisis: "+(r.isNull("points")?"Sin datos":text(r,"points"))+"\nParticipación: "+r.optInt("participation"),null);
 }}
 private void detail(String title,String body,String url){ScrollView sc=new ScrollView(this);TextView t=label(body,15,FG,false);t.setTextIsSelectable(true);sc.addView(t);AlertDialog.Builder b=new AlertDialog.Builder(this).setTitle(title).setView(sc).setNegativeButton("Cerrar",null);if(validDiscord(url))b.setPositiveButton("Abrir enlace",(d,w)->openLink(url));b.show();}
 private boolean validDiscord(String url){try{URI u=new URI(url);return "https".equals(u.getScheme())&&("discord.com".equals(u.getHost())||"discordapp.com".equals(u.getHost()));}catch(Exception e){return false;}}
 private void openLink(String url){if(validDiscord(url)){try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}catch(Exception e){toast("No hay una aplicación para abrir este enlace.");}}}
 private void classDetail(String title,String body,JSONObject c){new AlertDialog.Builder(this).setTitle(title).setMessage(body).setItems(new String[]{"Abrir publicación del video","Abrir comprobante"},(d,w)->openLink(c.optString(w==0?"video_url":"receipt_url"))).setNegativeButton("Cerrar",null).show();}
 private void taskDetail(JSONObject s){String[] choices={"Países de doblaje","Análisis"};new AlertDialog.Builder(this).setTitle(s.optString("student")).setItems(choices,(d,w)->{
  JSONArray videos=arr(s,w==0?"countries":"analysis");String[] names=new String[videos.length()];for(int i=0;i<names.length;i++){JSONObject v=videos.optJSONObject(i);names[i]=w==0?"Video "+v.optInt("number")+" · "+(v.optBoolean("done")?v.optString("result"):"Pendiente"):v.optString("title")+" · "+v.optString("state");}
  new AlertDialog.Builder(this).setTitle(choices[w]).setItems(names,(dd,index)->{JSONObject v=videos.optJSONObject(index);if(w==0)detail(names[index],"Resultado: "+text(v,"result"),v.optString("url"));else{
   StringBuilder body=new StringBuilder("Coincidencia: "+pct(v,"agreement")+"\nAspectos identificados: "+pct(v,"coverage")+"\nDiferencias: "+pct(v,"differences")+"\n\n");JSONArray aspects=arr(v,"aspects");
   for(int j=0;j<aspects.length();j++){JSONObject a=aspects.optJSONObject(j);body.append(a.optString("aspect")).append(" — ").append(a.optString("status")).append("\nProfesor: ").append(a.optString("teacher")).append("\nAlumno: ").append(a.optString("student")).append("\n").append(a.optString("reason")).append("\n\n");}
   body.append("APORTES ADICIONALES\n");JSONArray extras=arr(v,"extras");for(int j=0;j<extras.length();j++)body.append("• ").append(extras.optString(j)).append("\n");detail(names[index],body.toString(),v.optString("url"));}
  }).setNegativeButton("Cerrar",null).show();
 }).setNegativeButton("Cerrar",null).show();}
 private void pairMenu(){new AlertDialog.Builder(this).setTitle("Vinculación con Windows").setItems(new String[]{"Importar archivo de Windows","Pegar contenido del archivo","Desvincular y borrar copia local"},(d,w)->{
  if(w==0){Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);intent.setType("*/*");intent.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(intent,12);}
  if(w==1){EditText input=new EditText(this);input.setHint("Contenido de DubTimes_Android.json");new AlertDialog.Builder(this).setTitle("Pegar vinculación").setView(input).setPositiveButton("Vincular",(dd,ww)->applyPair(input.getText().toString())).setNegativeButton("Cancelar",null).show();}
  if(w==2)new AlertDialog.Builder(this).setMessage("¿Borrar la vinculación y la copia descargada de este dispositivo?").setPositiveButton("Desvincular",(dd,ww)->{generation++;pairing=null;snapshot=null;outbox=new JSONArray();new File(getFilesDir(),"changes.json").delete();getPreferences(0).edit().clear().apply();new File(getFilesDir(),"snapshot.json").delete();notice="Desvinculado";render();}).setNegativeButton("Cancelar",null).show();
 }).setNegativeButton("Cerrar",null).show();}
 @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request==12&&result==RESULT_OK&&data!=null){try{applyPair(readAll(getContentResolver().openInputStream(data.getData()),65536));}catch(Exception e){toast("No se pudo leer el archivo de vinculación.");}}}
 private void applyPair(String raw){try{
  JSONObject p=new JSONObject(raw);if((!"dubtimes-readonly-v1".equals(p.optString("format"))&&!"dubtimes-edit-v2".equals(p.optString("format")))||p.has("writer_key"))throw new Exception();if("dubtimes-edit-v2".equals(p.optString("format"))&&!p.optString("editor_key").matches("[0-9a-f]{64}"))throw new Exception();
  if(!p.optString("url").matches("https://[a-z0-9-]+\\.supabase\\.co")||!p.optString("reader_key").matches("[0-9a-f]{64}"))throw new Exception();UUID.fromString(p.optString("connect_id"));
  String key=p.optString("public_key");boolean valid=key.startsWith("sb_publishable_");if(!valid&&key.startsWith("eyJ")){String[] parts=key.split("\\.");JSONObject jwt=new JSONObject(new String(android.util.Base64.decode(parts[1],android.util.Base64.URL_SAFE),StandardCharsets.UTF_8));valid="anon".equals(jwt.optString("role"));}if(!valid)throw new Exception();
  JSONObject clean=new JSONObject();for(String field:new String[]{"url","public_key","connect_id","reader_key","format"})clean.put(field,p.get(field));
  if("dubtimes-edit-v2".equals(p.optString("format")))clean.put("editor_key",p.getString("editor_key"));String encrypted=encrypt(clean.toString());generation++;pairing=clean;snapshot=null;new File(getFilesDir(),"snapshot.json").delete();getPreferences(0).edit().putString("pairing",encrypted).apply();notice=canEdit()?"Vinculado con permiso de edición":"Vinculado con permiso de lectura";render();refresh(true);
 }catch(Exception e){toast("Archivo inválido. Importa el archivo para Android generado por Connect, no la configuración de Windows.");}}
 private JSONArray outbox=new JSONArray();
 private final Map<String,String> groups=new LinkedHashMap<String,String>(){{put("project","Proyectos");put("version","Versiones");put("character","Personajes y asignaciones");put("student","Alumnos");put("collective","Participantes de colectivos / wallas");put("class","Clases y entregas");put("package","Paquetes de clases");put("teacher","Análisis del profesor");put("comment","Comentarios de alumnos");put("aspect","Comparaciones por aspecto");put("extras","Aportes adicionales");put("rubric","Argumentación y revisión");}};
 private boolean canEdit(){return pairing!=null&&pairing.optString("editor_key").matches("[0-9a-f]{64}");}
 private boolean sameWorkspace(JSONObject item){return pairing!=null&&pairing.optString("connect_id").equals(item.optString("connect_id"));}
 private int pendingCount(){int count=0;for(int i=0;i<outbox.length();i++){JSONObject q=outbox.optJSONObject(i);if(q!=null&&sameWorkspace(q)&&(q.optString("state").equals("local")||q.optString("state").equals("pending")))count++;}return count;}
 private void saveOutbox()throws Exception{for(int i=0;i<outbox.length();i++){JSONObject q=outbox.optJSONObject(i);if(q!=null&&(q.optString("state").equals("applied")||q.optString("state").equals("conflict")||q.optString("state").equals("error")))q.remove("edit");}if(outbox.toString().getBytes(StandardCharsets.UTF_8).length>7340032)throw new IOException("Hay demasiados cambios pendientes");File tmp=new File(getFilesDir(),"changes.tmp");try(FileOutputStream out=new FileOutputStream(tmp)){out.write(outbox.toString().getBytes(StandardCharsets.UTF_8));}if(!tmp.renameTo(new File(getFilesDir(),"changes.json")))throw new IOException();}
 private String fieldLabel(String key){switch(key){case "name":return "Nombre";case "student_name":return "Alumno del colectivo";case "actor":return "Alumno asignado (varios candidatos separados por /)";case "suggestion":return "Sugerencias";case "state":return "Estado";case "entries":return "Cantidad de entradas";case "class_number":return "Número de clase";case "project_name":return "Proyecto";case "character_name":return "Personaje";case "paid_status":return "Clase pagada";case "pending_minutes":return "Minutos pendientes";case "receipt_url":return "Enlace del comprobante";case "created_at":return "Fecha y hora (AAAA-MM-DD HH:MM:SS)";case "total":return "Clases del paquete";case "text":return "Texto";case "status":return "Estado de comparación";case "student":return "Cita del alumno";case "reason":return "Observación";case "level":return "Argumentación";case "reviewed":return "Revisado por el profesor";case "validated_extras":return "Aportes validados (uno existente por línea)";default:return key;}}
 private void editors(){
  if(!canEdit()){card("Dispositivo de consulta","Para editar, habilita el permiso desde Más > Android en Windows e importa el nuevo archivo de vinculación.",null);return;}
  JSONArray entities=arr(snapshot,"editables");if(entities.length()==0){card("Actualiza Windows","Esta copia todavía no incluye registros editables. Sincroniza la nueva versión de Windows.",null);return;}
  for(String kind:groups.keySet()){int n=0;for(int i=0;i<entities.length();i++){JSONObject e=entities.optJSONObject(i);if(e!=null&&kind.equals(e.optString("kind"))&&match(e.optString("label")))n++;}if(n>0){final String selected=kind;card(groups.get(kind),n+" registros para editar",()->chooseEntity(selected));}}
 }
 private void chooseEntity(String kind){JSONArray all=arr(snapshot,"editables");List<JSONObject> entries=new ArrayList<>();List<String> labels=new ArrayList<>();for(int i=0;i<all.length();i++){JSONObject e=all.optJSONObject(i);if(e!=null&&kind.equals(e.optString("kind"))&&match(e.optString("label"))){entries.add(e);labels.add(e.optString("label"));}}
  new AlertDialog.Builder(this).setTitle(groups.get(kind)).setItems(labels.toArray(new String[0]),(d,w)->editEntity(entries.get(w))).setNegativeButton("Cerrar",null).show();
 }
 private void editEntity(JSONObject entity){
  for(int i=0;i<outbox.length();i++){JSONObject q=outbox.optJSONObject(i);if(q==null||!sameWorkspace(q))continue;JSONObject e=q.optJSONObject("edit");if(e!=null&&e.optString("kind").equals(entity.optString("kind"))&&e.optString("id").equals(entity.optString("id"))&&(q.optString("state").equals("local")||q.optString("state").equals("pending"))){toast("Este registro ya tiene un cambio pendiente. Actualiza después de sincronizar Windows.");return;}}
  JSONObject base=entity.optJSONObject("fields");if(base==null)return;
  ScrollView scroll=new ScrollView(this);LinearLayout form=new LinearLayout(this);form.setOrientation(1);form.setPadding(dp(12),dp(4),dp(12),dp(4));scroll.addView(form);Map<String,View> controls=new LinkedHashMap<>();Map<String,List<Object>> options=new HashMap<>();
  Iterator<String> keys=base.keys();while(keys.hasNext()){String field=keys.next();form.addView(label(fieldLabel(field),13,MUTED,true));Object value=base.opt(field);View control;
   if(field.equals("state")||field.equals("status")||field.equals("level")){
    List<Object> values=new ArrayList<>();List<String> names=new ArrayList<>();
    if(field.equals("state")){String[] codes={"⚪","🟡","🟢","🔵","⛔"},labels={"Pendiente","En proceso","Terminado","Correcciones","Sin asignar"};for(int i=0;i<codes.length;i++){values.add(codes[i]);names.add(labels[i]);}}
    else if(field.equals("status")){for(String v:new String[]{"No mencionado","Coincide","Difiere","Mencionado sin valoración"}){values.add(v);names.add(v);}}
    else{for(int i=-1;i<4;i++){values.add(i);names.add(new String[]{"Sin evaluar","Sin fundamentar","Básica","Clara","Sólida"}[i+1]);}}
    Spinner spinner=new Spinner(this);spinner.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,names));for(int i=0;i<values.size();i++)if(String.valueOf(values.get(i)).equals(String.valueOf(value)))spinner.setSelection(i);options.put(field,values);control=spinner;
   }else if(field.equals("paid_status")||field.equals("reviewed")){CheckBox check=new CheckBox(this);check.setText("Sí");check.setTextColor(FG);check.setChecked(base.optInt(field)==1);control=check;}
   else{EditText input;
    if(field.equals("actor")||field.equals("student_name")){AutoCompleteTextView auto=new AutoCompleteTextView(this);List<String> names=new ArrayList<>();JSONArray students=arr(snapshot,"students");for(int i=0;i<students.length();i++)names.add(students.optJSONObject(i).optString("name"));auto.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_dropdown_item_1line,names));auto.setThreshold(1);input=auto;}
    else input=new EditText(this);
    input.setTextColor(FG);input.setText(value==JSONObject.NULL?"":String.valueOf(value));if(value instanceof Number)input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);else if(field.equals("text")||field.equals("reason")||field.equals("student")||field.equals("validated_extras")){input.setMinLines(3);input.setGravity(Gravity.TOP);input.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);}control=input;
   }
   controls.put(field,control);form.addView(control,new LinearLayout.LayoutParams(-1,-2));
  }
  AlertDialog dialog=new AlertDialog.Builder(this).setTitle(entity.optString("label")).setView(scroll).setPositiveButton("Guardar cambio",null).setNegativeButton("Cancelar",null).create();
  dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{try{
   JSONObject values=new JSONObject();for(String field:controls.keySet()){View control=controls.get(field);Object value;if(control instanceof Spinner)value=options.get(field).get(((Spinner)control).getSelectedItemPosition());else if(control instanceof CheckBox)value=((CheckBox)control).isChecked()?1:0;else{String raw=((EditText)control).getText().toString();value=base.opt(field) instanceof Number?Integer.parseInt(raw.trim()):raw;}
    if(!String.valueOf(value).equals(String.valueOf(base.opt(field))))values.put(field,value);
   }
   if(values.length()==0){dialog.dismiss();return;}
   JSONObject edit=new JSONObject();edit.put("kind",entity.getString("kind"));edit.put("id",entity.getString("id"));edit.put("base",new JSONObject(base.toString()));edit.put("values",values);
   JSONObject queued=new JSONObject();queued.put("edit_id",UUID.randomUUID().toString());queued.put("connect_id",pairing.getString("connect_id"));queued.put("edit",edit);queued.put("label",entity.optString("label"));queued.put("state","local");queued.put("message","Guardado en el teléfono; pendiente de enviar");outbox.put(queued);
   try{saveOutbox();}catch(Exception e){outbox.remove(outbox.length()-1);throw e;}
   dialog.dismiss();notice="Cambio guardado. Windows lo recibirá al sincronizar.";render();refresh(false);
  }catch(Exception e){toast("No se pudo guardar. Revisa los valores numéricos y vuelve a intentarlo.");}}));dialog.show();
 }
 private void changesDialog(){List<JSONObject> rows=new ArrayList<>();List<String> labels=new ArrayList<>();for(int i=outbox.length()-1;i>=0;i--){JSONObject q=outbox.optJSONObject(i);if(q==null||!sameWorkspace(q))continue;rows.add(q);String state=q.optString("state");String label=state.equals("applied")?"✓ Aplicado":state.equals("conflict")?"Conflicto":state.equals("error")?"Rechazado":state.equals("pending")?"Esperando Windows":"Pendiente de envío";labels.add(label+" · "+q.optString("label"));}
  if(rows.isEmpty()){toast("Todavía no hay cambios de este dispositivo.");return;}
  new AlertDialog.Builder(this).setTitle("Cambios de Android").setItems(labels.toArray(new String[0]),(d,w)->detail(labels.get(w),rows.get(w).optString("message"),"")).setNegativeButton("Cerrar",null).show();
 }
 private Object rpc(JSONObject p,String function,JSONObject args)throws Exception{
  URL url=new URL(p.getString("url")+"/rest/v1/rpc/"+function);HttpsURLConnection c=(HttpsURLConnection)url.openConnection();c.setInstanceFollowRedirects(false);c.setRequestMethod("POST");c.setConnectTimeout(20000);c.setReadTimeout(25000);c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");c.setRequestProperty("apikey",p.getString("public_key"));if(p.getString("public_key").startsWith("eyJ"))c.setRequestProperty("Authorization","Bearer "+p.getString("public_key"));
  try{try(OutputStream out=c.getOutputStream()){out.write(args.toString().getBytes(StandardCharsets.UTF_8));}if(c.getResponseCode()!=200)throw new IOException("HTTP "+c.getResponseCode());return new JSONTokener(readAll(c.getInputStream(),8388608)).nextValue();}finally{c.disconnect();}
 }
 private void refresh(boolean manual){
  if(pairing==null||busy){if(manual&&pairing==null)toast("Vincula primero el archivo de Windows.");return;}
  final JSONObject p=pairing;final long requestGeneration=generation;final JSONArray queued;
  try{queued=new JSONArray(outbox.toString());}catch(Exception e){toast("No se pudieron leer los cambios pendientes.");return;}
  busy=true;notice="Sincronizando…";render();
  executor.execute(()->{try{
   Map<String,JSONObject> updates=new HashMap<>();boolean failedSend=false;
   if(p.has("editor_key")){for(int i=0;i<queued.length();i++){
    JSONObject q=queued.optJSONObject(i);if(q==null||!q.optString("connect_id").equals(p.optString("connect_id"))||!q.optString("state").equals("local"))continue;
    try{JSONObject args=new JSONObject();args.put("connect_id",p.getString("connect_id"));args.put("editor_key",p.getString("editor_key"));args.put("edit_id",q.getString("edit_id"));args.put("edit",q.getJSONObject("edit"));JSONObject response=(JSONObject)rpc(p,"connect_submit_edit",args);updates.put(q.getString("edit_id"),response);}catch(Exception e){failedSend=true;}
   }}
   JSONObject readArgs=new JSONObject();readArgs.put("connect_id",p.getString("connect_id"));readArgs.put("reader_key",p.getString("reader_key"));
   JSONObject response=(JSONObject)rpc(p,"connect_read_snapshot",readArgs);JSONObject payload=response.optJSONObject("payload");if(payload==null)throw new IOException("Windows todavía no subió una copia");validateSnapshot(payload);
   try{JSONArray statuses=(JSONArray)rpc(p,"connect_edit_status",readArgs);for(int i=0;i<statuses.length();i++){JSONObject item=statuses.getJSONObject(i);updates.put(item.getString("edit_id"),item);}}catch(Exception e){/* Proyectos antiguos de solo lectura aún no tienen esta función. */}
   final boolean notSent=failedSend;
   handler.post(()->{busy=false;if(requestGeneration!=generation||isFinishing()||isDestroyed())return;try{
    for(int i=0;i<outbox.length();i++){JSONObject q=outbox.optJSONObject(i);if(q==null||!sameWorkspace(q))continue;JSONObject u=updates.get(q.optString("edit_id"));if(u!=null){q.put("state",u.optString("state","pending"));q.put("message",u.optString("message","Esperando sincronización de Windows"));}}
    saveOutbox();File tmp=new File(getFilesDir(),"snapshot.tmp"),dest=new File(getFilesDir(),"snapshot.json");try(FileOutputStream out=new FileOutputStream(tmp)){out.write(payload.toString().getBytes(StandardCharsets.UTF_8));}if(!tmp.renameTo(dest))throw new IOException();snapshot=payload;notice=notSent?"Hay cambios sin enviar. Revisa la vinculación de edición y vuelve a actualizar.":"Sincronizado";
   }catch(Exception e){notice="No se pudo guardar la copia local. Los cambios pendientes se conservan.";}render();});
  }catch(Exception e){handler.post(()->{busy=false;if(requestGeneration!=generation||isFinishing()||isDestroyed())return;notice="No se pudo sincronizar. Se conserva la última copia y los cambios pendientes.";render();});}});
 }
 private void validateSnapshot(JSONObject p)throws Exception{if(p.optInt("schema_version")!=1)throw new Exception();for(String key:new String[]{"students","projects","classes","activities","learning"})if(p.optJSONArray(key)==null)throw new Exception();}
 private String readAll(InputStream stream,int max)throws IOException{if(stream==null)throw new IOException();try(InputStream in=stream;ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(out.size()+n>max)throw new IOException("Demasiado grande");out.write(b,0,n);}return out.toString("UTF-8");}}
 private javax.crypto.SecretKey key()throws Exception{KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);String alias="DubTimesConnectReader";if(!ks.containsAlias(alias)){KeyGenerator generator=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");generator.init(new KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());generator.generateKey();}return (javax.crypto.SecretKey)ks.getKey(alias,null);}
 private String encrypt(String value)throws Exception{Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key());return android.util.Base64.encodeToString(cipher.getIV(),2)+":"+android.util.Base64.encodeToString(cipher.doFinal(value.getBytes(StandardCharsets.UTF_8)),2);}
 private String decrypt(String value)throws Exception{String[] parts=value.split(":");Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,android.util.Base64.decode(parts[0],2)));return new String(cipher.doFinal(android.util.Base64.decode(parts[1],2)),StandardCharsets.UTF_8);}
 private void toast(String text){Toast.makeText(this,text,Toast.LENGTH_LONG).show();}
}