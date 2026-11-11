package com.pfm.personalfinancemanager.util;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public final class DateTimeUtils {

    private DateTimeUtils() {
    }

    public static Instant startOfMonthUtc(int year, int month) {
        return YearMonth.of(year, month).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    public static Instant startOfNextMonthUtc(int year, int month) {
        return YearMonth.of(year, month).plusMonths(1).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    public static YearMonth yearMonthUtc(Instant instant) {
        return YearMonth.from(ZonedDateTime.ofInstant(instant, ZoneOffset.UTC));
    }
}

