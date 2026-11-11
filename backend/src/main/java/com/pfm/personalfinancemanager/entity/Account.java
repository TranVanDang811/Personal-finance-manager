package com.pfm.personalfinancemanager.entity;

import com.pfm.personalfinancemanager.enums.AccountStatus;
import com.pfm.personalfinancemanager.enums.AccountType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "accounts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Account extends BaseAuditableEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "account_name", length = 100, nullable = false)
    private String accountName;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", length = 30, nullable = false)
    private AccountType accountType;

    @Builder.Default
    @Column(name = "initial_balance", precision = 19, scale = 4, nullable = false)
    private BigDecimal initialBalance = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "current_balance", precision = 19, scale = 4, nullable = false)
    private BigDecimal currentBalance = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "currency", length = 10, nullable = false)
    private String currency = "VND";

    @Column(name = "description", length = 255)
    private String description;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private AccountStatus status = AccountStatus.ACTIVE;

    @Builder.Default
    @Column(name = "allow_negative", nullable = false)
    private Boolean allowNegative = false;

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @PrePersist
    public void ensureId() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
    }
}

