package com.common_lib.enums;

public enum PaymentStatus {
    CREATED,
    AUTHORIZING,
    AUTHORIZED,
    CAPTURING,
    CAPTURED,
    FAILED,
    CANCELLED,
    AUTH_EXPIRED,
    REFUNDED,
    PARTIALLY_REFUNDED,
    SETTLED
}
