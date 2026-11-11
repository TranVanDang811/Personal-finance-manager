package com.pfm.personalfinancemanager.util;

import com.pfm.personalfinancemanager.enums.RecurringFrequency;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;

public final class RecurringDateCalculator {

    private static final int SCHEDULED_HOUR_UTC = 0;
    private static final int SCHEDULED_MINUTE_UTC = 5;

    private RecurringDateCalculator() {
    }

    public static LocalDate calculateNextExecutionDate(
            LocalDate anchorStartDate,
            RecurringFrequency frequency,
            LocalDate currentExecutionDate
    ) {
        int anchorDay = anchorStartDate.getDayOfMonth();
        return switch (frequency) {
            case DAILY -> currentExecutionDate.plusDays(1);
            case WEEKLY -> currentExecutionDate.plusWeeks(1);
            case MONTHLY -> {
                YearMonth nextYm = YearMonth.from(currentExecutionDate).plusMonths(1);
                yield nextYm.atDay(Math.min(anchorDay, nextYm.lengthOfMonth()));
            }
            case YEARLY -> {
                int nextYear = currentExecutionDate.getYear() + 1;
                YearMonth nextYm = YearMonth.of(nextYear, anchorStartDate.getMonth());
                yield nextYm.atDay(Math.min(anchorDay, nextYm.lengthOfMonth()));
            }
        };
    }

    public static LocalDate advanceToOnOrAfter(
            LocalDate anchorStartDate,
            RecurringFrequency frequency,
            LocalDate candidateDate,
            LocalDate targetDate
    ) {
        LocalDate current = candidateDate;
        while (current.isBefore(targetDate)) {
            current = calculateNextExecutionDate(anchorStartDate, frequency, current);
        }
        return current;
    }

    public static Instant scheduledExecutionTimestamp(LocalDate executionDate) {
        return executionDate.atTime(SCHEDULED_HOUR_UTC, SCHEDULED_MINUTE_UTC).toInstant(ZoneOffset.UTC);
    }

    public static Instant dayStartUtc(LocalDate date) {
        return date.atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    public static Instant nextDayStartUtc(LocalDate date) {
        return date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
    }
}

