package com.zzz.fitness;
import android.app.*;
import android.content.*;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.io.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class)
public class FormUiTest {
    private final Instrumentation ins=InstrumentationRegistry.getInstrumentation();
    private Activity start(){return ins.startActivitySync(new Intent(ins.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}
    private void capture(String name)throws Exception{ins.waitForIdleSync();SystemClock.sleep(350);Bitmap b=ins.getUiAutomation().takeScreenshot();assertNotNull(b);try(OutputStream out=new FileOutputStream(new File(ins.getTargetContext().getCacheDir(),name))){b.compress(Bitmap.CompressFormat.PNG,100,out);}b.recycle();}
    @Test public void formShowsInlineErrorAndSupportsKeyboard()throws Exception{
        Activity activity=start();AtomicReference<FormDialog> dialog=new AtomicReference<>();AtomicReference<FormInput> input=new AtomicReference<>();
        try{
            ins.runOnMainSync(()->{LinearLayout f=new LinearLayout(activity);f.setOrientation(LinearLayout.VERTICAL);f.setPadding(48,32,48,8);FormInput weight=FormInput.addTo(f,"体重 kg（必填）","",true);FormInput.addTo(f,"体脂率 %（选填）","",true);FormInput.addTo(f,"腰围 cm（选填）","",true);FormInput.addTo(f,"备注","",false);FormDialog d=new FormDialog(activity,"身体指标",f);dialog.set(d);input.set(weight);d.show();d.getButton(-1).setOnClickListener(v->weight.setError("请填写 20 至 500 之间的体重"));d.getButton(-1).performClick();assertTrue(d.isShowing());assertNotNull(weight.getError());});
            capture("ui-form.png");
            ins.runOnMainSync(()->{input.get().setText("68.5");assertNull(input.get().getError());input.get().requestFocus();InputMethodManager ime=(InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE);ime.showSoftInput(input.get(),InputMethodManager.SHOW_IMPLICIT);});
            SystemClock.sleep(600);capture("ui-form-keyboard.png");
            ins.runOnMainSync(()->{assertNotEquals(0,dialog.get().getWindow().getAttributes().softInputMode&WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);assertFalse((dialog.get().getWindow().getAttributes().flags&WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)!=0);});
        }finally{ins.runOnMainSync(()->{if(dialog.get()!=null)dialog.get().dismiss();activity.finish();});}
    }
    @Test public void calendarCommitsOnlyOnConfirmAndSupportsMonthJump()throws Exception{
        Activity activity=start();AtomicReference<CalendarDialog> dialog=new AtomicReference<>();AtomicReference<LocalDate> committed=new AtomicReference<>();LocalDate today=LocalDate.now();
        try{
            ins.runOnMainSync(()->{CalendarDialog d=new CalendarDialog(activity,today,m->new HashSet<>(Arrays.asList(today.minusDays(2),today.minusDays(4))),committed::set);dialog.set(d);d.show();findText(d.getWindow().getDecorView(),"昨天").performClick();assertNull(committed.get());});
            capture("ui-calendar.png");
            ins.runOnMainSync(()->{View root=dialog.get().getWindow().getDecorView();findDescription(root,"选择年月").performClick();assertNotNull(findText(dialog.get().getWindow().getDecorView(),today.getYear()+" 年"));findText(dialog.get().getWindow().getDecorView(),today.getYear()+" 年").performClick();findText(dialog.get().getWindow().getDecorView(),today.getMonthValue()+" 月").performClick();dialog.get().getButton(-1).performClick();assertEquals(today.minusDays(1),committed.get());});
        }finally{ins.runOnMainSync(()->{if(dialog.get()!=null)dialog.get().dismiss();activity.finish();});}
    }
    private static View findDescription(View v,String text){if(v.getContentDescription()!=null&&text.contentEquals(v.getContentDescription()))return v;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){View r=findDescription(g.getChildAt(i),text);if(r!=null)return r;}}return null;}
    private static TextView findText(View v,String text){if(v instanceof TextView&&text.contentEquals(((TextView)v).getText()))return (TextView)v;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){TextView r=findText(g.getChildAt(i),text);if(r!=null)return r;}}return null;}
}
