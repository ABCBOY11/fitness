package com.zzz.fitness;
import java.time.*;
public final class CalendarModel {
    public static int leadingDays(YearMonth month){return month.atDay(1).getDayOfWeek().getValue()-1;}
    public static LocalDate cell(YearMonth month,int index){int day=index-leadingDays(month)+1;return day<1||day>month.lengthOfMonth()?null:month.atDay(day);}
    public static boolean selectable(LocalDate date,LocalDate today){return date!=null&&!date.isAfter(today)&&!date.isBefore(LocalDate.of(1970,1,1));}
}
