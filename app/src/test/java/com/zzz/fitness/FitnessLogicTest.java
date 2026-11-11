package com.zzz.fitness;

import org.junit.Test;
import java.time.LocalDate;
import java.util.List;
import static org.junit.Assert.*;

public class FitnessLogicTest {
    @Test public void recentDaysIncludeTodayAndCrossYear() {
        LocalDate now=LocalDate.of(2026,1,2);
        assertEquals(now,Retention.start(now,0));
        assertEquals(LocalDate.of(2025,12,31),Retention.start(now,1));
        assertEquals(LocalDate.of(2025,12,27),Retention.start(now,2));
    }
    @Test public void calendarMonthsHandleLeapYearsAndShortMonths() {
        assertEquals(LocalDate.of(2024,3,1),Retention.start(LocalDate.of(2024,3,31),3));
        assertEquals(LocalDate.of(2025,3,1),Retention.start(LocalDate.of(2025,3,31),3));
        assertEquals(LocalDate.of(2025,11,29),Retention.start(LocalDate.of(2026,2,28),4));
        assertEquals(LocalDate.of(2026,5,12),Retention.start(LocalDate.of(2026,11,11),5));
        assertEquals(LocalDate.of(2025,11,12),Retention.start(LocalDate.of(2026,11,11),6));
    }
    @Test public void allTimeHasStableLowerBound() {
        assertEquals(LocalDate.of(1970,1,1),Retention.start(LocalDate.now(),7));
    }
    @Test public void parserDeduplicatesAndClassifiesSuggestions() {
        List<PlanParser.Suggestion> list=PlanParser.parse("健身计划\n• 深蹲 3 组 × 10 次\n早餐：燕麦与鸡蛋\n深蹲 3 组 × 10 次\nKeep going\nLunch: chicken and vegetables");
        assertEquals(3,list.size());
        assertEquals("training",list.get(0).kind);
        assertEquals("diet",list.get(1).kind);
        assertEquals("diet",list.get(2).kind);
    }
    @Test public void parserHandlesEmptyAndCapsSuggestions() {
        assertTrue(PlanParser.parse("").isEmpty());
        StringBuilder s=new StringBuilder();for(int i=0;i<100;i++)s.append("跑步训练 ").append(i).append('\n');
        assertEquals(60,PlanParser.parse(s.toString()).size());
    }
}
