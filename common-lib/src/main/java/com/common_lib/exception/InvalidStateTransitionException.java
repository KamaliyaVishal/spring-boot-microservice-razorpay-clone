package com.common_lib.exception;

import com.common_lib.exception.enums.ErrorCode;
import lombok.Getter;

@Getter
public class InvalidStateTransitionException extends BaseBusinessException {

    private final String fromState;
    private final String toEvent;

    public InvalidStateTransitionException(String message, String fromState, String event) {
        super(ErrorCode.INVALID_TRANSITION_STATE, message);
        this.fromState = fromState;
        this.toEvent = event;
    }
}
