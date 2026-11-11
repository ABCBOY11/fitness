package com.zzz.fitness;
import org.junit.Test;
import java.time.*;
import static org.junit.Assert.*;
public class CalendarModelTest {
    @Test public void startsWithMondayAndHandlesSundayStart(){
        YearMonth month=YearMonth.of(2026,11);assertEquals(6,CalendarModel.leadingDays(month));assertNull(CalendarModel.cell(month,0));assertEquals(LocalDate.of(2026,11,1),CalendarModel.cell(month,6));assertEquals(LocalDate.of(2026,11,30),CalendarModel.cell(month,35));assertNull(CalendarModel.cell(month,36));
    }
    @Test public void supportsLeapDay(){YearMonth month=YearMonth.of(2024,2);assertEquals(LocalDate.of(2024,2,29),CalendarModel.cell(month,31));assertNull(CalendarModel.cell(month,32));}
    @Test public void disallowsFutureAndTooEarlyDates(){LocalDate today=LocalDate.of(2026,11,11);assertTrue(CalendarModel.selectable(today,today));assertFalse(CalendarModel.selectable(today.plusDays(1),today));assertFalse(CalendarModel.selectable(LocalDate.of(1969,12,31),today));assertFalse(CalendarModel.selectable(null,today));}
}
