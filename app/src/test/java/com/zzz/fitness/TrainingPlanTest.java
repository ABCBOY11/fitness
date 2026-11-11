package com.zzz.fitness;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
public class TrainingPlanTest {
    @Test public void supportsAnyNumberOfTrainingDays(){
        for(int count:new int[]{3,4,5}){
            StringBuilder text=new StringBuilder();for(int i=0;i<count;i++)text.append("[DAY] 训练 ").append(i).append("\n[ITEM] 动作 | 3组 | 无 | 无 | 无\n[/DAY]\n");
            assertEquals(count,TrainingPlan.parse(text.toString()).size());
        }
    }
    @Test public void preservesWrappedItemsAndSkipsPlaceholders(){
        List<TrainingPlan.Day> days=TrainingPlan.parse("[DAY] 力量 A\n[ITEM] 深蹲 | 3组8次 |\n自选 | 90秒 | 无\n[ITEM] {动作名称} | {组数}\n[/DAY]\n[DAY] 休息日\n[/DAY]");
        assertEquals(2,days.size());assertEquals(1,days.get(0).items.size());assertTrue(days.get(0).items.get(0).contains("自选"));assertTrue(days.get(1).items.isEmpty());
    }
    @Test public void ignoresUnclosedAndBlankBlocks(){
        assertTrue(TrainingPlan.parse("[DAY] 力量 A\n[ITEM] {动作名称}\n[/DAY]").isEmpty());
        assertTrue(TrainingPlan.parse("[DAY] 力量 A\n[ITEM] 跑步").isEmpty());
    }
}
