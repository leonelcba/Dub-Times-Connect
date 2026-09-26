package com.dubtimes.connect;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import java.util.*;

public class AppDb extends SQLiteOpenHelper {
    public static final String DB_NAME="dub_times_connect_android.db";
    public AppDb(Context c){ super(c,DB_NAME,null,1); }
    @Override public void onCreate(SQLiteDatabase db){
        db.execSQL("CREATE TABLE projects(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL UNIQUE COLLATE NOCASE)");
        db.execSQL("CREATE TABLE students(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL UNIQUE COLLATE NOCASE)");
        db.execSQL("CREATE TABLE characters(id INTEGER PRIMARY KEY AUTOINCREMENT,project_id INTEGER NOT NULL,name TEXT NOT NULL,student_id INTEGER,state TEXT NOT NULL DEFAULT 'Pendiente',collective INTEGER NOT NULL DEFAULT 0,FOREIGN KEY(project_id) REFERENCES projects(id),FOREIGN KEY(student_id) REFERENCES students(id))");
        db.execSQL("CREATE TABLE classes(id INTEGER PRIMARY KEY AUTOINCREMENT,student_id INTEGER NOT NULL,project_id INTEGER NOT NULL,character_id INTEGER,number INTEGER NOT NULL,total INTEGER NOT NULL,pending INTEGER NOT NULL DEFAULT 0,paid INTEGER NOT NULL DEFAULT 1,receipt TEXT DEFAULT '',created_at INTEGER NOT NULL)");
    }
    @Override public void onUpgrade(SQLiteDatabase db,int oldV,int newV){}
    public long addProject(String name){ ContentValues v=new ContentValues();v.put("name",name.trim());return getWritableDatabase().insertOrThrow("projects",null,v); }
    public long addStudent(String name){ ContentValues v=new ContentValues();v.put("name",name.trim());return getWritableDatabase().insertOrThrow("students",null,v); }
    public long addCharacter(long projectId,String name,Long studentId,String state,boolean collective){ ContentValues v=new ContentValues();v.put("project_id",projectId);v.put("name",name.trim());if(studentId==null)v.putNull("student_id");else v.put("student_id",studentId);v.put("state",state);v.put("collective",collective?1:0);return getWritableDatabase().insert("characters",null,v); }
    public List<Row> projects(){ return rows("SELECT id,name FROM projects ORDER BY name COLLATE NOCASE",null); }
    public List<Row> students(){ return rows("SELECT id,name FROM students ORDER BY name COLLATE NOCASE",null); }
    public List<Row> studentsForProject(long pid){ return rows("SELECT DISTINCT s.id,s.name FROM students s JOIN characters c ON c.student_id=s.id WHERE c.project_id=? ORDER BY s.name COLLATE NOCASE",new String[]{String.valueOf(pid)}); }
    public List<Row> characters(long pid){ return rows("SELECT c.id,c.name,c.state,c.collective,c.student_id,COALESCE(s.name,'Sin asignar') student_name FROM characters c LEFT JOIN students s ON s.id=c.student_id WHERE c.project_id=? ORDER BY c.collective,c.name COLLATE NOCASE",new String[]{String.valueOf(pid)}); }
    public List<Row> charactersForStudent(long pid,long sid){ return rows("SELECT c.id,c.name,c.state,c.collective FROM characters c WHERE c.project_id=? AND c.student_id=? ORDER BY c.collective,c.name COLLATE NOCASE",new String[]{String.valueOf(pid),String.valueOf(sid)}); }
    public Row latestClass(long pid,long sid){ List<Row> r=rows("SELECT number,total,pending,paid,receipt FROM classes WHERE project_id=? AND student_id=? ORDER BY created_at DESC,id DESC LIMIT 1",new String[]{String.valueOf(pid),String.valueOf(sid)});return r.isEmpty()?null:r.get(0); }
    public long addClass(long pid,long sid,Long cid,int number,int total,int pending,boolean paid,String receipt){ ContentValues v=new ContentValues();v.put("project_id",pid);v.put("student_id",sid);if(cid==null)v.putNull("character_id");else v.put("character_id",cid);v.put("number",number);v.put("total",total);v.put("pending",pending);v.put("paid",paid?1:0);v.put("receipt",receipt==null?"":receipt.trim());v.put("created_at",System.currentTimeMillis());return getWritableDatabase().insert("classes",null,v); }
    private List<Row> rows(String sql,String[] args){ ArrayList<Row> out=new ArrayList<>();try(Cursor c=getReadableDatabase().rawQuery(sql,args)){while(c.moveToNext()){Row r=new Row();for(int i=0;i<c.getColumnCount();i++){String n=c.getColumnName(i);int t=c.getType(i);if(t==Cursor.FIELD_TYPE_INTEGER)r.put(n,c.getLong(i));else r.put(n,c.getString(i));}out.add(r);}}return out; }
    public static class Row extends HashMap<String,Object>{ public long l(String k){Object v=get(k);return v instanceof Number?((Number)v).longValue():0;} public String s(String k){Object v=get(k);return v==null?"":String.valueOf(v);} }
}
