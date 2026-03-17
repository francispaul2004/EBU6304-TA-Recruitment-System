package edu.bupt.ta.controller;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;

import java.util.Optional;

public class DialogControllerFactory {

    private static final String STYLESHEET = "/styles/app.css";

    private DialogControllerFactory() {
    }

    public static void validationError(String message, Window owner) {
        Alert alert = create(Alert.AlertType.ERROR, "Validation Error", "Please correct the input and try again.",
                message, owner, "ta-dialog-error");
        alert.showAndWait();
    }

    public static void operationFailed(String title, String message, Window owner) {
        Alert alert = create(Alert.AlertType.ERROR, title, "Operation failed", message, owner, "ta-dialog-error");
        alert.showAndWait();
    }

    public static void success(String title, String message, Window owner) {
        Alert alert = create(Alert.AlertType.INFORMATION, title, "Operation completed", message, owner,
                "ta-dialog-success");
        alert.showAndWait();
    }

    public static void info(String title, String message, Window owner) {
        Alert alert = create(Alert.AlertType.INFORMATION, title, null, message, owner, "ta-dialog-info");
        alert.showAndWait();
    }

    public static void permissionDenied(String message, Window owner) {
        Alert alert = create(Alert.AlertType.WARNING, "Permission Denied",
                "You do not have permission for this action.", message, owner, "ta-dialog-warning");
        alert.showAndWait();
    }

    public static void workloadWarning(String message, Window owner) {
        Alert alert = create(Alert.AlertType.WARNING, "Workload Warning", "This action may exceed weekly limit.",
                message, owner, "ta-dialog-warning");
        alert.showAndWait();
    }

    public static boolean confirmAction(String title, String message, Window owner) {
        ButtonType confirm = new ButtonType("Confirm", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, confirm, cancel);
        setup(alert, title, "Please confirm this action.", owner, "ta-dialog-info");
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == confirm;
    }

    private static Alert create(Alert.AlertType type, String title, String header, String message, Window owner,
                                String styleClass) {
        Alert alert = new Alert(type, message, ButtonType.OK);
        setup(alert, title, header, owner, styleClass);
        return alert;
    }

    private static void setup(Alert alert, String title, String header, Window owner, String styleClass) {
        alert.setTitle(title == null ? "Notification" : title);
        alert.setHeaderText(header);
        if (owner != null) {
            alert.initOwner(owner);
        }
        if (DialogControllerFactory.class.getResource(STYLESHEET) != null) {
            String stylesheet = DialogControllerFactory.class.getResource(STYLESHEET).toExternalForm();
            if (!alert.getDialogPane().getStylesheets().contains(stylesheet)) {
                alert.getDialogPane().getStylesheets().add(stylesheet);
            }
        }
        alert.getDialogPane().getStyleClass().add("ta-dialog");
        if (styleClass != null && !styleClass.isBlank()) {
            alert.getDialogPane().getStyleClass().add(styleClass);
        }
    }
}
