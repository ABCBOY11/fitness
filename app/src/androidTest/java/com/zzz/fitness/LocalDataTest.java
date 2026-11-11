package com.zzz.fitness;

import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class LocalDataTest {
    @Test public void cleanupPreservesOlderRecordsPlansAndTasksAndPersistence() {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        // Isolated database path so this test never deletes the user's real records.
        Context isolated=new android.content.ContextWrapper(context){
            @Override public java.io.File getDatabasePath(String name){return new java.io.File(context.getCacheDir(),"test-"+name);}
            @Override public android.database.sqlite.SQLiteDatabase openOrCreateDatabase(String name,int mode,android.database.sqlite.SQLiteDatabase.CursorFactory factory){return android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name),factory);}
            @Override public android.database.sqlite.SQLiteDatabase openOrCreateDatabase(String name,int mode,android.database.sqlite.SQLiteDatabase.CursorFactory factory,android.database.DatabaseErrorHandler handler){return android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name).getPath(),factory,handler);}
        };
        java.io.File file=isolated.getDatabasePath("fitness.db");file.delete();
        FitnessDb db=new FitnessDb(isolated);
        try {
            try(android.database.Cursor pragma=db.getReadableDatabase().rawQuery("PRAGMA secure_delete",null)){
                assertTrue(pragma.moveToFirst());assertEquals(1,pragma.getInt(0));
            }
            FitnessDb.Row old=new FitnessDb.Row();old.day="2026-10-01";old.kind="diet";old.title="早餐";db.saveRecord(old);
            FitnessDb.Row recent=new FitnessDb.Row();recent.day="2026-11-11";recent.kind="metric";recent.title="体重";recent.value=70.5;db.saveRecord(recent);
            db.addTask("跑步","training","135");db.addPlan("计划.pdf","/test.pdf","test");
            // Simulate the previously shipped v1 schema and upgrade it in place.
            db.getWritableDatabase().execSQL("DROP TABLE training_days");
            db.getWritableDatabase().execSQL("DROP INDEX session_day");
            db.getWritableDatabase().setVersion(1);
            db.close();db=new FitnessDb(isolated);
            assertEquals(2,db.records("1970-01-01","2026-11-11").size());
            FitnessDb.Row template=new FitnessDb.Row();template.title="力量 A";template.note="测试项目 | 3组8次";db.saveTrainingDay(template);
            FitnessDb.Row session=new FitnessDb.Row();session.day="2026-11-11";session.kind="session";session.title="力量 A";session.note="[{\"title\":\"测试项目 | 3组8次\",\"done\":true}]";db.saveRecord(session);
            template=db.trainingDays().get(0);template.note="新计划";db.saveTrainingDay(template);
            assertTrue(db.records("2026-11-11","2026-11-11").get(0).note.contains("3组8次"));
            assertEquals(2,db.clearRecords("2026-11-11","2026-11-11"));
            assertEquals("早餐",db.records("1970-01-01","2026-11-11").get(0).title);
            assertEquals(1,db.tasks().size());assertEquals(1,db.plans().size());
            assertEquals(1,db.trainingDays().size());
        } finally {db.close();file.delete();}
    }
}
