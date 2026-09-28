package com.hesoy9.guesthouse.web;

import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

public final class ValidationUtil {

    private ValidationUtil() {
        // utility class - not meant to be instantiated
    }

    public static String firstErrorMessage(BindingResult bindingResult) {
        FieldError error = bindingResult.getFieldError();
        return error != null ? error.getDefaultMessage() : "Invalid input.";
    }
}
