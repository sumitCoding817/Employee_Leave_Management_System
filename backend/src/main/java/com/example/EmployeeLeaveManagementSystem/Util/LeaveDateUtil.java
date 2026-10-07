package com.example.EmployeeLeaveManagementSystem.Util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public final class LeaveDateUtil {

    private LeaveDateUtil() {
    }

    /** Inclusive day count between start and end, optionally skipping Saturdays and Sundays. */
    public static int calculateLeaveDays(LocalDate start, LocalDate end, boolean excludeWeekends) {
        if (!excludeWeekends) {
            return (int) ChronoUnit.DAYS.between(start, end) + 1;
        }
        int days = 0;
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            DayOfWeek day = date.getDayOfWeek();
            if (day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY) {
                days++;
            }
        }
        return days;
    }
}
