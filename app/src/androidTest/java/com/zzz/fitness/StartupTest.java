package com.zzz.fitness;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Uses existing data without creating, clearing or changing any user records. */
@RunWith(AndroidJUnit4.class)
public class StartupTest {
    @Test public void launchesAndOpensEveryTab() {
        Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
        Intent intent=new Intent(instrumentation.getTargetContext(),MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Activity activity=instrumentation.startActivitySync(intent);
        instrumentation.waitForIdleSync();
        try {
            instrumentation.runOnMainSync(()->{
                View root=activity.getWindow().getDecorView();
                assertNotNull("Home must finish its first database read",findText(root,"今日训练日"));
                for(String meal:new String[]{"早餐","午餐","晚餐","加餐"}){
                    assertTrue(findText(root,meal+"  ＋")!=null||findText(root,meal+"  ✓")!=null);
                }
                click(root,"计划");assertNotNull(findText(root,"从一份 PDF 开始"));
                click(root,"趋势");assertNotNull(findText(root,"看见自己的每一点进步。"));
                click(root,"设置");assertNotNull(findText(root,"本地数据占用"));
                click(root,"今日");assertNotNull(findText(root,"今日训练日"));
            });
        } finally { instrumentation.runOnMainSync(activity::finish); }
    }
    private static void click(View root,String text) {
        TextView label=findText(root,text);assertNotNull("Missing tab: "+text,label);
        // The tab's parent owns the click listener.
        ((View)label.getParent()).performClick();
    }
    private static TextView findText(View view,String text) {
        if(view instanceof TextView&&text.contentEquals(((TextView)view).getText()))return (TextView)view;
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++){TextView match=findText(group.getChildAt(i),text);if(match!=null)return match;}}
        return null;
    }
}
