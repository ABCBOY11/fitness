package com.zzz.fitness;

import android.app.*;
import android.content.*;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.view.*;
import android.widget.*;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.io.*;
import java.lang.reflect.*;
import java.time.LocalDate;
import java.util.*;
import org.json.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class TrainingLayoutTest {
    private final Instrumentation ins=InstrumentationRegistry.getInstrumentation();
    private TextView text(View v,String s){if(v instanceof TextView&&s.contentEquals(((TextView)v).getText()))return (TextView)v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){TextView found=text(((ViewGroup)v).getChildAt(i),s);if(found!=null)return found;}return null;}
    private View description(View v,String s){if(s.contentEquals(v.getContentDescription()==null?"":v.getContentDescription()))return v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){View found=description(((ViewGroup)v).getChildAt(i),s);if(found!=null)return found;}return null;}
    private void capture(String name)throws Exception{ins.waitForIdleSync();SystemClock.sleep(450);try(android.os.ParcelFileDescriptor fd=ins.getUiAutomation().executeShellCommand("screencap -p /data/local/tmp/"+name);InputStream in=new android.os.ParcelFileDescriptor.AutoCloseInputStream(fd)){byte[] buffer=new byte[1024];while(in.read(buffer)!=-1){}}}
    @Test public void independentCardsEditOrderAndPreserveSnapshot()throws Exception{
        Context target=ins.getTargetContext();Context isolated=new ContextWrapper(target){
            @Override public File getDatabasePath(String n){return new File(target.getCacheDir(),"training-layout-test.db");}
            @Override public SQLiteDatabase openOrCreateDatabase(String n,int m,SQLiteDatabase.CursorFactory f){return SQLiteDatabase.openOrCreateDatabase(getDatabasePath(n),f);}
            @Override public SQLiteDatabase openOrCreateDatabase(String n,int m,SQLiteDatabase.CursorFactory f,android.database.DatabaseErrorHandler h){return SQLiteDatabase.openOrCreateDatabase(getDatabasePath(n).getPath(),f,h);}
        };
        isolated.getDatabasePath("fitness.db").delete();FitnessDb testDb=new FitnessDb(isolated);
        FitnessDb.Row day=new FitnessDb.Row();day.title="肩腿塑形 · 力量 A";day.note="哑铃高脚杯深蹲 | 4 组 × 10–12 次，组间休息 90 秒\n保加利亚分腿蹲 | 每侧 3 组 × 10 次\n哑铃侧平举 | 3 组 × 15 次\n平板支撑 | 3 组 × 45 秒";testDb.saveTrainingDay(day);day=testDb.trainingDays().get(0);
        FitnessDb.Row session=new FitnessDb.Row();session.kind="session";session.day=LocalDate.now().toString();session.title=day.title;JSONArray items=new JSONArray();for(String title:day.note.split("\n")){JSONObject o=new JSONObject();o.put("title",title);o.put("done",false);o.put("actual","");items.put(o);}session.note=items.toString();testDb.saveRecord(session);
        Activity a=ins.startActivitySync(new Intent(target,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        Field db=MainActivity.class.getDeclaredField("db");db.setAccessible(true);Field content=MainActivity.class.getDeclaredField("content");content.setAccessible(true);Method show=MainActivity.class.getDeclaredMethod("show");show.setAccessible(true);
        Method editor=MainActivity.class.getDeclaredMethod("trainingDayEditor",FitnessDb.Row.class);editor.setAccessible(true);Field sheets=MainActivity.class.getDeclaredField("formSheets");sheets.setAccessible(true);
        final FitnessDb.Row template=day;
        try{
            ins.runOnMainSync(()->{try{a.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);((FitnessDb)db.get(a)).close();db.set(a,testDb);show.invoke(a);}catch(Exception e){throw new RuntimeException(e);}});
            ins.waitForIdleSync();SystemClock.sleep(300);
            ins.runOnMainSync(()->{try{LinearLayout c=(LinearLayout)content.get(a);ScrollView scroll=(ScrollView)c.getParent();View title=text(c,template.title);scroll.scrollTo(0,((View)title.getParent()).getTop()-24);}catch(Exception e){throw new RuntimeException(e);}});
            capture("ui-training-cards.png");
            int[] position=new int[1];ins.runOnMainSync(()->{try{LinearLayout c=(LinearLayout)content.get(a);position[0]=((ScrollView)c.getParent()).getScrollY();View check=description(c,"完成 "+items.optJSONObject(0).optString("title"));assertTrue(check.getWidth()>=48*a.getResources().getDisplayMetrics().density);check.performClick();}catch(Exception e){throw new RuntimeException(e);}});ins.waitForIdleSync();
            ins.runOnMainSync(()->{try{
                LinearLayout original=(LinearLayout)content.get(a);ScrollView scroll=(ScrollView)original.getParent();
                View check=description(original,"完成 "+items.optJSONObject(0).optString("title"));
                for(int i=0;i<20;i++){check.performClick();assertSame("Completion must keep the existing page",original,content.get(a));assertSame(check,description(original,"完成 "+items.optJSONObject(0).optString("title")));assertEquals(position[0],scroll.getScrollY());}
                assertNotNull(text(original,"1 / 4"));
                text(original,"一键完成本日所有项目").performClick();assertSame(original,content.get(a));assertNotNull(text(original,"4 / 4"));assertEquals(position[0],scroll.getScrollY());
                for(int i=1;i<4;i++)description(original,"完成 "+items.optJSONObject(i).optString("title")).performClick();
                assertNotNull(text(original,"1 / 4"));assertEquals(position[0],scroll.getScrollY());
                editor.invoke(a,template);
            }catch(Exception e){throw new RuntimeException(e);}});
            capture("ui-training-editor.png");
            ins.runOnMainSync(()->{try{
                FormDialog dialog=(FormDialog)((List<?>)sheets.get(a)).get(0);View root=dialog.getWindow().getDecorView();TrainingDayForm f=findForm(root);assertEquals(template.note,f.body());
                text(root,"↑");description(root,"下移项目 1").performClick();assertTrue(f.body().startsWith("保加利亚分腿蹲"));
                ((EditText)description(root,"项目 1 动作名称")).setText("分腿蹲（修改）");
                text(root,"删除").performClick();assertEquals(3,f.body().split("\n").length);
                text(root,"＋ 添加项目").performClick();try{f.body();fail("Empty new exercise must be validated");}catch(IllegalArgumentException expected){}
                ((EditText)description(root,"项目 4 动作名称")).setText("拉伸");((EditText)description(root,"项目 4 计划要求")).setText("5 分钟");
                dialog.getButton(-1).performClick();assertFalse(dialog.isShowing());assertTrue(testDb.trainingDays().get(0).note.endsWith("拉伸 | 5 分钟"));
                FitnessDb.Row saved=testDb.records(session.day,session.day).get(0);JSONArray snapshot=new JSONArray(saved.note);assertTrue(snapshot.getJSONObject(0).getBoolean("done"));assertEquals(items.getJSONObject(0).getString("title"),snapshot.getJSONObject(0).getString("title"));
                editor.invoke(a,testDb.trainingDays().get(0));FormDialog cancel=(FormDialog)((List<?>)sheets.get(a)).get(0);((EditText)description(cancel.getWindow().getDecorView(),"项目 1 动作名称")).setText("未保存");cancel.getButton(-2).performClick();assertFalse(testDb.trainingDays().get(0).note.contains("未保存"));
            }catch(Exception e){throw new RuntimeException(e);}});
        }finally{ins.runOnMainSync(a::finish);ins.waitForIdleSync();}
    }
    private TrainingDayForm findForm(View v){if(v instanceof TrainingDayForm)return (TrainingDayForm)v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){TrainingDayForm f=findForm(((ViewGroup)v).getChildAt(i));if(f!=null)return f;}return null;}
}
