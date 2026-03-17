package edu.bupt.ta.controller;

import edu.bupt.ta.dto.LoginResult;
import edu.bupt.ta.model.User;
import edu.bupt.ta.service.ServiceRegistry;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.function.Consumer;

public class LoginController {

    private final ServiceRegistry services;
    private final Consumer<User> onLoginSuccess;

    private final TextField usernameField = new TextField();
    private final PasswordField passwordField = new PasswordField();

    private final BorderPane view = new BorderPane();

    public LoginController(ServiceRegistry services, Consumer<User> onLoginSuccess) {
        this.services = services;
        this.onLoginSuccess = onLoginSuccess;
        initialize();
    }

    public Parent getView() {
        return view;
    }

    private void initialize() {
        HBox root = new HBox();

        VBox leftPane = buildLeftPane();
        VBox rightPane = buildRightPane();

        HBox.setHgrow(leftPane, Priority.ALWAYS);
        HBox.setHgrow(rightPane, Priority.ALWAYS);
        leftPane.setPrefWidth(640);
        rightPane.setPrefWidth(640);

        root.getChildren().addAll(leftPane, rightPane);
        view.setCenter(root);
    }

    private VBox buildLeftPane() {
        VBox left = new VBox(24);
        left.setPadding(new Insets(56));
        left.setStyle("-fx-background-color: #354A5F;");

        Label brand = new Label("BUPT International");
        brand.setStyle("-fx-font-size: 34px; -fx-font-weight: 800; -fx-text-fill: white;");

        Label title = new Label("Teaching Assistant\nRecruitment System");
        title.setStyle("-fx-font-size: 54px; -fx-font-weight: 800; -fx-text-fill: white; -fx-line-spacing: 8px;");

        Label desc = new Label("Streamlining the bridge between academic excellence and professional teaching support.\n"
                + "Manage applications, schedules, and assignments in one professional portal.");
        desc.setWrapText(true);
        desc.setStyle("-fx-font-size: 22px; -fx-text-fill: #E2E8F0; -fx-line-spacing: 8px;");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Label footer = new Label("Secure Academic Portal for Students & Faculty\n"
                + "Test accounts password: Password123!");
        footer.setStyle("-fx-font-size: 14px; -fx-text-fill: #CBD5E1; -fx-line-spacing: 4px;");

        left.getChildren().addAll(brand, title, desc, spacer, footer);
        return left;
    }

    private VBox buildRightPane() {
        VBox right = new VBox(18);
        right.setAlignment(Pos.CENTER_LEFT);
        right.setPadding(new Insets(64));
        right.setStyle("-fx-background-color: #F6F7F7;");

        Label heading = new Label("Portal Login");
        heading.setStyle("-fx-font-size: 36px; -fx-font-weight: 800; -fx-text-fill: #0F172A;");

        Label subtitle = new Label("Enter your university credentials to continue");
        subtitle.setStyle("-fx-font-size: 16px; -fx-text-fill: #475569;");

        Label userLabel = new Label("University ID / Username");
        userLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #334155;");
        usernameField.setPromptText("e.g. ta001 / mo001 / admin");
        usernameField.setPrefHeight(42);

        Label passLabel = new Label("Password");
        passLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #334155;");
        passwordField.setPromptText("Password123!");
        passwordField.setPrefHeight(42);

        CheckBox keepLogin = new CheckBox("Keep me logged in on this device");
        keepLogin.setTextFill(Color.web("#475569"));

        Button loginButton = new Button("LOGIN TO PORTAL");
        loginButton.getStyleClass().add("primary-button");
        loginButton.setPrefHeight(44);
        loginButton.setPrefWidth(280);
        loginButton.setOnAction(event -> doLogin());

        Button resetButton = new Button("Reset");
        resetButton.getStyleClass().add("secondary-button");
        resetButton.setPrefHeight(44);
        resetButton.setPrefWidth(120);
        resetButton.setOnAction(event -> {
            usernameField.clear();
            passwordField.clear();
        });

        HBox actions = new HBox(12, loginButton, resetButton);

        Label quickHint = new Label("TA: ta001  |  MO: mo001  |  ADMIN: admin");
        quickHint.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");

        right.getChildren().addAll(
                heading, subtitle,
                userLabel, usernameField,
                passLabel, passwordField,
                keepLogin, actions,
                quickHint
        );

        return right;
    }

    private void doLogin() {
        LoginResult result = services.authenticationService().login(usernameField.getText(), passwordField.getText());
        if (!result.success()) {
            Alert alert = new Alert(Alert.AlertType.ERROR, result.message());
            alert.setHeaderText("Login failed");
            alert.showAndWait();
            return;
        }
        onLoginSuccess.accept(result.user());
    }
}
