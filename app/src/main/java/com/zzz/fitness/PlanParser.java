package com.zzz.fitness;
import java.util.*;
public final class PlanParser {
    public static final class Suggestion {
        public final String title, kind;
        Suggestion(String title, String kind) { this.title=title; this.kind=kind; }
    }
    public static List<Suggestion> parse(String text) {
        List<Suggestion> out=new ArrayList<>(); Set<String> seen=new LinkedHashSet<>();
        for(String raw:text.split("[\\r\\n]+")) {
            String line=raw.replaceAll("^[\\s•●▪◆\\-]+", "").trim();
            if(line.length()<4 || line.length()>180 || !seen.add(line)) continue;
            String s=line.toLowerCase(Locale.ROOT), kind=null;
            if(s.matches(".*(早餐|午餐|晚餐|加餐|蛋白|蔬菜|燕麦|鸡胸|breakfast|lunch|dinner|protein).*")) kind="diet";
            else if(s.matches(".*(深蹲|卧推|硬拉|划船|跑步|步行|有氧|俯卧撑|平板支撑|拉伸|引体|训练|squat|deadlift|cardio|workout).*")) kind="training";
            if(kind!=null) out.add(new Suggestion(line,kind));
            if(out.size()==60) break;
        } return out;
    }
}
