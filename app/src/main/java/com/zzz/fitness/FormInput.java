package com.zzz.fitness;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.*;
import android.text.*;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.*;

/** Rounded input with persistent label, unit suffix and an inline error. */
public class FormInput extends EditText {
    private TextView errorView;
    private CharSequence validationError;
    public FormInput(Context context){super(context);}
    private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private void refresh(){
        GradientDrawable bg=new GradientDrawable();bg.setCornerRadius(dp(16));bg.setColor(hasFocus()?0xffFFFFFF:0xffF2F5EF);
        bg.setStroke(dp(hasFocus()||validationError!=null?1.5f:1),validationError!=null?0xffB65E50:hasFocus()?0xff4E8B66:0xffE3E9DE);setBackground(bg);
    }
    @Override public void setError(CharSequence error){validationError=error;if(errorView!=null){errorView.setText(error);errorView.setVisibility(error==null?View.GONE:View.VISIBLE);}refresh();}
    @Override public void setError(CharSequence error,Drawable icon){setError(error);}
    @Override public CharSequence getError(){return validationError;}
    public static FormInput addTo(LinearLayout parent,String caption,String value,boolean numeric){
        Context c=parent.getContext();FormInput input=new FormInput(c);int pad=input.dp(16);
        LinearLayout group=new LinearLayout(c);group.setOrientation(LinearLayout.VERTICAL);
        LinearLayout heading=new LinearLayout(c);heading.setGravity(Gravity.CENTER_VERTICAL);
        String unit=numeric?(caption.contains("kcal")?"kcal":caption.contains("kg")?"kg":caption.contains("cm")?"cm":caption.contains("%")?"%":caption.contains("分钟")?"分钟":""):"";
        String title=caption.replace("（必填）","").replace("（选填）","").replace(" kcal","").replace(" kg","").replace(" cm","").replace(" %","").replace(" 分钟","");
        TextView label=new TextView(c);label.setText(title);label.setTextSize(13);label.setTextColor(0xff334D3D);label.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));heading.addView(label,new LinearLayout.LayoutParams(0,-2,1));
        boolean required=caption.contains("必填")||caption.startsWith("训练日名称")||caption.startsWith("每行一个项目");
        TextView optional=new TextView(c);optional.setText(required?"必填":"选填");optional.setTextSize(11);optional.setTextColor(0xff8B968B);heading.addView(optional);group.addView(heading);
        FrameLayout frame=new FrameLayout(c);LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(-1,-2);fp.topMargin=input.dp(8);group.addView(frame,fp);
        input.setTextSize(16);input.setTextColor(0xff203B31);input.setHintTextColor(0xff9AA69A);input.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));
        boolean multiline=!numeric&&(caption.contains("食物")||caption.contains("吃了")||caption.contains("备注")||caption.contains("感受")||caption.contains("每行")||caption.contains("组数")||caption.contains("实际组次"));
        input.setInputType(numeric?InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL:InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES|(multiline?InputType.TYPE_TEXT_FLAG_MULTI_LINE:0));
        input.setMinLines(multiline?3:1);input.setMaxLines(multiline?8:1);input.setMinHeight(input.dp(56));input.setGravity(Gravity.TOP|Gravity.START);
        input.setPadding(pad,pad,unit.isEmpty()?pad:input.dp(68),pad);input.setImeOptions(multiline?EditorInfo.IME_FLAG_NO_ENTER_ACTION:EditorInfo.IME_ACTION_NEXT);
        input.setContentDescription(caption);input.setHint(numeric?(unit.equals("kg")?"例如 68.5":unit.equals("%")?"例如 22.5":unit.equals("cm")?"例如 80":unit.equals("kcal")?"填写参考热量":"填写时长"):multiline?"写下内容，记录一些细节…":"请输入"+title);
        input.setText(value);input.setBackgroundTintList(null);input.refresh();frame.addView(input,new FrameLayout.LayoutParams(-1,-2));
        Drawable cursor=input.getTextCursorDrawable();if(cursor!=null){cursor=cursor.mutate();cursor.setTint(0xff34745A);input.setTextCursorDrawable(cursor);}
        if(!unit.isEmpty()){TextView suffix=new TextView(c);suffix.setText(unit);suffix.setTextSize(13);suffix.setTextColor(0xff7A897B);suffix.setGravity(Gravity.CENTER);FrameLayout.LayoutParams sp=new FrameLayout.LayoutParams(input.dp(64),input.dp(56),Gravity.TOP|Gravity.END);frame.addView(suffix,sp);}
        TextView error=new TextView(c);error.setTextColor(0xffB65E50);error.setTextSize(12);error.setPadding(input.dp(4),input.dp(6),0,0);error.setVisibility(View.GONE);error.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);group.addView(error);input.errorView=error;
        input.setOnFocusChangeListener((v,focused)->input.refresh());input.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){if(input.validationError!=null)input.setError(null);}public void afterTextChanged(Editable e){}});
        LinearLayout.LayoutParams gp=new LinearLayout.LayoutParams(-1,-2);gp.bottomMargin=input.dp(18);parent.addView(group,gp);return input;
    }
}
