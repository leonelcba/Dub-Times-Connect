package com.dubtimes.connect;

import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;

public class DiscordSync {
    static final String API="https://discord.com/api/v10";
    final String token,guildId,forumId;
    public DiscordSync(String token,String guildId,String forumId){this.token=token.trim();this.guildId=guildId.trim();this.forumId=forumId.trim();}

    String get(String path) throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL(API+path).openConnection();
        c.setRequestMethod("GET");c.setConnectTimeout(15000);c.setReadTimeout(20000);
        c.setRequestProperty("Authorization","Bot "+token);c.setRequestProperty("User-Agent","DubTimesConnectAndroid/0.4");
        int code=c.getResponseCode();InputStream in=code>=200&&code<300?c.getInputStream():c.getErrorStream();
        String text=read(in);if(code<200||code>=300)throw new IOException("Discord HTTP "+code+(text.isEmpty()?"":" · "+text));return text;
    }
    String read(InputStream in)throws Exception{if(in==null)return "";ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] x=new byte[8192];int n;while((n=in.read(x))>0)b.write(x,0,n);return b.toString("UTF-8");}

    public List<ProjectData> downloadProjects() throws Exception{
        if(token.isEmpty()||guildId.isEmpty()||forumId.isEmpty())throw new IllegalArgumentException("Falta token, servidor o foro.");
        // Validación rápida del token.
        get("/users/@me");
        LinkedHashMap<String,JSONObject> threads=new LinkedHashMap<>();
        JSONObject active=new JSONObject(get("/guilds/"+guildId+"/threads/active"));
        addThreads(threads,active.optJSONArray("threads"));
        String before=null;
        do{
            String path="/channels/"+forumId+"/threads/archived/public?limit=100"+(before==null?"":"&before="+URLEncoder.encode(before,"UTF-8"));
            JSONObject page=new JSONObject(get(path));JSONArray a=page.optJSONArray("threads");addThreads(threads,a);
            if(!page.optBoolean("has_more",false)||a==null||a.length()==0)break;
            JSONObject meta=a.getJSONObject(a.length()-1).optJSONObject("thread_metadata");before=meta==null?"":meta.optString("archive_timestamp","");
            if(before.isEmpty())break;
        }while(true);
        ArrayList<ProjectData> out=new ArrayList<>();
        for(JSONObject t:threads.values()){
            if(!forumId.equals(t.optString("parent_id")))continue;
            String id=t.optString("id"),name=t.optString("name","(sin nombre)");
            JSONArray msgs=new JSONArray(get("/channels/"+id+"/messages?limit=100"));
            ProjectData p=parseProject(id,name,msgs);if(!p.characters.isEmpty())out.add(p);
        }
        Collections.sort(out,(a,b)->a.name.compareToIgnoreCase(b.name));return out;
    }
    void addThreads(Map<String,JSONObject> out,JSONArray a)throws Exception{if(a==null)return;for(int i=0;i<a.length();i++){JSONObject t=a.getJSONObject(i);out.put(t.optString("id"),t);}}

    static final Pattern VERSION=Pattern.compile("^\\s*(?:[0-9\\uFE0F\\u20E3️⃣]+\\s*)?VERSI[ÓO]N\\s*(.+?)\\s*$",Pattern.CASE_INSENSITIVE);
    static final Pattern CHAR=Pattern.compile("^\\s*(🟢|🟡|🔵|⚪|⛔)\\s*(.+?)(?:\\s+\\(([^()]+)\\))?\\s*:\\s*(.*?)(?:\\s+[—-]\\s+(\\d+)\\s+entradas?)?\\s*$",Pattern.CASE_INSENSITIVE);
    ProjectData parseProject(String threadId,String name,JSONArray msgs)throws Exception{
        ProjectData p=new ProjectData();p.threadId=threadId;p.name=name;String version="VERSION 1";
        // API devuelve más nuevo primero: invertimos para mantener orden visual del post.
        for(int mi=msgs.length()-1;mi>=0;mi--){String content=msgs.getJSONObject(mi).optString("content","");for(String raw:content.split("\\r?\\n")){
            String line=raw.trim();if(line.isEmpty())continue;String plain=line.replace("**","").trim();Matcher vm=VERSION.matcher(plain);if(vm.matches()){String s=vm.group(1).trim().replaceFirst("^[^\\p{L}\\p{N}]+","");if(s.isEmpty())s="1";version=("VERSION "+s).toUpperCase(Locale.ROOT);continue;}
            Matcher m=CHAR.matcher(plain);if(!m.matches())continue;CharacterData c=new CharacterData();c.version=version;c.state=stateName(m.group(1));c.name=m.group(2).trim();c.suggestion=m.group(3)==null?"":m.group(3).trim();String actor=m.group(4)==null?"":m.group(4).trim();c.entries=m.group(5)==null?0:Integer.parseInt(m.group(5));
            c.collective=isCollective(c.name,actor);if(!actor.equalsIgnoreCase("Sin asignar")){if(c.collective){for(String part:actor.split(",")){String q=part.trim().replaceFirst("^(🟢|🟡|🔵|⚪|⛔)\\s*","").trim();if(!q.isEmpty()&&!q.equalsIgnoreCase("Sin alumnos"))c.students.add(q);}}else{for(String part:actor.split("/")){String q=part.trim();if(!q.isEmpty())c.students.add(q);}}}p.characters.add(c);
        }}return p;
    }
    static boolean isCollective(String name,String actor){String n=name.trim().toLowerCase(Locale.ROOT);return n.equals("walla")||n.equals("wallas")||n.equals("todos")||n.equals("todas")||n.equals("gente")||actor.contains(",");}
    static String stateName(String e){if("🟢".equals(e))return "Terminado";if("🟡".equals(e))return "En proceso";if("🔵".equals(e))return "Correcciones";if("⛔".equals(e))return "Sin asignar";return "Pendiente";}
    public static class ProjectData{public String name,threadId;public final List<CharacterData> characters=new ArrayList<>();}
    public static class CharacterData{public String version,name,state,suggestion;public int entries;public boolean collective;public final List<String> students=new ArrayList<>();}
}
