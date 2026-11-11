package com.pfm.personalfinancemanager.repository.spec;

import com.pfm.personalfinancemanager.dto.request.TransactionFilterRequest;
import com.pfm.personalfinancemanager.entity.Transaction;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class TransactionSpecification {

    private TransactionSpecification() {
    }

    public static Specification<Transaction> withFilter(String userId, TransactionFilterRequest filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("user").get("id"), userId));

            if (filter == null) {
                return cb.and(predicates.toArray(Predicate[]::new));
            }

            if (StringUtils.hasText(filter.getKeyword())) {
                String pattern = "%" + filter.getKeyword().trim().toLowerCase(Locale.ROOT) + "%";
                Predicate descMatch = cb.like(cb.lower(root.get("description")), pattern);
                Predicate noteMatch = cb.like(cb.lower(cb.coalesce(root.get("note"), "")), pattern);
                predicates.add(cb.or(descMatch, noteMatch));
            }

            if (filter.getTransactionType() != null) {
                predicates.add(cb.equal(root.get("transactionType"), filter.getTransactionType()));
            }

            if (StringUtils.hasText(filter.getAccountId())) {
                String accountId = filter.getAccountId().trim();
                var toAccountJoin = root.join("toAccount", JoinType.LEFT);
                predicates.add(cb.or(
                        cb.equal(root.get("account").get("id"), accountId),
                        cb.equal(toAccountJoin.get("id"), accountId)
                ));
            }

            if (StringUtils.hasText(filter.getCategoryId())) {
                predicates.add(cb.equal(root.get("category").get("id"), filter.getCategoryId().trim()));
            }

            if (filter.getStartDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("transactionDate"), filter.getStartDate()));
            }

            if (filter.getEndDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("transactionDate"), filter.getEndDate()));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}

