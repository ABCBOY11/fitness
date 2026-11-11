package com.zzz.fitness;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import java.util.*;

/** One draft card per exercise; the persisted newline-based format stays compatible. */
public class TrainingDayForm extends LinearLayout {
    private final FormInput name;
    private final LinearLayout list;
    private final List<Entry> entries=new ArrayList<>();
    private final TextView count;
    private class Entry {
        LinearLayout card;
        TextView number,up,down;
        FormInput title,plan;
    }
    public TrainingDayForm(Context context,String title,String body){
        super(context);setOrientation(VERTICAL);setPadding(dp(20),dp(20),dp(20),dp(12));
        name=FormInput.addTo(this,"训练日名称（必填）",title,false);
        count=label("",15);addView(count);addView(label("每个动作单独编辑，组次、负重或时长写在计划要求中。",13));
        list=new LinearLayout(context);list.setOrientation(VERTICAL);addView(list);
        TextView add=button("＋ 添加项目",()->{Entry e=append("","");e.title.requestFocus();e.card.post(()->e.card.requestRectangleOnScreen(new android.graphics.Rect(0,0,e.card.getWidth(),e.card.getHeight()),false));});
        addView(add,new LayoutParams(-1,-2));addView(label("保存后用于之后的训练安排。已记录的训练保持原样。",12));
        for(String item:body.split("\\n"))if(!item.trim().isEmpty()){
            String[] parts=item.split("[|｜]",2);append(parts[0].trim(),parts.length>1?parts[1].trim():"");
        }
        if(entries.isEmpty())append("","");
    }
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private TextView label(String value,int size){TextView v=new TextView(getContext());v.setText(value);v.setTextSize(size);v.setTextColor(0xff607568);v.setPadding(0,dp(8),0,dp(12));v.setLineSpacing(dp(3),1);return v;}
    private TextView button(String title,Runnable click){TextView v=label(title,14);v.setTextColor(0xff34745A);v.setGravity(Gravity.CENTER);v.setMinHeight(dp(48));v.setPadding(dp(10),dp(12),dp(10),dp(12));v.setOnClickListener(w->click.run());return v;}
    private Entry append(String title,String plan){
        Entry e=new Entry();e.card=new LinearLayout(getContext());e.card.setOrientation(VERTICAL);e.card.setPadding(dp(16),dp(8),dp(16),0);
        GradientDrawable bg=new GradientDrawable();bg.setColor(Color.WHITE);bg.setCornerRadius(dp(18));bg.setStroke(dp(1),0xffE3E9DE);e.card.setBackground(bg);
        LinearLayout header=new LinearLayout(getContext());header.setGravity(Gravity.CENTER_VERTICAL);
        e.number=label("",14);header.addView(e.number,new LayoutParams(0,-2,1));
        e.up=button("↑",()->move(e,-1));e.down=button("↓",()->move(e,1));
        header.addView(e.up,new LayoutParams(dp(48),-2));header.addView(e.down,new LayoutParams(dp(48),-2));
        TextView remove=button("删除",()->{entries.remove(e);list.removeView(e.card);refresh();});remove.setTextColor(0xffA25346);header.addView(remove);e.card.addView(header);
        e.title=FormInput.addTo(e.card,"动作名称（必填）",title,false);
        e.title.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);e.title.setMaxLines(4);
        e.plan=FormInput.addTo(e.card,"计划要求（选填）",plan,false);e.plan.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);e.plan.setMinLines(2);e.plan.setMaxLines(6);e.plan.setHint("例如：3 组 × 10 次，休息 60 秒");
        LayoutParams cp=new LayoutParams(-1,-2);cp.topMargin=dp(12);cp.bottomMargin=dp(4);list.addView(e.card,cp);entries.add(e);refresh();return e;
    }
    private void move(Entry e,int delta){int from=entries.indexOf(e),to=from+delta;if(to<0||to>=entries.size())return;Collections.swap(entries,from,to);list.removeView(e.card);list.addView(e.card,to);refresh();}
    private void refresh(){count.setText("训练项目 · "+entries.size()+" 项");for(int i=0;i<entries.size();i++){Entry e=entries.get(i);e.number.setText("项目 "+String.format(Locale.ROOT,"%02d",i+1));e.up.setEnabled(i>0);e.up.setAlpha(i>0?1:.25f);e.down.setEnabled(i<entries.size()-1);e.down.setAlpha(i<entries.size()-1?1:.25f);e.up.setContentDescription("上移项目 "+(i+1));e.down.setContentDescription("下移项目 "+(i+1));e.title.setContentDescription("项目 "+(i+1)+" 动作名称");e.plan.setContentDescription("项目 "+(i+1)+" 计划要求");}}
    public String title(){String s=name.getText().toString().trim();if(s.isEmpty()){name.setError("请填写训练日名称");name.requestFocus();throw new IllegalArgumentException();}return s;}
    public String body(){
        if(entries.isEmpty()){count.setText("请添加至少一个训练项目");count.requestRectangleOnScreen(new android.graphics.Rect(0,0,count.getWidth(),count.getHeight()));throw new IllegalArgumentException();}
        List<String> values=new ArrayList<>();for(Entry e:entries){String title=e.title.getText().toString().replaceAll("[\\r\\n]+"," ").trim(),plan=e.plan.getText().toString().replaceAll("[\\r\\n]+"," ").trim();if(title.isEmpty()){e.title.setError("请填写动作名称，或删除此项目");e.title.requestFocus();throw new IllegalArgumentException();}values.add(title+(plan.isEmpty()?"":" | "+plan));}return String.join("\n",values);
    }
}
