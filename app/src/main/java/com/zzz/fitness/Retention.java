package com.zzz.fitness;
import java.time.LocalDate;
public final class Retention {
    public static final String[] LABELS={"近 1 天","近 3 天","近 7 天","近 1 个月","近 3 个月","近 6 个月","近 1 年","全部记录"};
    public static LocalDate start(LocalDate today,int option) {
        switch(option) {
            case 0:return today;
            case 1:return today.minusDays(2);
            case 2:return today.minusDays(6);
            case 3:return today.minusMonths(1).plusDays(1);
            case 4:return today.minusMonths(3).plusDays(1);
            case 5:return today.minusMonths(6).plusDays(1);
            case 6:return today.minusYears(1).plusDays(1);
            case 7:return LocalDate.of(1970,1,1);
            default:throw new IllegalArgumentException("Unknown retention option");
        }
    }
}
