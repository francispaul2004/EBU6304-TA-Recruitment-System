package edu.bupt.ta.dto;

import edu.bupt.ta.model.User;

public record LoginResult(boolean success, String message, User user) {

    public static LoginResult success(User user) {
        return new LoginResult(true, "Login successful.", user);
    }

    public static LoginResult fail(String message) {
        return new LoginResult(false, message, null);
    }
}
