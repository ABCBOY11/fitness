package com.zzz.fitness;

import java.util.*;
import java.util.regex.*;

/** Stable text contract shared by the Word/PDF template and the app. */
public final class TrainingPlan {
    public static final class Day {
        public final String name;
        public final List<String> items;
        public Day(String name,List<String> items){this.name=name;this.items=new ArrayList<>(items);}
    }
    public static List<Day> parse(String text){
        List<Day> result=new ArrayList<>();
        Pattern blocks=Pattern.compile("\\[DAY\\][ \\t]*([^\\r\\n]+)\\r?\\n(.*?)\\[/DAY\\]",Pattern.DOTALL);
        Matcher block=blocks.matcher(text);
        Set<String> names=new HashSet<>();
        while(block.find()){
            String name=block.group(1).trim();
            if(name.isEmpty()||name.length()>60||name.contains("{")||!names.add(name))continue;
            List<String> items=new ArrayList<>();
            Matcher item=Pattern.compile("\\[ITEM\\]\\s*(.*?)(?=\\[ITEM\\]|$)",Pattern.DOTALL).matcher(block.group(2));
            while(item.find()){
                String value=item.group(1).replaceAll("[\\r\\n]+"," ").replaceAll("\\s+"," ").trim();
                if(!value.isEmpty()&&!value.contains("{")&&value.length()<=1000)items.add(value);
                if(items.size()==100)break;
            }
            if(!items.isEmpty()||name.contains("休息"))result.add(new Day(name,items));
            if(result.size()==30)break;
        }
        return result;
    }
}
