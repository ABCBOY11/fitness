package com.zzz.fitness;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.*;

/** Monday-first calendar; selection is committed only by the confirmation button. */
public final class CalendarDialog extends FormDialog {
    private final LinearLayout panel;
    private final LocalDate today=LocalDate.now();
    private LocalDate chosen;
    private YearMonth month;
    private int mode=0,yearPage;
    private final Function<YearMonth,Set<LocalDate>> recordDays;
    private final Consumer<LocalDate> onConfirm;
    private final String[] week={"一","二","三","四","五","六","日"};
    public CalendarDialog(Context c,LocalDate selected,Function<YearMonth,Set<LocalDate>> records,Consumer<LocalDate> onConfirm){
        this(c,new LinearLayout(c),selected,records,onConfirm);
    }
    private CalendarDialog(Context c,LinearLayout panel,LocalDate selected,Function<YearMonth,Set<LocalDate>> records,Consumer<LocalDate> confirm){
        super(c,"选择记录日期",panel);this.panel=panel;this.chosen=selected;this.month=YearMonth.from(selected);this.recordDays=records;this.onConfirm=confirm;yearPage=(selected.getYear()/12)*12;
        panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(20),dp(18),dp(20),dp(8));setButton(BUTTON_POSITIVE,"确认日期",(android.content.DialogInterface.OnClickListener)null);
    }
    private int dp(float n){return Math.round(n*getContext().getResources().getDisplayMetrics().density);}
    private GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private TextView text(String s,int size,int color){TextView v=new TextView(getContext());v.setText(s);v.setTextSize(size);v.setTextColor(color);v.setGravity(Gravity.CENTER);v.setIncludeFontPadding(false);return v;}
    private LinearLayout row(){LinearLayout l=new LinearLayout(getContext());l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private void add(View view){panel.addView(view,new LinearLayout.LayoutParams(-1,-2));}
    private void gap(int n){Space s=new Space(getContext());panel.addView(s,new LinearLayout.LayoutParams(1,dp(n)));}
    @Override protected void onStart(){super.onStart();render();getButton(BUTTON_POSITIVE).setOnClickListener(v->{onConfirm.accept(chosen);dismiss();});}
    private TextView chip(String title,Runnable action){TextView v=text(title,13,0xff34745A);v.setBackground(shape(0xffEDF3E7,12));v.setPadding(dp(10),dp(12),dp(10),dp(12));v.setOnClickListener(b->action.run());return v;}
    private void jump(LocalDate date){chosen=date;month=YearMonth.from(date);mode=0;render();}
    private void render(){
        panel.removeAllViews();
        TextView date=text(chosen.format(DateTimeFormatter.ofPattern("yyyy 年 M 月 d 日"))+"  周"+week[chosen.getDayOfWeek().getValue()-1],19,0xff203B31);date.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));date.setContentDescription("选中日期 "+chosen);add(date);gap(14);
        LinearLayout shortcuts=row();String[] labels={"今天","昨天","本周一"};LocalDate[] dates={today,today.minusDays(1),today.minusDays(today.getDayOfWeek().getValue()-1)};
        for(int i=0;i<labels.length;i++){final LocalDate target=dates[i];TextView c=chip(labels[i],()->jump(target));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(44),1);p.setMargins(dp(3),0,dp(3),0);shortcuts.addView(c,p);}add(shortcuts);gap(20);
        LinearLayout navigation=row();TextView prev=text("‹",26,0xff34745A),next=text("›",26,0xff34745A);
        prev.setContentDescription(mode==1?"上一组年份":"上个月");next.setContentDescription(mode==1?"下一组年份":"下个月");
        String caption=mode==0?month.getYear()+" 年  "+month.getMonthValue()+" 月  ▾":mode==1?yearPage+" – "+(yearPage+11)+" 年":month.getYear()+" 年  ·  选择月份";
        TextView title=text(caption,16,0xff203B31);title.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));title.setContentDescription("选择年月");
        prev.setOnClickListener(v->{if(mode==0&&month.isAfter(YearMonth.of(1970,1)))month=month.minusMonths(1);else if(mode==1)yearPage=Math.max(1968,yearPage-12);else if(mode==2&&month.getYear()>1970)month=month.minusYears(1);render();});
        boolean canNext=mode==0?month.isBefore(YearMonth.from(today)):mode==1?yearPage+11<today.getYear():month.getYear()<today.getYear();
        next.setEnabled(canNext);next.setAlpha(canNext?1:.25f);next.setOnClickListener(v->{if(mode==0)month=month.plusMonths(1);else if(mode==1)yearPage+=12;else month=month.plusYears(1);render();});
        title.setOnClickListener(v->{mode=mode==1?0:1;yearPage=(month.getYear()/12)*12;render();});
        navigation.addView(prev,new LinearLayout.LayoutParams(dp(44),dp(44)));navigation.addView(title,new LinearLayout.LayoutParams(0,dp(44),1));navigation.addView(next,new LinearLayout.LayoutParams(dp(44),dp(44)));add(navigation);gap(12);
        if(mode==0)days();else choices();
    }
    private void days(){
        LinearLayout weekdays=row();for(int i=0;i<7;i++){TextView label=text(week[i],12,i>=5?0xff8F9F8D:0xff798779);weekdays.addView(label,new LinearLayout.LayoutParams(0,dp(28),1));}add(weekdays);gap(4);
        Set<LocalDate> records=recordDays.apply(month);
        for(int r=0;r<6;r++){
            LinearLayout line=row();for(int col=0;col<7;col++){
                LocalDate d=CalendarModel.cell(month,r*7+col);LinearLayout cell=new LinearLayout(getContext());cell.setOrientation(LinearLayout.VERTICAL);cell.setGravity(Gravity.CENTER);
                LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,dp(47),1);cp.setMargins(dp(2),dp(1),dp(2),dp(1));line.addView(cell,cp);
                if(d==null)continue;
                boolean enabled=CalendarModel.selectable(d,today),selected=d.equals(chosen);TextView number=text(""+d.getDayOfMonth(),15,selected?Color.WHITE:enabled?0xff203B31:0xffCBD2C7);cell.addView(number,new LinearLayout.LayoutParams(-1,dp(27)));
                GradientDrawable bg=shape(selected?0xff34745A:Color.TRANSPARENT,15);if(d.equals(today)&&!selected)bg.setStroke(dp(1),0xff83A27D);cell.setBackground(bg);
                View dot=new View(getContext());dot.setBackground(shape(selected?0xffD9EAB6:0xff82A36F,3));dot.setVisibility(records.contains(d)?View.VISIBLE:View.INVISIBLE);cell.addView(dot,new LinearLayout.LayoutParams(dp(4),dp(4)));
                cell.setEnabled(enabled);cell.setContentDescription("选择日期 "+d+(records.contains(d)?"，有记录":""));cell.setSelected(selected);if(enabled)cell.setOnClickListener(v->{chosen=d;render();});
            }add(line);
        }
        gap(10);TextView help=text("●  有记录     ·     点击年月可快速跳转",11,0xff84917E);add(help);
    }
    private void choices(){
        for(int r=0;r<4;r++){LinearLayout line=row();for(int c=0;c<3;c++){
            int value=mode==1?yearPage+r*3+c:r*3+c+1;boolean enabled=mode==1?value>=1970&&value<=today.getYear():!YearMonth.of(month.getYear(),value).isAfter(YearMonth.from(today));
            TextView item=chip(mode==1?value+" 年":value+" 月",()->{if(mode==1){month=YearMonth.of(value,Math.min(month.getMonthValue(),value==today.getYear()?today.getMonthValue():12));mode=2;}else{month=YearMonth.of(month.getYear(),value);mode=0;}render();});item.setEnabled(enabled);item.setAlpha(enabled?1:.25f);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(52),1);p.setMargins(dp(3),dp(4),dp(3),dp(4));line.addView(item,p);
        }add(line);}gap(12);add(text("选择年份后选择月份，再点击具体日期",12,0xff84917E));
    }
}
