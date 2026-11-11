package com.zzz.fitness;
import android.app.*;
import android.content.*;
import android.database.sqlite.SQLiteDatabase;
import android.view.*;
import android.widget.TextView;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.io.File;
import java.lang.reflect.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.json.JSONArray;
import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static androidx.test.espresso.action.ViewActions.*;

@RunWith(AndroidJUnit4.class)
public class DaySelectionTest {
    @Test public void chooseDayCompleteAllAndRecordBreakfastInIsolatedDatabase()throws Exception{
        Instrumentation ins=InstrumentationRegistry.getInstrumentation();Context target=ins.getTargetContext();
        Context isolated=new ContextWrapper(target){
            @Override public File getDatabasePath(String name){return new File(target.getCacheDir(),"day-ui-test.db");}
            @Override public SQLiteDatabase openOrCreateDatabase(String n,int m,SQLiteDatabase.CursorFactory f){return SQLiteDatabase.openOrCreateDatabase(getDatabasePath(n),f);}
            @Override public SQLiteDatabase openOrCreateDatabase(String n,int m,SQLiteDatabase.CursorFactory f,android.database.DatabaseErrorHandler h){return SQLiteDatabase.openOrCreateDatabase(getDatabasePath(n).getPath(),f,h);}
        };
        File file=isolated.getDatabasePath("fitness.db");file.delete();FitnessDb testDb=new FitnessDb(isolated);
        FitnessDb.Row day=new FitnessDb.Row();day.title="测试力量 A";day.note="测试项目 1 | 3组8次\n测试项目 2 | 2组10次";testDb.saveTrainingDay(day);
        Activity activity=ins.startActivitySync(new Intent(target,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        Field db=MainActivity.class.getDeclaredField("db");db.setAccessible(true);Method show=MainActivity.class.getDeclaredMethod("show");show.setAccessible(true);
        ins.runOnMainSync(()->{try{((FitnessDb)db.get(activity)).close();db.set(activity,testDb);show.invoke(activity);}catch(Exception e){throw new RuntimeException(e);}});
        try{
            onView(withText("选择今日训练日  ↓")).perform(scrollTo(),click());
            onView(withText("测试力量 A")).perform(click());
            onView(withText("一键完成本日所有项目")).perform(scrollTo(),click());
            FitnessDb.Row session=testDb.records("1970-01-01","9999-12-31").get(0);assertEquals("session",session.kind);
            JSONArray items=new JSONArray(session.note);assertEquals(2,items.length());assertTrue(items.getJSONObject(0).getBoolean("done"));assertTrue(items.getJSONObject(1).getBoolean("done"));
            onView(withText("早餐  ＋")).perform(scrollTo(),click());
            onView(withContentDescription("吃了什么及份量（必填）")).perform(replaceText("测试早餐 一份"));
            onView(withText("保存")).perform(click());
            assertEquals(2,testDb.records("1970-01-01","9999-12-31").size());
        }finally{ins.runOnMainSync(activity::finish);ins.waitForIdleSync();}
    }
}
