package com.vault_service.validation;

import com.vault_service.validation.impl.ExpiryYearValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(
        validatedBy = {ExpiryYearValidator.class}
)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ExpiryYear {

    String message() default "Expiry Year cannot be in Past";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
