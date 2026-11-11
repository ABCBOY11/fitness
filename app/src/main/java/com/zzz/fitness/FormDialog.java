package com.zzz.fitness;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.util.HashMap;
import java.util.Map;

/** Branded, keyboard-aware form sheet. Keeps AlertDialog's button contract for callers. */
public class FormDialog extends AlertDialog {
    private final String heading;
    private final LinearLayout fields;
    private final Map<Integer,Button> buttons=new HashMap<>();
    private final Map<Integer,CharSequence> captions=new HashMap<>();
    private final Map<Integer,DialogInterface.OnClickListener> callbacks=new HashMap<>();
    private LinearLayout shell, footer;
    private boolean expanded;

    public FormDialog expanded(){expanded=true;return this;}

    public FormDialog(Context context,String heading,LinearLayout fields){
        super(context);this.heading=heading;this.fields=fields;
        captions.put(BUTTON_POSITIVE,"保存");captions.put(BUTTON_NEGATIVE,"取消");
    }
    private int dp(float value){return Math.round(value*getContext().getResources().getDisplayMetrics().density);}
    private TextView text(String value,int size,int color){TextView t=new TextView(getContext());t.setText(value);t.setTextSize(size);t.setTextColor(color);return t;}
    private GradientDrawable background(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        shell=new LinearLayout(getContext()){
            @Override protected void onMeasure(int width,int height){
                int max=(int)(getResources().getDisplayMetrics().heightPixels*(expanded?.94f:.89f));
                int available=MeasureSpec.getMode(height)==MeasureSpec.UNSPECIFIED?max:Math.min(max,MeasureSpec.getSize(height));
                super.onMeasure(width,MeasureSpec.makeMeasureSpec(available,expanded?MeasureSpec.EXACTLY:MeasureSpec.AT_MOST));
            }
        };
        shell.setOrientation(LinearLayout.VERTICAL);shell.setBackground(background(0xffFCFDF9,28));
        shell.setPadding(0,dp(10),0,dp(12));shell.setFocusableInTouchMode(true);
        View handle=new View(getContext());handle.setBackground(background(0xffDCE4DA,3));
        LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(dp(32),dp(4));hp.gravity=Gravity.CENTER_HORIZONTAL;hp.bottomMargin=dp(18);shell.addView(handle,hp);
        LinearLayout header=new LinearLayout(getContext());header.setGravity(Gravity.CENTER_VERTICAL);header.setPadding(dp(24),0,dp(20),dp(18));
        LinearLayout titles=new LinearLayout(getContext());titles.setOrientation(LinearLayout.VERTICAL);
        TextView eyebrow=text("循迹  /  记录与计划",11,0xff748678);eyebrow.setLetterSpacing(.08f);titles.addView(eyebrow);
        TextView title=text(heading,23,0xff203B31);title.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));title.setPadding(0,dp(7),0,0);titles.addView(title);
        header.addView(titles,new LinearLayout.LayoutParams(0,-2,1));
        TextView close=text("×",25,0xff6C7D70);close.setGravity(Gravity.CENTER);close.setBackground(background(0xffEDF1E9,22));close.setContentDescription("关闭表单");close.setOnClickListener(v->dismiss());header.addView(close,new LinearLayout.LayoutParams(dp(44),dp(44)));shell.addView(header);
        View divider=new View(getContext());divider.setBackgroundColor(0xffE9EEE5);shell.addView(divider,new LinearLayout.LayoutParams(-1,dp(1)));
        ScrollView scroll=new ScrollView(getContext());scroll.setFillViewport(false);scroll.setClipToPadding(false);scroll.setVerticalScrollBarEnabled(false);
        scroll.addView(fields);shell.addView(scroll,new LinearLayout.LayoutParams(-1,-2,1));
        footer=new LinearLayout(getContext());footer.setOrientation(LinearLayout.VERTICAL);footer.setPadding(dp(20),dp(14),dp(20),0);
        LinearLayout actions=new LinearLayout(getContext());actions.setGravity(Gravity.CENTER_VERTICAL);
        for(int which:new int[]{BUTTON_NEGATIVE,BUTTON_POSITIVE}){
            Button button=makeButton(which);LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(0,dp(52),which==BUTTON_POSITIVE?1.65f:1f);bp.setMargins(dp(4),0,dp(4),0);actions.addView(button,bp);
        }
        footer.addView(actions);Button remove=makeButton(BUTTON_NEUTRAL);LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,dp(44));rp.topMargin=dp(6);footer.addView(remove,rp);shell.addView(footer);
        setContentView(shell);Window window=getWindow();
        if(window!=null){window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));window.setDimAmount(.32f);window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);window.clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE|WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);window.setGravity(Gravity.BOTTOM);}
        shell.requestFocus();
    }
    @Override protected void onStart(){
        super.onStart();Window w=getWindow();if(w!=null){int max=dp(560),width=getContext().getResources().getDisplayMetrics().widthPixels;w.setLayout(Math.min(width-dp(16),max),WindowManager.LayoutParams.WRAP_CONTENT);WindowManager.LayoutParams a=w.getAttributes();a.y=dp(8);w.setAttributes(a);}
    }
    private Button makeButton(int which){
        Button b=new Button(getContext());b.setAllCaps(false);b.setTextSize(15);b.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));b.setMinWidth(0);b.setMinimumWidth(0);b.setPadding(dp(12),0,dp(12),0);b.setStateListAnimator(null);
        b.setTextColor(which==BUTTON_POSITIVE?Color.WHITE:which==BUTTON_NEUTRAL?0xffA25346:0xff34745A);
        b.setBackground(background(which==BUTTON_POSITIVE?0xff34745A:which==BUTTON_NEUTRAL?Color.TRANSPARENT:0xffEAF0E5,16));
        buttons.put(which,b);refresh(which);b.setOnClickListener(v->{DialogInterface.OnClickListener cb=callbacks.get(which);if(cb!=null)cb.onClick(this,which);dismiss();});return b;
    }
    private void refresh(int which){Button b=buttons.get(which);if(b==null)return;CharSequence caption=captions.get(which);b.setText(caption);b.setVisibility(caption==null?View.GONE:View.VISIBLE);}
    @Override public void setButton(int which,CharSequence text,DialogInterface.OnClickListener listener){captions.put(which,text);callbacks.put(which,listener);refresh(which);}
    @Override public Button getButton(int which){return buttons.get(which);}
    @Override public void dismiss(){
        Window w=getWindow();if(w!=null){InputMethodManager ime=(InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE);if(ime!=null)ime.hideSoftInputFromWindow(w.getDecorView().getWindowToken(),0);}super.dismiss();
    }
}
