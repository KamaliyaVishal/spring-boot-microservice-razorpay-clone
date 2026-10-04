package com.payment_service.entity;

import com.common_lib.entity.BaseEntity;
import com.common_lib.entity.Money;
import com.common_lib.enums.PaymentMethod;
import com.common_lib.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "payment", indexes = {
        @Index(name = "idx_payment_order_id", columnList = "order_record_id"),
        @Index(name = "idx_payment_merchant_id", columnList = "merchant_id"),
        @Index(name = "idx_payment_merchant_idempotency", columnList = "merchant_id, idempotency_key", unique = true)
})
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_record_id")
    private OrderRecord orderRecord;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Embedded
    private Money amount;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "method_details", columnDefinition = "jsonb")
    private Map<String, Object> methodDetails;

    @Column(name = "bank_reference", length = 100)
    private String bankReference;

    @Column(name = "processor_reference", length = 100)
    private String processorReference;

    @Column(name = "error_code", length = 50)
    private String errorCode;

    @Column(name = "error_description", length = 200)
    private String errorDescription;

    @Column(name = "authorized_at")
    private LocalDateTime authorizedAt;

    @Column(name = "captured_at")
    private LocalDateTime capturedAt;

    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    @Column(name = "settled_at")
    private LocalDateTime settledAt;

}
