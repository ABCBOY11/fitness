package com.zzz.fitness;
import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import java.util.*;
public final class FitnessDb extends SQLiteOpenHelper {
    public static final class Row {
        public long id,taskId;
        public String day="",kind="",title="",note="",path="",days="";
        public double value,extra,third;
    }
    public FitnessDb(Context c){super(c,"fitness.db",null,2);}
    @Override public void onConfigure(SQLiteDatabase db){
        // This PRAGMA returns a row, even when assigning the setting. execSQL()
        // rejects result-bearing statements on Android, crashing the first open.
        try(Cursor result=db.rawQuery("PRAGMA secure_delete=ON",null)){
            result.moveToFirst(); // Cursors execute lazily; consume the result.
        }
    }
    @Override public void onCreate(SQLiteDatabase db){
        db.execSQL("CREATE TABLE records(id INTEGER PRIMARY KEY,day TEXT NOT NULL,kind TEXT NOT NULL,title TEXT NOT NULL,note TEXT NOT NULL DEFAULT '',value REAL DEFAULT 0,extra REAL DEFAULT 0,third REAL DEFAULT 0,taskId INTEGER DEFAULT 0)");
        db.execSQL("CREATE INDEX records_day ON records(day)");
        db.execSQL("CREATE UNIQUE INDEX task_check ON records(day,taskId) WHERE taskId>0");
        db.execSQL("CREATE UNIQUE INDEX metrics_day ON records(day) WHERE kind='metric'");
        db.execSQL("CREATE TABLE tasks(id INTEGER PRIMARY KEY AUTOINCREMENT,title TEXT NOT NULL,kind TEXT NOT NULL,days TEXT NOT NULL)");
        db.execSQL("CREATE TABLE plans(id INTEGER PRIMARY KEY,title TEXT NOT NULL,path TEXT NOT NULL,note TEXT NOT NULL)");
        createTrainingTables(db);
    }
    private void createTrainingTables(SQLiteDatabase db){
        db.execSQL("CREATE TABLE training_days(id INTEGER PRIMARY KEY AUTOINCREMENT,title TEXT NOT NULL,body TEXT NOT NULL)");
        db.execSQL("CREATE UNIQUE INDEX session_day ON records(day) WHERE kind='session'");
    }
    @Override public void onUpgrade(SQLiteDatabase db,int a,int b){if(a<2)createTrainingTables(db);}
    public List<Row> trainingDays(){
        List<Row> list=new ArrayList<>();try(Cursor c=getReadableDatabase().rawQuery("SELECT id,title,body FROM training_days ORDER BY id",null)){
            while(c.moveToNext()){Row r=new Row();r.id=c.getLong(0);r.title=c.getString(1);r.note=c.getString(2);list.add(r);}
        }return list;
    }
    public void saveTrainingDay(Row r){ContentValues v=new ContentValues();v.put("title",r.title);v.put("body",r.note);if(r.id>0)getWritableDatabase().update("training_days",v,"id=?",new String[]{""+r.id});else getWritableDatabase().insertOrThrow("training_days",null,v);}
    public void deleteTrainingDay(long id){getWritableDatabase().delete("training_days","id=?",new String[]{""+id});}
    public void importTrainingDays(List<TrainingPlan.Day> days){SQLiteDatabase d=getWritableDatabase();d.beginTransaction();try{for(TrainingPlan.Day day:days){Row r=new Row();r.title=day.name;r.note=String.join("\n",day.items);saveTrainingDay(r);}d.setTransactionSuccessful();}finally{d.endTransaction();}}
    public List<Row> records(String start,String end){
        List<Row> rows=new ArrayList<>();
        try(Cursor c=getReadableDatabase().rawQuery("SELECT * FROM records WHERE day>=? AND day<=? ORDER BY day DESC,id DESC",new String[]{start,end})){
            while(c.moveToNext()){Row r=new Row();r.id=c.getLong(0);r.day=c.getString(1);r.kind=c.getString(2);r.title=c.getString(3);r.note=c.getString(4);r.value=c.getDouble(5);r.extra=c.getDouble(6);r.third=c.getDouble(7);r.taskId=c.getLong(8);rows.add(r);}
        }return rows;
    }
    public List<Row> tasks(){return simple("tasks");}
    public List<Row> plans(){return simple("plans");}
    private List<Row> simple(String table){
        List<Row> rows=new ArrayList<>();
        try(Cursor c=getReadableDatabase().rawQuery("SELECT * FROM "+table+" ORDER BY id DESC",null)){
            while(c.moveToNext()){Row r=new Row();r.id=c.getLong(0);r.title=c.getString(1);if(table.equals("tasks")){r.kind=c.getString(2);r.days=c.getString(3);}else{r.path=c.getString(2);r.note=c.getString(3);}rows.add(r);}
        }return rows;
    }
    public void saveRecord(Row r){
        ContentValues v=new ContentValues();v.put("day",r.day);v.put("kind",r.kind);v.put("title",r.title);v.put("note",r.note);v.put("value",r.value);v.put("extra",r.extra);v.put("third",r.third);v.put("taskId",r.taskId);
        if(r.id>0)getWritableDatabase().update("records",v,"id=?",new String[]{""+r.id});else getWritableDatabase().insertOrThrow("records",null,v);
    }
    public void addTask(String title,String kind,String days){ContentValues v=new ContentValues();v.put("title",title);v.put("kind",kind);v.put("days",days);getWritableDatabase().insertOrThrow("tasks",null,v);}
    public void updateTask(long id,String title,String kind,String days){ContentValues v=new ContentValues();v.put("title",title);v.put("kind",kind);v.put("days",days);getWritableDatabase().update("tasks",v,"id=?",new String[]{""+id});}
    public void addTasks(List<PlanParser.Suggestion> list){SQLiteDatabase d=getWritableDatabase();d.beginTransaction();try{for(PlanParser.Suggestion s:list)addTask(s.title,s.kind,"1234567");d.setTransactionSuccessful();}finally{d.endTransaction();}}
    public void addPlan(String title,String path,String note){ContentValues v=new ContentValues();v.put("title",title);v.put("path",path);v.put("note",note);getWritableDatabase().insertOrThrow("plans",null,v);}
    public void deleteRecord(long id){getWritableDatabase().delete("records","id=?",new String[]{""+id});}
    public void deleteTask(long id){getWritableDatabase().delete("tasks","id=?",new String[]{""+id});}
    public void deletePlan(long id){getWritableDatabase().delete("plans","id=?",new String[]{""+id});}
    public int clearRecords(String start,String end){int n=getWritableDatabase().delete("records","day>=? AND day<=?",new String[]{start,end});getWritableDatabase().execSQL("VACUUM");return n;}
}
