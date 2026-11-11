package com.pfm.personalfinancemanager.entity;

import com.pfm.personalfinancemanager.enums.SavingGoalStatus;
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
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "saving_goals")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavingGoal extends BaseAuditableEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "name", length = 150, nullable = false)
    private String name;

    @Column(name = "target_amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal targetAmount;

    @Builder.Default
    @Column(name = "current_amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal currentAmount = BigDecimal.ZERO;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    @Column(name = "description", length = 255)
    private String description;

    @Builder.Default
    @Column(name = "color", length = 20, nullable = false)
    private String color = "#10B981";

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private SavingGoalStatus status = SavingGoalStatus.IN_PROGRESS;

    @PrePersist
    public void ensureId() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
    }
}

