package com.pfm.personalfinancemanager.scheduler;

import com.pfm.personalfinancemanager.dto.response.RecurringExecutionBatchResult;
import com.pfm.personalfinancemanager.service.RecurringTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecurringTransactionScheduler {

    private final RecurringTransactionService recurringTransactionService;

    @Scheduled(cron = "${app.recurring.cron:0 5 0 * * *}", zone = "UTC")
    public RecurringExecutionBatchResult executeScheduledRecurringTransactions() {
        log.info("Starting scheduled recurring transactions batch execution");
        return recurringTransactionService.processDueRecurringTransactions();
    }
}

