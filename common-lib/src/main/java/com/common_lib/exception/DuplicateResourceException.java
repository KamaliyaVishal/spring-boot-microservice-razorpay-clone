package com.common_lib.exception;

import com.common_lib.exception.enums.ErrorCode;
import lombok.Getter;

@Getter
public class DuplicateResourceException extends BaseBusinessException {

    private final String conflictField;
    private final Object duplicatedValue;

    public DuplicateResourceException(String message, String conflictField, Object duplicatedValue) {
        super(ErrorCode.DUPLICATE_RESOURCE, message);
        this.conflictField = conflictField;
        this.duplicatedValue = duplicatedValue;
    }

}
