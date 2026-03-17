package edu.bupt.ta.util;

import java.util.ArrayList;
import java.util.List;

public class ValidationResult {

    private final boolean valid;
    private final List<String> errors;

    private ValidationResult(boolean valid, List<String> errors) {
        this.valid = valid;
        this.errors = errors;
    }

    public static ValidationResult ok() {
        return new ValidationResult(true, List.of());
    }

    public static ValidationResult fail(String error) {
        return new ValidationResult(false, List.of(error));
    }

    public static ValidationResult fail(List<String> errors) {
        return new ValidationResult(false, new ArrayList<>(errors));
    }

    public boolean isValid() {
        return valid;
    }

    public List<String> getErrors() {
        return errors;
    }
}
