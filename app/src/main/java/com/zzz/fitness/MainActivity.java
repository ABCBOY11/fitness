package com.zzz.fitness;

import android.app.*;
import android.content.*;
import android.database.Cursor;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.graphics.pdf.PdfRenderer;
import android.graphics.pdf.content.PdfPageTextContent;
import android.net.Uri;
import android.os.*;
import android.provider.OpenableColumns;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;
import java.util.zip.*;
import org.xmlpull.v1.XmlPullParser;

public class MainActivity extends Activity {
    private static final int BG=0xffF5F6F1, INK=0xff203B31, GREEN=0xff34745A, MUTED=0xff79867D, LINE=0xffE5E9E1;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private FitnessDb db;
    private LinearLayout root, content, nav;
    private TextView progressCount,progressHint;
    private Ring progressRing;
    private LocalDate selected=LocalDate.now();
    private int tab=0, trendDays=30, trendMetric=0, cleanup=0;
    private boolean importing=false;
    private String exportAsset;
    private ProgressDialog extractionProgress;
    private PdfPreview pdfPreview;
    private final java.util.List<Dialog> formSheets=new ArrayList<>();
    private final String[] weekdays={"一","二","三","四","五","六","日"};

    @Override public void onCreate(Bundle state){
        super.onCreate(state); db=new FitnessDb(this);
        if(state!=null){exportAsset=state.getString("exportAsset");tab=state.getInt("tab");selected=LocalDate.parse(state.getString("day",LocalDate.now().toString()));}
        root=column();root.setBackgroundColor(BG);
        root.setOnApplyWindowInsetsListener((v,insets)->{android.graphics.Insets i=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());root.setPadding(i.left,i.top,i.right,i.bottom);return insets;});
        setContentView(root);show();
    }
    @Override public void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);out.putString("exportAsset",exportAsset);out.putInt("tab",tab);out.putString("day",selected.toString());}
    @Override protected void onDestroy(){
        for(Dialog sheet:new ArrayList<>(formSheets))sheet.dismiss();
        if(extractionProgress!=null)extractionProgress.dismiss();
        if(pdfPreview!=null)pdfPreview.dismiss();
        worker.execute(db::close);worker.shutdown();super.onDestroy();
    }
    private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private TextView text(String s,int sp,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(color);if(bold)t.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));t.setLineSpacing(dp(3),1);return t;}
    private TextView label(String s){return text(s,13,MUTED,false);}
    private void gap(LinearLayout l,int size){Space s=new Space(this);l.addView(s,new LinearLayout.LayoutParams(1,dp(size)));}
    private void add(LinearLayout l,View v){l.addView(v,new LinearLayout.LayoutParams(-1,-2));}
    private TextView action(String s,boolean primary,Runnable run){TextView t=text(s,14,primary?Color.WHITE:GREEN,true);t.setGravity(Gravity.CENTER);t.setPadding(dp(14),dp(13),dp(14),dp(13));t.setBackground(shape(primary?GREEN:0xffEAF0E8,14));t.setOnClickListener(v->run.run());t.setMinHeight(dp(48));return t;}
    private void weighted(LinearLayout row,View v){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.setMargins(dp(3),0,dp(3),0);row.addView(v,p);}
    private LinearLayout card(LinearLayout parent){LinearLayout c=column();c.setPadding(dp(18),dp(18),dp(18),dp(18));c.setBackground(shape(Color.WHITE,22));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(14);parent.addView(c,p);return c;}
    private void section(String title,String note){gap(content,8);LinearLayout r=row();TextView t=text(title,19,INK,true);r.addView(t,new LinearLayout.LayoutParams(0,-2,1));if(note!=null)r.addView(label(note));add(content,r);gap(content,14);}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
    private void show(){
        if(isFinishing()||isDestroyed())return;
        root.removeAllViews();
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setClipToPadding(false);
        content=column();content.setPadding(dp(22),dp(22),dp(22),dp(18));scroll.addView(content);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        switch(tab){case 0:today();break;case 1:plans();break;case 2:trends();break;case 3:settings();break;}
        nav=row();nav.setBackgroundColor(Color.WHITE);nav.setPadding(dp(10),dp(8),dp(10),dp(8));
        String[] titles={"今日","计划","趋势","设置"};String[] symbols={"◉","▤","↗","⚙"};
        for(int i=0;i<4;i++){final int idx=i;LinearLayout item=column();item.setGravity(Gravity.CENTER);TextView icon=text(symbols[i],23,tab==i?GREEN:MUTED,true);icon.setGravity(Gravity.CENTER);add(item,icon);TextView title=text(titles[i],11,tab==i?GREEN:MUTED,tab==i);title.setGravity(Gravity.CENTER);add(item,title);item.setContentDescription(titles[i]);item.setPadding(0,dp(3),0,dp(3));item.setBackground(shape(tab==i?0xffEDF3EA:Color.WHITE,16));item.setOnClickListener(v->{tab=idx;show();});nav.addView(item,new LinearLayout.LayoutParams(0,dp(59),1));}
        add(root,nav);
    }
    private void header(String eyebrow,String title,String subtitle){add(content,text(eyebrow,11,GREEN,true));gap(content,8);add(content,text(title,30,INK,true));gap(content,6);add(content,label(subtitle));gap(content,24);}
    private java.util.List<FitnessDb.Row> dayRows(){return db.records(selected.toString(),selected.toString());}
    private java.util.List<FitnessDb.Row> dueTasks(){java.util.List<FitnessDb.Row> tasks=new ArrayList<>();for(FitnessDb.Row r:db.tasks())if(r.days.contains(""+selected.getDayOfWeek().getValue()))tasks.add(r);return tasks;}
    private void today(){
        header("循迹  /  DAILY FITNESS","让每一天，都有迹可循。","一份计划，一点行动，慢慢成为更好的自己。");
        LinearLayout dates=row();dates.setPadding(dp(6),dp(6),dp(6),dp(6));dates.setBackground(shape(Color.WHITE,20));TextView prev=action("‹",false,()->{selected=selected.minusDays(1);show();});dates.addView(prev,new LinearLayout.LayoutParams(dp(46),-2));
        TextView date=text(selected.format(DateTimeFormatter.ofPattern("M 月 d 日"))+"  周"+weekdays[selected.getDayOfWeek().getValue()-1]+(selected.equals(LocalDate.now())?" · 今天":"")+"  ▾",15,INK,true);date.setGravity(Gravity.CENTER);dates.addView(date,new LinearLayout.LayoutParams(0,-2,1));date.setContentDescription("打开日期选择");date.setOnClickListener(v->pickDate());
        TextView next=action("›",false,()->{if(selected.isBefore(LocalDate.now())){selected=selected.plusDays(1);show();}});next.setAlpha(selected.equals(LocalDate.now())?.3f:1);dates.addView(next,new LinearLayout.LayoutParams(dp(46),-2));add(content,dates);gap(content,18);
        java.util.List<FitnessDb.Row> records=dayRows(), tasks=dueTasks();int done=0;for(FitnessDb.Row t:tasks)for(FitnessDb.Row r:records)if(r.taskId==t.id)done++;
        FitnessDb.Row session=findSession(records);
        int total=tasks.size();if(session!=null){JSONArray items=sessionItems(session);total+=items.length();for(int i=0;i<items.length();i++)if(items.optJSONObject(i).optBoolean("done"))done++;}
        LinearLayout hero=card(content);hero.setBackground(shape(INK,24));LinearLayout hr=row();LinearLayout hl=column();add(hl,text("TODAY'S PROGRESS",10,0xffB8D0BD,true));gap(hl,10);progressCount=text(done+" / "+total,38,Color.WHITE,true);add(hl,progressCount);progressHint=text(total==0?"从你的第一份计划开始":done==total?"今日计划已完成，做得很好":"每一次打卡，都是一次积累",13,0xffC9D9CA,false);add(hl,progressHint);hr.addView(hl,new LinearLayout.LayoutParams(0,-2,1));progressRing=new Ring(total==0?0:(float)done/total);hr.addView(progressRing,new LinearLayout.LayoutParams(dp(90),dp(90)));add(hero,hr);
        trainingPanel(records);
        mealPanel(records);
        LinearLayout quick=row();weighted(quick,action("＋  记饮食",false,()->mealChooser(records)));weighted(quick,action("＋  记训练",false,()->recordDialog("training",null)));weighted(quick,action("＋  记指标",false,()->metricDialog(null)));add(content,quick);gap(content,14);
        if(!tasks.isEmpty()){
        section("额外重复打卡",tasks.size()+" 项");
        LinearLayout taskCard=card(content);
        if(tasks.isEmpty()){add(taskCard,text("先给今天安排一点小目标",16,INK,true));gap(taskCard,8);add(taskCard,label("导入 GPT 生成的 PDF，或手动添加每周重复的打卡项。"));gap(taskCard,16);add(taskCard,action("去添加计划  ↗",true,()->{tab=1;show();}));}
        for(FitnessDb.Row t:tasks){FitnessDb.Row found=null;for(FitnessDb.Row r:records)if(r.taskId==t.id)found=r;final FitnessDb.Row checked=found;LinearLayout line=row();CheckBox box=new CheckBox(this);box.setChecked(found!=null);box.setContentDescription("完成 "+t.title);line.addView(box);LinearLayout info=column();add(info,text(t.title,15,INK,true));add(info,label(t.kind.equals("diet")?"饮食计划":"训练计划"));line.addView(info,new LinearLayout.LayoutParams(0,-2,1));line.setPadding(0,dp(6),0,dp(6));add(taskCard,line);box.setOnCheckedChangeListener((b,on)->{if(on){FitnessDb.Row r=new FitnessDb.Row();r.day=selected.toString();r.kind="check";r.title=t.title;r.note=t.kind;r.taskId=t.id;db.saveRecord(r);}else for(FitnessDb.Row saved:dayRows())if(saved.taskId==t.id)db.deleteRecord(saved.id);refreshProgress();});}
        }
        section("当天记录","点击可编辑");boolean any=false;
        for(FitnessDb.Row r:records){if(r.kind.equals("check")||r.kind.equals("session")||isMeal(r))continue;any=true;LinearLayout c=card(content);LinearLayout line=row();TextView badge=text(r.kind.equals("diet")?"食":r.kind.equals("training")?"练":"测",16,GREEN,true);badge.setGravity(Gravity.CENTER);badge.setBackground(shape(0xffEDF2E8,12));line.addView(badge,new LinearLayout.LayoutParams(dp(42),dp(42)));LinearLayout info=column();info.setPadding(dp(12),0,0,0);add(info,text(r.kind.equals("metric")?"身体指标":r.title,16,INK,true));String detail=r.kind.equals("metric")?"体重 "+number(r.value)+" kg"+(r.extra>0?" · 体脂 "+number(r.extra)+"%":"")+(r.third>0?" · 腰围 "+number(r.third)+" cm":""):r.value>0?number(r.value)+(r.kind.equals("diet")?" kcal（手动填写）":" 分钟"):"已记录";add(info,label(detail));line.addView(info,new LinearLayout.LayoutParams(0,-2,1));line.addView(label("›"));add(c,line);if(!r.note.isEmpty()){gap(c,10);add(c,label(r.note));}c.setOnClickListener(v->{if(r.kind.equals("metric"))metricDialog(r);else recordDialog(r.kind,r);});}
        for(FitnessDb.Row r:records){
            if(!r.kind.equals("check"))continue;
            boolean visible=false;for(FitnessDb.Row t:tasks)if(t.id==r.taskId)visible=true;
            if(visible)continue;
            any=true;LinearLayout history=card(content);add(history,text("✓  "+r.title,16,INK,true));
            gap(history,6);add(history,label("已完成的历史打卡 · 原打卡项已删除或调整 · 点击可删除记录"));
            history.setOnClickListener(v->confirmDelete(r));
        }
        if(!any){LinearLayout c=card(content);add(c,label("今天的故事还没写下。记录一餐饭、一次训练或一次称重吧。"));}
        add(content,label("记录属于你 · 全部保存在这台设备"));
    }
    private void trackSheet(Dialog sheet){formSheets.add(sheet);sheet.setOnDismissListener(d->formSheets.remove(sheet));}
    private void pickDate(){
        CalendarDialog sheet=new CalendarDialog(this,selected,month->{Set<LocalDate> dates=new HashSet<>();for(FitnessDb.Row r:db.records(month.atDay(1).toString(),month.atEndOfMonth().toString()))dates.add(LocalDate.parse(r.day));return dates;},date->{selected=date;show();});
        trackSheet(sheet);sheet.show();
    }
    private EditText field(LinearLayout parent,String hint,String value,boolean numeric){return FormInput.addTo(parent,hint,value,numeric);}
    private LinearLayout form(){LinearLayout f=column();f.setPadding(dp(24),dp(20),dp(24),dp(4));return f;}
    private AlertDialog dialog(String title,LinearLayout form){FormDialog sheet=new FormDialog(this,title,form);trackSheet(sheet);return sheet;}
    private double numeric(EditText e,double min,double max,boolean optional){String s=e.getText().toString().trim();if(s.isEmpty()&&optional)return 0;try{double n=Double.parseDouble(s);if(Double.isFinite(n)&&n>=min&&n<=max)return n;}catch(NumberFormatException ignored){}e.setError("请输入 "+min+" 至 "+max+" 之间的数值");throw new IllegalArgumentException();}
    private void recordDialog(String kind,FitnessDb.Row existing){
        LinearLayout f=form();add(f,label(selected+" · "+(kind.equals("diet")?"饮食记录":"训练记录")));gap(f,12);
        EditText title=field(f,kind.equals("diet")?"餐次 / 食物（必填）":"训练项目（必填）",existing==null?"":existing.title,false);
        EditText value=field(f,kind.equals("diet")?"热量 kcal（选填）":"训练时长 分钟（选填）",existing==null||existing.value==0?"":number(existing.value),true);
        EditText note=field(f,kind.equals("diet")?"份量、食材与备注":"组数、次数、负重与感受",existing==null?"":existing.note,false);
        AlertDialog d=dialog(kind.equals("diet")?"记录这一餐":"记录这次训练",f);if(existing!=null)d.setButton(AlertDialog.BUTTON_NEUTRAL,"删除",(a,b)->confirmDelete(existing));d.show();d.getButton(-1).setOnClickListener(v->{if(title.getText().toString().trim().isEmpty()){title.setError("请填写名称");return;}try{FitnessDb.Row r=existing==null?new FitnessDb.Row():existing;r.title=title.getText().toString().trim();r.note=note.getText().toString().trim();r.kind=kind;r.day=selected.toString();r.value=numeric(value,0,kind.equals("diet")?20000:1440,true);db.saveRecord(r);d.dismiss();show();}catch(IllegalArgumentException ignored){}});
    }
    private void metricDialog(FitnessDb.Row existing){
        if(existing==null)for(FitnessDb.Row r:dayRows())if(r.kind.equals("metric"))existing=r;
        final FitnessDb.Row target=existing;LinearLayout f=form();add(f,label(selected+" · 每天保留一组指标，可随时修改"));gap(f,12);
        EditText weight=field(f,"体重 kg（必填）",target==null?"":number(target.value),true),fat=field(f,"体脂率 %（选填）",target==null||target.extra==0?"":number(target.extra),true),waist=field(f,"腰围 cm（选填）",target==null||target.third==0?"":number(target.third),true),note=field(f,"备注",target==null?"":target.note,false);
        AlertDialog d=dialog("身体指标",f);if(target!=null)d.setButton(-3,"删除",(a,b)->confirmDelete(target));d.show();d.getButton(-1).setOnClickListener(v->{try{double w=numeric(weight,20,500,false),b=numeric(fat,1,75,true),c=numeric(waist,20,300,true);FitnessDb.Row r=target==null?new FitnessDb.Row():target;r.day=selected.toString();r.kind="metric";r.title="身体指标";r.value=w;r.extra=b;r.third=c;r.note=note.getText().toString().trim();db.saveRecord(r);d.dismiss();show();}catch(IllegalArgumentException ignored){}});
    }
    private void confirmDelete(FitnessDb.Row r){new AlertDialog.Builder(this).setTitle("删除这条记录？").setMessage("删除后无法恢复。").setNegativeButton("取消",null).setPositiveButton("删除",(d,w)->{db.deleteRecord(r.id);show();}).show();}
    private void plans(){
        header("YOUR PERSONAL ROADMAP","计划，让行动更简单。","把 GPT 的建议，变成每天能完成的小事。");
        LinearLayout c=card(content);add(c,text("从一份 PDF 开始",22,INK,true));gap(c,8);
        add(c,label("下载标准模板，在 Word 中填写各训练日后上传 Word 或 PDF。每天选择训练日即可自动展开项目，支持任意三练、四练、五练安排。"));gap(c,14);
        LinearLayout downloads=row();weighted(downloads,action("下载 Word",false,()->exportTemplate("fitness-plan.docx")));weighted(downloads,action("下载 PDF",false,()->exportTemplate("fitness-plan.pdf")));add(c,downloads);gap(c,12);
        add(c,action(importing?"正在导入…":"＋  上传 Word / PDF",true,()->{if(!importing){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("*/*");i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"application/pdf","application/vnd.openxmlformats-officedocument.wordprocessingml.document"});i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,100);}}));
        gap(c,10);add(c,label("本地处理 · 最大 30 MB · 支持 .docx 和带文字的 PDF"));
        trainingLibrary();
        section("我的计划文档",db.plans().size()+" 份");
        for(FitnessDb.Row p:db.plans()){LinearLayout doc=card(content);add(doc,text((p.path.endsWith(".docx")?"WORD  /  ":"PDF  /  ")+p.title,16,INK,true));gap(doc,6);add(doc,label(p.note));gap(doc,14);LinearLayout actions=row();weighted(actions,action("阅读原文",false,()->{if(p.path.endsWith(".docx"))toast("Word 文档请用原编辑器阅读；可直接提取训练日");else openPdf(p);}));weighted(actions,action("提取训练日",false,()->extractPlan(p)));weighted(actions,action("移除",false,()->new AlertDialog.Builder(this).setTitle("移除 PDF？").setMessage("移除本地文档，已添加的打卡项和历史记录会保留。").setNegativeButton("取消",null).setPositiveButton("移除",(d,w)->{File file=new File(p.path);if(!file.exists()||file.delete()){db.deletePlan(p.id);show();}else toast("文档删除失败，请重试");}).show()));add(doc,actions);}
        if(db.plans().isEmpty()){LinearLayout empty=card(content);add(empty,label("还没有计划文档。你可以先添加打卡项，之后再导入 PDF。"));}
        section("补充习惯","可选的重复打卡");add(content,action("＋  手动添加打卡项",false,()->taskDialog()));gap(content,14);
        for(FitnessDb.Row t:db.tasks()){LinearLayout task=card(content);add(task,text(t.title,16,INK,true));gap(task,6);String days="";for(int i=1;i<=7;i++)if(t.days.contains(""+i))days+="周"+weekdays[i-1]+" ";add(task,label((t.kind.equals("diet")?"饮食 · ":"训练 · ")+(t.days.length()==7?"每天":days)));gap(task,8);TextView remove=text("删除打卡项",12,MUTED,false);remove.setPadding(0,dp(8),0,dp(8));remove.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("删除打卡项？").setMessage("历史完成记录会保留。").setNegativeButton("取消",null).setPositiveButton("删除",(a,b)->{db.deleteTask(t.id);show();}).show());add(task,remove);task.setOnClickListener(v->taskDialog(t));}
    }
    private void taskDialog(){taskDialog(null);}
    private void taskDialog(FitnessDb.Row existing){
        LinearLayout f=form();
        EditText title=field(f,"打卡内容（必填）",existing==null?"":existing.title,false);
        Spinner kind=new Spinner(this);
        kind.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"训练","饮食"}));
        kind.setSelection(existing!=null&&existing.kind.equals("diet")?1:0);kind.setVisibility(View.GONE);add(f,kind);
        add(f,label("项目类型"));gap(f,8);LinearLayout categories=row();TextView[] categoryButtons=new TextView[2];
        Runnable repaint=()->{for(int i=0;i<2;i++){boolean selectedType=kind.getSelectedItemPosition()==i;categoryButtons[i].setBackground(shape(selectedType?GREEN:0xffEAF0E5,14));categoryButtons[i].setTextColor(selectedType?Color.WHITE:GREEN);categoryButtons[i].setSelected(selectedType);}};
        for(int i=0;i<2;i++){final int index=i;categoryButtons[i]=action(i==0?"训练":"饮食",false,()->{kind.setSelection(index);repaint.run();});weighted(categories,categoryButtons[i]);}repaint.run();add(f,categories);gap(f,20);add(f,label("重复日期"));gap(f,8);
        LinearLayout week=row();CheckBox[] boxes=new CheckBox[7];
        for(int i=0;i<7;i++){
            CheckBox box=new CheckBox(this);boxes[i]=box;box.setText(weekdays[i]);box.setTextSize(11);box.setButtonDrawable(null);
            box.setChecked(existing==null||existing.days.contains(""+(i+1)));
            box.setGravity(Gravity.CENTER);box.setBackground(shape(box.isChecked()?GREEN:0xffEAF0E5,12));box.setTextColor(box.isChecked()?Color.WHITE:GREEN);
            box.setOnCheckedChangeListener((b,on)->{box.setBackground(shape(on?GREEN:0xffEAF0E5,12));box.setTextColor(on?Color.WHITE:GREEN);});
            LinearLayout.LayoutParams wp=new LinearLayout.LayoutParams(0,dp(44),1);wp.setMargins(dp(2),0,dp(2),0);week.addView(box,wp);
        }
        add(f,week);gap(f,10);add(f,label("修改仅影响计划安排，历史完成记录保留原始内容。"));
        AlertDialog d=dialog(existing==null?"添加打卡项":"编辑打卡项",f);d.show();
        d.getButton(-1).setOnClickListener(v->{
            String s=title.getText().toString().trim(),days="";
            if(s.isEmpty()){title.setError("请填写内容");return;}
            for(int i=0;i<7;i++)if(boxes[i].isChecked())days+=(i+1);
            if(days.isEmpty()){toast("至少选择一天");return;}
            String type=kind.getSelectedItemPosition()==0?"training":"diet";
            if(existing==null)db.addTask(s,type,days);else db.updateTask(existing.id,s,type,days);
            d.dismiss();show();
        });
    }
    @Override protected void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);if(req==101&&result==RESULT_OK&&data!=null&&data.getData()!=null){saveExport(data.getData());return;}if(req==100&&result==RESULT_OK&&data!=null&&data.getData()!=null)importPdf(data.getData());}
    private void importPdf(Uri uri){
        importing=true;show();worker.execute(()->{File file=null;try{String name="健身计划.pdf";try(Cursor cursor=getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){if(cursor!=null&&cursor.moveToFirst())name=cursor.getString(0);}File dir=new File(getFilesDir(),"plans");if(!dir.exists()&&!dir.mkdirs())throw new IOException("无法创建文档目录");boolean word=name.toLowerCase(Locale.ROOT).endsWith(".docx");file=new File(dir,UUID.randomUUID()+(word?".docx":".pdf"));try(InputStream in=getContentResolver().openInputStream(uri);OutputStream out=new FileOutputStream(file)){if(in==null)throw new IOException("无法读取文件");byte[] buffer=new byte[16384];long total=0;int n;while((n=in.read(buffer))!=-1){total+=n;if(total>30L*1024*1024)throw new IOException("文件超过 30 MB，请压缩后重试");out.write(buffer,0,n);}}
            int pages=0;if(word){readDocx(file);}else try(ParcelFileDescriptor fd=ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY);PdfRenderer renderer=new PdfRenderer(fd)){pages=renderer.getPageCount();if(pages<1)throw new IOException("PDF 没有页面");}
            db.addPlan(name,file.getAbsolutePath(),(word?"Word 文档":pages+" 页")+" · "+size(file.length())+" · "+LocalDate.now()+" 导入");runOnUiThread(()->{importing=false;if(!isDestroyed()){toast("文档已保存，请点击提取训练日");show();}});
        }catch(Exception e){if(file!=null)file.delete();String message=e instanceof SecurityException?"PDF 已加密或无访问权限":e.getMessage();runOnUiThread(()->{importing=false;if(!isDestroyed()){toast("导入失败："+message);show();}});}});
    }
    private void extractPlan(FitnessDb.Row p){
        ProgressDialog progress=ProgressDialog.show(this,"提取打卡建议","正在读取 PDF 文字…",true,false);extractionProgress=progress;
        worker.execute(()->{try{StringBuilder text=new StringBuilder();boolean limited=false;if(p.path.endsWith(".docx")){text.append(readDocx(new File(p.path)));}else try(ParcelFileDescriptor fd=ParcelFileDescriptor.open(new File(p.path),ParcelFileDescriptor.MODE_READ_ONLY);PdfRenderer renderer=new PdfRenderer(fd)){int count=Math.min(renderer.getPageCount(),100);limited=count<renderer.getPageCount();for(int i=0;i<count;i++){try(PdfRenderer.Page page=renderer.openPage(i)){for(PdfPageTextContent t:page.getTextContents()){text.append(t.getText()).append('\n');if(text.length()>500000){limited=true;break;}}}if(text.length()>500000)break;}}
            java.util.List<TrainingPlan.Day> days=TrainingPlan.parse(text.toString());
            if(!days.isEmpty()){runOnUiThread(()->{if(!isDestroyed()){progress.dismiss();confirmTrainingImport(days);}});return;}
            if(text.toString().contains("[DAY]")){runOnUiThread(()->{if(!isDestroyed()){progress.dismiss();new AlertDialog.Builder(this).setTitle("请先填写模板").setMessage("没有识别到已填写的训练项目。请替换花括号占位文字，并保留 DAY、ITEM 和结束标记，再上传文档。").setPositiveButton("知道了",null).show();}});return;}
            runOnUiThread(()->{if(!isDestroyed()){progress.dismiss();new AlertDialog.Builder(this).setTitle("未识别到标准训练日").setMessage("请下载标准模板，在 Word 中填写 DAY 与 ITEM 区块后上传。普通 PDF 可继续阅读，也可在训练日库手动创建训练日。扫描件暂不支持识别。").setPositiveButton("知道了",null).show();}});        }catch(Exception e){runOnUiThread(()->{if(!isDestroyed()){progress.dismiss();toast("提取失败，请尝试阅读原文或重新导入");}});}});
    }
    private void openPdf(FitnessDb.Row p){pdfPreview=new PdfPreview(p);pdfPreview.show();}
    private class PdfPreview extends Dialog {
        final FitnessDb.Row plan;int pageIndex=0;boolean loading=false,closed=false;ImageView image;TextView pageLabel,back,next;Bitmap displayed;
        PdfPreview(FitnessDb.Row plan){super(MainActivity.this);this.plan=plan;}
        @Override protected void onCreate(Bundle b){super.onCreate(b);LinearLayout body=column();body.setPadding(dp(16),dp(16),dp(16),dp(16));body.setBackgroundColor(BG);add(body,text(plan.title,18,INK,true));gap(body,10);ScrollView scroll=new ScrollView(MainActivity.this);image=new ImageView(MainActivity.this);image.setAdjustViewBounds(true);scroll.addView(image);body.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));LinearLayout controls=row();back=action("上一页",false,()->{if(!loading&&pageIndex>0){pageIndex--;load();}});next=action("下一页",false,()->{if(!loading){pageIndex++;load();}});weighted(controls,back);pageLabel=label("读取中");weighted(controls,pageLabel);weighted(controls,next);add(body,controls);gap(body,10);add(body,action("关闭文档",true,this::dismiss));setContentView(body);getWindow().setLayout(-1,-1);setOnDismissListener(d->{closed=true;image.setImageDrawable(null);if(displayed!=null){displayed.recycle();displayed=null;}});load();}
        private void load(){loading=true;back.setEnabled(false);next.setEnabled(false);worker.execute(()->{try{Bitmap bitmap;int count;try(ParcelFileDescriptor fd=ParcelFileDescriptor.open(new File(plan.path),ParcelFileDescriptor.MODE_READ_ONLY);PdfRenderer r=new PdfRenderer(fd)){count=r.getPageCount();pageIndex=Math.max(0,Math.min(pageIndex,count-1));try(PdfRenderer.Page page=r.openPage(pageIndex)){float scale=Math.min(1600f/page.getWidth(),2200f/page.getHeight());int w=Math.max(1,(int)(page.getWidth()*scale)),h=Math.max(1,(int)(page.getHeight()*scale));bitmap=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);bitmap.eraseColor(Color.WHITE);page.render(bitmap,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);}}Bitmap result=bitmap;int pages=count;runOnUiThread(()->{loading=false;if(closed||isDestroyed()){result.recycle();return;}Bitmap old=displayed;displayed=result;image.setImageBitmap(result);if(old!=null)old.recycle();pageLabel.setText((pageIndex+1)+" / "+pages);back.setEnabled(pageIndex>0);next.setEnabled(pageIndex<pages-1);});}catch(Exception e){runOnUiThread(()->{loading=false;if(!closed){toast("无法阅读此 PDF，请重新导入");dismiss();}});}});}
    }
    private final String[] mealNames={"早餐","午餐","晚餐","加餐"};
    private boolean isMeal(FitnessDb.Row r){return r.kind.equals("diet")&&Arrays.asList(mealNames).contains(r.title);}
    private void mealChooser(java.util.List<FitnessDb.Row> rows){new AlertDialog.Builder(this).setTitle("选择餐次").setItems(mealNames,(d,w)->editMeal(mealNames[w],rows)).show();}
    private void editMeal(String name,java.util.List<FitnessDb.Row> rows){
        FitnessDb.Row found=null;for(FitnessDb.Row r:rows)if(r.kind.equals("diet")&&r.title.equals(name)){found=r;break;}
        final FitnessDb.Row existing=found;LinearLayout f=form();add(f,label(selected+" · "+name));gap(f,10);
        EditText food=field(f,"吃了什么及份量（必填）",existing==null?"":existing.note,false);
        EditText calories=field(f,"热量 kcal（选填）",existing==null||existing.value==0?"":number(existing.value),true);
        AlertDialog dialog=dialog("记录"+name,f);if(existing!=null)dialog.setButton(-3,"删除",(d,w)->confirmDelete(existing));dialog.show();
        dialog.getButton(-1).setOnClickListener(v->{if(food.getText().toString().trim().isEmpty()){food.setError("请填写饮食内容");return;}try{double kcal=numeric(calories,0,20000,true);FitnessDb.Row r=existing==null?new FitnessDb.Row():existing;r.day=selected.toString();r.kind="diet";r.title=name;r.note=food.getText().toString().trim();r.value=kcal;db.saveRecord(r);dialog.dismiss();show();}catch(IllegalArgumentException ignored){}});
    }
    private void mealPanel(java.util.List<FitnessDb.Row> rows){
        section("三餐与加餐","自己填写饮食");
        for(String name:mealNames){FitnessDb.Row found=null;for(FitnessDb.Row r:rows)if(r.kind.equals("diet")&&r.title.equals(name)){found=r;break;}
            LinearLayout c=card(content);add(c,text(name+(found==null?"  ＋":"  ✓"),16,INK,true));gap(c,4);add(c,label(found==null?"点击填写食物与份量":found.note+(found.value>0?" · "+number(found.value)+" kcal":"")));c.setOnClickListener(v->editMeal(name,rows));
        }
    }
    private FitnessDb.Row findSession(java.util.List<FitnessDb.Row> rows){for(FitnessDb.Row r:rows)if(r.kind.equals("session"))return r;return null;}
    private JSONArray sessionItems(FitnessDb.Row r){try{return new JSONArray(r.note);}catch(JSONException e){return new JSONArray();}}
    private void trainingPanel(java.util.List<FitnessDb.Row> rows){
        FitnessDb.Row session=findSession(rows);section("今日训练日",session==null?"先选类型，再打卡":null);
        LinearLayout c=card(content);
        if(session!=null){add(c,text(session.title,21,INK,true));gap(c,8);}
        add(c,action(session==null?"选择今日训练日  ↓":"切换训练日",false,()->chooseTrainingDay(session)));
        if(session==null){gap(c,10);add(c,label("先在计划页上传模板或创建训练日。训练频率由你安排，不限制每周训练次数。"));return;}
        JSONArray items=sessionItems(session);gap(c,12);int completed=0;for(int i=0;i<items.length();i++)if(items.optJSONObject(i).optBoolean("done"))completed++;
        if(items.length()==0){add(c,text("休息日 · 今天好好恢复",17,INK,true));return;}
        TextView summary=label("已完成 "+completed+" / "+items.length()+" 项 · 按自己的节奏训练");add(c,summary);
        java.util.List<CheckBox> checks=new ArrayList<>();boolean[] updating={false};
        Runnable refresh=()->{int count=0;updating[0]=true;try{for(int i=0;i<checks.size();i++){boolean on=items.optJSONObject(i).optBoolean("done");checks.get(i).setChecked(on);checks.get(i).setText(on?"已完成":"完成");if(on)count++;}}finally{updating[0]=false;}summary.setText("已完成 "+count+" / "+items.length()+" 项 · 按自己的节奏训练");refreshProgress();};
        for(int i=0;i<items.length();i++){
            JSONObject item=items.optJSONObject(i);final int index=i;boolean done=item.optBoolean("done");
            LinearLayout exercise=card(content);LinearLayout status=row();
            TextView order=label(String.format(Locale.ROOT,"动作 %02d",i+1));status.addView(order,new LinearLayout.LayoutParams(0,-2,1));
            CheckBox box=new CheckBox(this);box.setText(done?"已完成":"完成");box.setTextSize(13);box.setTextColor(GREEN);box.setMinHeight(dp(48));box.setChecked(done);box.setContentDescription("完成 "+item.optString("title"));status.addView(box);add(exercise,status);gap(exercise,4);
            String[] parts=item.optString("title").split("[|｜]",2);add(exercise,text(parts[0].trim(),18,INK,true));
            if(parts.length>1){gap(exercise,10);TextView prescription=text(parts[1].trim(),15,GREEN,false);prescription.setLineSpacing(dp(5),1);add(exercise,prescription);}
            gap(exercise,16);View divider=new View(this);divider.setBackgroundColor(LINE);exercise.addView(divider,new LinearLayout.LayoutParams(-1,dp(1)));gap(exercise,12);
            if(!item.optString("actual").isEmpty()){add(exercise,label("实际记录"));gap(exercise,4);add(exercise,text(item.optString("actual"),15,INK,false));gap(exercise,10);}
            add(exercise,action(item.optString("actual").isEmpty()?"＋ 记录实际训练":"编辑实际记录",false,()->{LinearLayout f=form();add(f,text(item.optString("title"),17,INK,true));gap(f,16);EditText actual=field(f,"实际组次、负重、时长或备注",item.optString("actual"),false);AlertDialog d=dialog("补充实际训练",f);d.show();d.getButton(-1).setOnClickListener(b->{try{item.put("actual",actual.getText().toString().trim());session.note=items.toString();db.saveRecord(session);d.dismiss();showKeepingScroll();}catch(JSONException ignored){}});}));
            checks.add(box);box.setOnCheckedChangeListener((v,on)->{if(updating[0])return;try{items.getJSONObject(index).put("done",on);session.note=items.toString();db.saveRecord(session);refresh.run();}catch(JSONException ignored){}});
        }
        add(content,action("一键完成本日所有项目",true,()->{try{for(int i=0;i<items.length();i++)items.getJSONObject(i).put("done",true);session.note=items.toString();db.saveRecord(session);refresh.run();}catch(JSONException ignored){}}));gap(content,18);
    }
    private void refreshProgress(){
        java.util.List<FitnessDb.Row> records=dayRows(),tasks=dueTasks();int done=0,total=tasks.size();
        for(FitnessDb.Row t:tasks)for(FitnessDb.Row r:records)if(r.taskId==t.id)done++;
        FitnessDb.Row session=findSession(records);if(session!=null){JSONArray items=sessionItems(session);total+=items.length();for(int i=0;i<items.length();i++)if(items.optJSONObject(i).optBoolean("done"))done++;}
        progressCount.setText(done+" / "+total);progressHint.setText(total==0?"从你的第一份计划开始":done==total?"今日计划已完成，做得很好":"每一次打卡，都是一次积累");progressRing.update(total==0?0:(float)done/total);
    }
    private void showKeepingScroll(){ScrollView before=(ScrollView)content.getParent();int y=before.getScrollY();show();ScrollView after=(ScrollView)content.getParent();after.getViewTreeObserver().addOnPreDrawListener(new android.view.ViewTreeObserver.OnPreDrawListener(){public boolean onPreDraw(){after.getViewTreeObserver().removeOnPreDrawListener(this);after.scrollTo(0,y);return true;}});}
    private void chooseTrainingDay(FitnessDb.Row existing){
        java.util.List<FitnessDb.Row> days=db.trainingDays();String[] names=new String[days.size()+1];for(int i=0;i<days.size();i++)names[i]=days.get(i).title;names[days.size()]="休息日";
        new AlertDialog.Builder(this).setTitle("选择当天训练类型").setItems(names,(d,w)->{
            Runnable apply=()->{try{JSONArray items=new JSONArray();if(w<days.size())for(String item:days.get(w).note.split("\\n"))if(!item.trim().isEmpty()){JSONObject o=new JSONObject();o.put("title",item);o.put("done",false);o.put("actual","");items.put(o);}FitnessDb.Row r=existing==null?new FitnessDb.Row():existing;r.kind="session";r.day=selected.toString();r.title=names[w];r.note=items.toString();db.saveRecord(r);show();}catch(JSONException ignored){}};
            if(existing==null)apply.run();else new AlertDialog.Builder(this).setTitle("切换当天训练日？").setMessage("将替换当天训练项目及勾选状态。饮食、身体指标和其他记录保留。").setNegativeButton("取消",null).setPositiveButton("切换",(a,b)->apply.run()).show();
        }).show();
    }
    private void trainingLibrary(){
        section("训练日库","每天选一次即可");add(content,action("＋  创建训练日",false,()->trainingDayEditor(null)));gap(content,12);
        for(FitnessDb.Row day:db.trainingDays()){
            LinearLayout c=card(content);add(c,text(day.title,20,INK,true));gap(c,8);
            String[] items=day.note.trim().isEmpty()?new String[0]:day.note.split("\\n");add(c,label(items.length+" 个训练项目"));gap(c,12);
            for(int i=0;i<Math.min(3,items.length);i++){add(c,text(String.format(Locale.ROOT,"%02d  ",i+1)+items[i].split("[|｜]",2)[0].trim(),15,INK,false));gap(c,8);}
            if(items.length>3)add(c,label("另有 "+(items.length-3)+" 项，进入编辑查看全部"));
            gap(c,12);LinearLayout buttons=row();weighted(buttons,action("编辑项目",false,()->trainingDayEditor(day)));weighted(buttons,action("删除",false,()->new AlertDialog.Builder(this).setTitle("删除训练日模板？").setMessage("已选择到具体日期的训练快照会保留。").setNegativeButton("取消",null).setPositiveButton("删除",(d,w)->{db.deleteTrainingDay(day.id);show();}).show()));add(c,buttons);
        }
    }
    private void trainingDayEditor(FitnessDb.Row existing){
        TrainingDayForm f=new TrainingDayForm(this,existing==null?"":existing.title,existing==null?"":existing.note);
        FormDialog d=new FormDialog(this,existing==null?"创建训练日":"编辑训练日",f).expanded();trackSheet(d);d.show();
        d.getButton(-1).setOnClickListener(v->{try{String title=f.title(),body=f.body();FitnessDb.Row r=existing==null?new FitnessDb.Row():existing;r.title=title;r.note=body;db.saveTrainingDay(r);d.dismiss();showKeepingScroll();}catch(IllegalArgumentException ignored){}});
    }
    private void confirmTrainingImport(java.util.List<TrainingPlan.Day> days){
        String[] names=new String[days.size()];boolean[] selectedDays=new boolean[days.size()];for(int i=0;i<days.size();i++){TrainingPlan.Day day=days.get(i);names[i]=day.name+" · "+day.items.size()+" 项";}
        new AlertDialog.Builder(this).setTitle("确认导入训练日").setMultiChoiceItems(names,selectedDays,(d,w,on)->selectedDays[w]=on).setNegativeButton("取消",null).setPositiveButton("导入所选",(d,w)->{java.util.List<TrainingPlan.Day> chosen=new ArrayList<>();for(int i=0;i<days.size();i++)if(selectedDays[i]&&!days.get(i).items.isEmpty())chosen.add(days.get(i));db.importTrainingDays(chosen);toast("已导入 "+chosen.size()+" 个训练日；休息日可直接选择");show();}).show();
    }
    static String readDocx(File file)throws Exception{
        try(ZipFile zip=new ZipFile(file)){ZipEntry entry=zip.getEntry("word/document.xml");if(entry==null)throw new IOException("不是有效的 Word 文档");try(InputStream raw=zip.getInputStream(entry)){
            ByteArrayOutputStream limited=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int n;while((n=raw.read(buffer))!=-1){if(limited.size()+n>5*1024*1024)throw new IOException("Word 正文过大");limited.write(buffer,0,n);}
            XmlPullParser parser=android.util.Xml.newPullParser();parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES,true);parser.setInput(new ByteArrayInputStream(limited.toByteArray()),"UTF-8");StringBuilder text=new StringBuilder();boolean inside=false;
            for(int event=parser.getEventType();event!=XmlPullParser.END_DOCUMENT;event=parser.next()){
                if(event==XmlPullParser.START_TAG&&parser.getName().equals("t"))inside=true;
                else if(event==XmlPullParser.TEXT&&inside)text.append(parser.getText());
                else if(event==XmlPullParser.END_TAG){if(parser.getName().equals("t"))inside=false;if(parser.getName().equals("p"))text.append('\n');}
            }return text.toString();
        }}
    }
    private void exportTemplate(String asset){exportAsset=asset;Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType(asset.endsWith(".pdf")?"application/pdf":"application/vnd.openxmlformats-officedocument.wordprocessingml.document");i.putExtra(Intent.EXTRA_TITLE,asset);startActivityForResult(i,101);}
    private void saveExport(Uri uri){String asset=exportAsset;if(asset==null){toast("请重新选择要下载的模板");return;}worker.execute(()->{try(InputStream in=getAssets().open(asset);OutputStream out=getContentResolver().openOutputStream(uri)){if(out==null)throw new IOException();byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);runOnUiThread(()->toast("模板已保存，请填写后上传"));}catch(Exception e){runOnUiThread(()->toast("保存失败，请重试"));}});}

    private String number(double n){return n==(long)n?String.valueOf((long)n):String.format(Locale.CHINA,"%.1f",n);}
    private String size(long n){if(n<1024)return n+" B";if(n<1024*1024)return String.format(Locale.CHINA,"%.1f KB",n/1024.0);return String.format(Locale.CHINA,"%.2f MB",n/1048576.0);}
    private void trends(){
        header("SMALL STEPS, REAL CHANGE","看见自己的每一点进步。","用真实的记录，了解身体与习惯的变化。");
        LinearLayout ranges=row();int[] values={7,30,90,365};for(int n:values)weighted(ranges,action(n==365?"1 年":n+" 天",trendDays==n,()->{trendDays=n;show();}));add(content,ranges);gap(content,18);
        LocalDate start=LocalDate.now().minusDays(trendDays-1);java.util.List<FitnessDb.Row> rows=db.records(start.toString(),LocalDate.now().toString()),metrics=new ArrayList<>();int train=0,checks=0;double minutes=0;Set<String> active=new HashSet<>();
        for(FitnessDb.Row r:rows){active.add(r.day);if(r.kind.equals("metric")&&(trendMetric==0?r.value:trendMetric==1?r.extra:r.third)>0)metrics.add(r);if(r.kind.equals("training")){train++;minutes+=r.value;}if(r.kind.equals("check"))checks++;if(r.kind.equals("session")){JSONArray items=sessionItems(r);for(int j=0;j<items.length();j++)if(items.optJSONObject(j).optBoolean("done"))checks++;if(items.length()>0)train++;}}Collections.reverse(metrics);
        LinearLayout c=card(content);LinearLayout switches=row();String[] names={"体重","体脂","腰围"};for(int i=0;i<3;i++){final int m=i;weighted(switches,action(names[i],trendMetric==i,()->{trendMetric=m;show();}));}add(c,switches);gap(c,22);
        String unit=trendMetric==0?"kg":trendMetric==1?"%":"cm";
        if(metrics.isEmpty()){add(c,text("— "+unit,32,INK,true));gap(c,8);add(c,label("这个时间段还没有"+names[trendMetric]+"数据"));gap(c,20);add(c,action("记录身体指标",false,()->{selected=LocalDate.now();metricDialog(null);}));}
        else {double latest=metricValue(metrics.get(metrics.size()-1)),diff=latest-metricValue(metrics.get(0));add(c,text(number(latest)+" "+unit,36,INK,true));add(c,label(metrics.get(metrics.size()-1).day+" 最近记录 · 较本期首次 "+(diff>0?"+":"")+number(diff)+" "+unit));gap(c,18);Chart chart=new Chart(metrics,trendMetric,start,LocalDate.now());c.addView(chart,new LinearLayout.LayoutParams(-1,dp(200)));if(metrics.size()==1)add(c,label("再记录一次，就能看见变化曲线。"));LinearLayout endpoints=row();TextView first=label(start.toString()),last=label(LocalDate.now().toString());endpoints.addView(first,new LinearLayout.LayoutParams(0,-2,1));endpoints.addView(last);add(c,endpoints);}
        LinearLayout stats=row();LinearLayout a=cardDetached(active.size()+" 天","有记录的日子"),b=cardDetached(number(minutes)+" 分钟","累计训练时长");weighted(stats,a);weighted(stats,b);add(content,stats);gap(content,14);LinearLayout summary=card(content);add(summary,text("习惯正在发生",18,INK,true));gap(summary,10);add(summary,label("本期记录训练 "+train+" 次，完成计划打卡 "+checks+" 项。"));add(summary,label("趋势仅展示实际填写的数据，缺失日期不会补值。"));section("指标历史","最近 30 条");int count=0;for(FitnessDb.Row r:rows){if(!r.kind.equals("metric"))continue;LinearLayout item=card(content);add(item,text(r.day+"     "+number(r.value)+" kg",16,INK,true));add(item,label("体脂 "+(r.extra>0?number(r.extra)+"%":"未填")+" · 腰围 "+(r.third>0?number(r.third)+" cm":"未填")));item.setOnClickListener(v->{selected=LocalDate.parse(r.day);metricDialog(r);});if(++count==30)break;}if(count==0)add(content,label("暂无指标记录"));
    }
    private double metricValue(FitnessDb.Row r){return trendMetric==0?r.value:trendMetric==1?r.extra:r.third;}
    private LinearLayout cardDetached(String value,String title){LinearLayout l=column();l.setPadding(dp(16),dp(18),dp(16),dp(18));l.setBackground(shape(0xffE8EEDF,18));add(l,text(value,23,INK,true));gap(l,6);add(l,label(title));return l;}
    private long folderSize(File f){if(!f.exists())return 0;if(f.isFile())return f.length();long n=0;File[] files=f.listFiles();if(files!=null)for(File child:files)n+=folderSize(child);return n;}
    private void settings(){
        header("LOCAL FIRST, ALWAYS YOURS","数据，由你掌握。","无需账号，你的计划与记录只保存在本机。");
        long documents=folderSize(new File(getFilesDir(),"plans")),database=folderSize(getDatabasePath("fitness.db").getParentFile()),cache=folderSize(getCacheDir())+folderSize(getCodeCacheDir()),other=folderSize(getFilesDir())-documents+folderSize(new File(getApplicationInfo().dataDir,"shared_prefs"));
        LinearLayout storage=card(content);add(storage,text("本地数据占用",16,INK,true));gap(storage,8);add(storage,text(size(documents+database+cache+other),36,INK,true));gap(storage,18);storageLine(storage,"计划文档（Word / PDF）",size(documents));storageLine(storage,"记录数据库（含日志）",size(database));storageLine(storage,"缓存",size(cache));storageLine(storage,"其他应用数据",size(other));gap(storage,12);add(storage,label("以上为应用私有目录的文件大小；系统分配空间可能略有不同。"));
        LinearLayout resource=card(content);add(resource,text("运行资源",18,INK,true));gap(resource,12);android.os.Debug.MemoryInfo memory=new android.os.Debug.MemoryInfo();android.os.Debug.getMemoryInfo(memory);storageLine(resource,"进程内存（PSS 快照）",size(memory.getTotalPss()*1024L));storageLine(resource,"当前进程累计 CPU 时间",String.format(Locale.CHINA,"%.2f 秒",android.os.Process.getElapsedCpuTime()/1000.0));storageLine(resource,"应用安装包（基础 APK）",size(new File(getApplicationInfo().sourceDir).length()));storageLine(resource,"后台服务 / 网络上传","无 / 无");gap(resource,10);add(resource,label("内存和 CPU 是打开此页时的快照；耗电请在系统应用设置中查看。"));gap(resource,10);add(resource,action("刷新资源数据",false,this::show));
        section("清理历史记录","保留计划文件");LinearLayout clear=card(content);add(clear,label("删除所选日期内的饮食、训练、身体指标与计划完成记录。文档、训练日模板和重复打卡项可在「计划」中单独移除。"));gap(clear,14);
        Spinner choices=new Spinner(this);choices.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,Retention.LABELS));choices.setSelection(cleanup);add(clear,choices);gap(clear,12);TextView preview=label("");add(clear,preview);choices.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){}public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){cleanup=pos;String start=Retention.start(LocalDate.now(),pos).toString(),end=LocalDate.now().toString();preview.setText((pos==7?"全部日期":start+" 至 "+end)+" · "+db.records(start,end).size()+" 条记录");}});gap(clear,14);TextView delete=action("清理所选记录",false,()->cleanupDialog());delete.setTextColor(0xffAF574B);delete.setBackground(shape(0xffFAEEE8,14));add(clear,delete);
        gap(content,10);add(content,text("循迹  v1.3.1",14,GREEN,true));gap(content,4);add(content,label("本地优先 · 无广告 · 无联网权限\n卸载应用会删除本地数据，请妥善保管原始 PDF。"));
    }
    private void storageLine(LinearLayout p,String title,String value){LinearLayout r=row();r.setPadding(0,dp(7),0,dp(7));r.addView(label(title),new LinearLayout.LayoutParams(0,-2,1));r.addView(text(value,13,INK,true));add(p,r);}
    private void cleanupDialog(){String start=Retention.start(LocalDate.now(),cleanup).toString(),end=LocalDate.now().toString();int count=db.records(start,end).size();if(count==0){toast("该时间范围内没有记录");return;}new AlertDialog.Builder(this).setTitle("清理"+Retention.LABELS[cleanup]+"？").setMessage("将永久删除 "+count+" 条记录。\n"+(cleanup==7?"范围：全部日期":"范围："+start+" 至 "+end)+"\n文档、训练日模板和重复打卡项会保留。删除后无法恢复。").setNegativeButton("取消",null).setPositiveButton("确认删除 "+count+" 条",(d,w)->{try{int n=db.clearRecords(start,end);toast("已清理 "+n+" 条记录");show();}catch(Exception e){toast("清理失败，请重试");}}).show();}
    private class Ring extends View {
        float progress;final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        final RectF bounds=new RectF();
        final Typeface face=Typeface.create("sans-serif-medium",Typeface.NORMAL);
        String percentage;
        Ring(float progress){super(MainActivity.this);update(progress);}
        void update(float progress){this.progress=progress;percentage=Math.round(progress*100)+"%";setContentDescription("计划完成 "+percentage);invalidate();}
        @Override protected void onDraw(Canvas c){
            super.onDraw(c);float w=getWidth(),h=getHeight();bounds.set(dp(7),dp(7),w-dp(7),h-dp(7));
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(7));p.setColor(0xff426050);c.drawOval(bounds,p);
            p.setColor(0xffD2E7A5);p.setStrokeCap(Paint.Cap.ROUND);c.drawArc(bounds,-90,progress*360,false,p);
            p.setStyle(Paint.Style.FILL);p.setColor(Color.WHITE);p.setTypeface(face);p.setTextSize(dp(22));p.setTextAlign(Paint.Align.CENTER);
            c.drawText(percentage,w/2,h/2+dp(8),p);
        }
    }
    private class Chart extends View {
        final java.util.List<FitnessDb.Row> data;final int metric;final LocalDate start,end;final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        final Path path=new Path();final java.util.List<PointF> points=new ArrayList<>();final String[] labels=new String[4];
        double min,max;float left,right,top,bottom;
        Chart(java.util.List<FitnessDb.Row> data,int metric,LocalDate start,LocalDate end){
            super(MainActivity.this);this.data=data;this.metric=metric;this.start=start;this.end=end;
            setContentDescription("身体指标变化趋势，具体数值见下方指标历史");
            min=Double.MAX_VALUE;max=-Double.MAX_VALUE;
            for(FitnessDb.Row r:data){min=Math.min(min,value(r));max=Math.max(max,value(r));}
            double padding=Math.max((max-min)*.25,.5);min-=padding;max+=padding;
            for(int i=0;i<4;i++)labels[i]=String.format(Locale.CHINA,"%.1f",max-(max-min)*i/3);
        }
        private double value(FitnessDb.Row r){return metric==0?r.value:metric==1?r.extra:r.third;}
        @Override protected void onSizeChanged(int w,int h,int oldW,int oldH){
            super.onSizeChanged(w,h,oldW,oldH);left=dp(38);right=w-dp(10);top=dp(15);bottom=h-dp(15);points.clear();path.reset();
            long days=Math.max(1,java.time.temporal.ChronoUnit.DAYS.between(start,end));
            for(int i=0;i<data.size();i++){FitnessDb.Row r=data.get(i);float x=left+(right-left)*java.time.temporal.ChronoUnit.DAYS.between(start,LocalDate.parse(r.day))/days,y=bottom-(float)((value(r)-min)/(max-min))*(bottom-top);points.add(new PointF(x,y));if(i==0)path.moveTo(x,y);else path.lineTo(x,y);}
        }
        @Override protected void onDraw(Canvas c){super.onDraw(c);
            paint.setTypeface(Typeface.DEFAULT);paint.setTextSize(dp(10));paint.setStrokeWidth(dp(1));
            for(int i=0;i<4;i++){float y=top+(bottom-top)*i/3;paint.setColor(LINE);c.drawLine(left,y,right,y,paint);paint.setColor(MUTED);paint.setTextAlign(Paint.Align.RIGHT);c.drawText(labels[i],left-dp(7),y+dp(3),paint);}
            paint.setColor(GREEN);paint.setStrokeWidth(dp(2.5f));paint.setStyle(Paint.Style.STROKE);c.drawPath(path,paint);paint.setStyle(Paint.Style.FILL);for(PointF point:points){paint.setColor(Color.WHITE);c.drawCircle(point.x,point.y,dp(5),paint);paint.setColor(GREEN);c.drawCircle(point.x,point.y,dp(3),paint);}
        }
    }
}
