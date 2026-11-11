package com.pfm.personalfinancemanager.util;

import com.pfm.personalfinancemanager.enums.RecurringFrequency;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class RecurringDateCalculatorTest {

    @Test
    void calculateNextExecutionDate_DailyAndWeekly_ShouldAdvanceAccurately() {
        LocalDate anchor = LocalDate.of(2026, 11, 11);

        assertThat(RecurringDateCalculator.calculateNextExecutionDate(anchor, RecurringFrequency.DAILY, anchor))
                .isEqualTo(LocalDate.of(2026, 11, 12));
        assertThat(RecurringDateCalculator.calculateNextExecutionDate(anchor, RecurringFrequency.WEEKLY, anchor))
                .isEqualTo(LocalDate.of(2026, 11, 18));
    }

    @Test
    void calculateNextExecutionDate_MonthlyAnchorOn31st_ShouldClampFebruaryAndRestoreOn31DayMonths() {
        LocalDate anchor = LocalDate.of(2026, 1, 31);

        LocalDate feb = RecurringDateCalculator.calculateNextExecutionDate(anchor, RecurringFrequency.MONTHLY, anchor);
        assertThat(feb).isEqualTo(LocalDate.of(2026, 2, 28));

        LocalDate mar = RecurringDateCalculator.calculateNextExecutionDate(anchor, RecurringFrequency.MONTHLY, feb);
        assertThat(mar).isEqualTo(LocalDate.of(2026, 3, 31));

        LocalDate apr = RecurringDateCalculator.calculateNextExecutionDate(anchor, RecurringFrequency.MONTHLY, mar);
        assertThat(apr).isEqualTo(LocalDate.of(2026, 4, 30));
    }

    @Test
    void calculateNextExecutionDate_YearlyLeapDay_ShouldHandleNonLeapAndReturnToLeapDay() {
        LocalDate leapAnchor = LocalDate.of(2024, 2, 29);

        LocalDate y2025 = RecurringDateCalculator.calculateNextExecutionDate(
                leapAnchor, RecurringFrequency.YEARLY, leapAnchor);
        assertThat(y2025).isEqualTo(LocalDate.of(2025, 2, 28));

        LocalDate y2028 = RecurringDateCalculator.calculateNextExecutionDate(
                leapAnchor, RecurringFrequency.YEARLY, LocalDate.of(2027, 2, 28));
        assertThat(y2028).isEqualTo(LocalDate.of(2028, 2, 29));
    }

    @Test
    void advanceToOnOrAfter_ShouldAdvancePausedScheduleToFirstValidDateOnOrAfterToday() {
        LocalDate anchor = LocalDate.of(2026, 1, 31);
        LocalDate pausedNext = LocalDate.of(2026, 2, 28);
        LocalDate resumeDay = LocalDate.of(2026, 5, 10);

        LocalDate res = RecurringDateCalculator.advanceToOnOrAfter(
                anchor, RecurringFrequency.MONTHLY, pausedNext, resumeDay);

        assertThat(res).isEqualTo(LocalDate.of(2026, 5, 31));
    }
}

