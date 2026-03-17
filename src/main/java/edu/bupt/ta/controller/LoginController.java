package edu.bupt.ta.controller;

import edu.bupt.ta.dto.LoginResult;
import edu.bupt.ta.model.User;
import edu.bupt.ta.service.ServiceRegistry;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
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
        view.getStyleClass().add("login-root");

        HBox root = new HBox();
        root.getStyleClass().add("login-shell");

        VBox leftPane = buildLeftPane();
        VBox rightPane = buildRightPane();

        leftPane.setPrefWidth(640);
        rightPane.setPrefWidth(640);
        HBox.setHgrow(leftPane, Priority.ALWAYS);
        HBox.setHgrow(rightPane, Priority.ALWAYS);

        root.getChildren().addAll(leftPane, rightPane);
        view.setCenter(root);
    }

    private VBox buildLeftPane() {
        VBox left = new VBox(44);
        left.getStyleClass().add("login-left");
        left.setPadding(new Insets(64));

        HBox brandRow = new HBox(12);
        brandRow.setAlignment(Pos.CENTER_LEFT);

        Region brandIcon = new Region();
        brandIcon.setPrefSize(36, 36);
        brandIcon.setStyle("-fx-background-color: rgba(255,255,255,0.15); -fx-background-radius: 8;");

        Label brandTitle = new Label("BUPT International");
        brandTitle.getStyleClass().add("login-brand-title");

        brandRow.getChildren().addAll(brandIcon, brandTitle);

        Label hero = new Label("Teaching Assistant\nRecruitment System");
        hero.getStyleClass().add("login-hero-title");

        Region bar = new Region();
        bar.setPrefSize(96, 6);
        bar.setStyle("-fx-background-color: rgba(255,255,255,0.35); -fx-background-radius: 999;");

        Label desc = new Label(
                "Streamlining the bridge between academic excellence\n"
                        + "and professional teaching support. Manage\n"
                        + "applications, schedules, and assignments in one\n"
                        + "professional portal.");
        desc.getStyleClass().add("login-hero-desc");

        VBox heroBlock = new VBox(22, hero, bar, desc);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        VBox footer = new VBox(12);
        Label secure = new Label("Secure Academic Portal for Students & Faculty");
        secure.setStyle("-fx-font-size: 14px; -fx-text-fill: #cbd5e1;");

        Label copyright = new Label("(c) 2024 Beijing University of Posts and Telecommunications. All rights reserved.");
        copyright.getStyleClass().add("login-left-footer");

        footer.getChildren().addAll(secure, copyright);

        left.getChildren().addAll(brandRow, heroBlock, spacer, footer);
        return left;
    }

    private VBox buildRightPane() {
        VBox right = new VBox();
        right.getStyleClass().add("login-right");
        right.setAlignment(Pos.CENTER);
        right.setPadding(new Insets(48));

        VBox content = new VBox(26);
        content.setMaxWidth(448);
        content.setPrefWidth(448);

        Label heading = new Label("Portal Login");
        heading.getStyleClass().add("login-heading");

        Label subtitle = new Label("Enter your university credentials to continue");
        subtitle.getStyleClass().add("login-subheading");

        VBox titleBlock = new VBox(6, heading, subtitle);

        Label userLabel = new Label("University ID / Username");
        userLabel.getStyleClass().add("field-label");
        usernameField.setPromptText("e.g. 2023211000");

        Label passLabel = new Label("Password");
        passLabel.getStyleClass().add("field-label");

        Label forgotLabel = new Label("Forgot password?");
        forgotLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #354a5f;");

        HBox passwordHeader = new HBox();
        passwordHeader.setAlignment(Pos.CENTER_LEFT);
        passwordHeader.getChildren().addAll(passLabel);
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        passwordHeader.getChildren().addAll(spacer, forgotLabel);

        passwordField.setPromptText("********");

        CheckBox keepLogin = new CheckBox("Keep me logged in on this device");
        keepLogin.setStyle("-fx-text-fill: #475569; -fx-font-size: 14px;");

        Button loginButton = new Button("LOGIN TO PORTAL");
        loginButton.getStyleClass().add("primary-button");
        loginButton.setPrefHeight(44);
        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setOnAction(event -> doLogin());

        Button resetButton = new Button("Reset");
        resetButton.getStyleClass().add("secondary-button");
        resetButton.setPrefHeight(44);
        resetButton.setPrefWidth(96);
        resetButton.setOnAction(event -> {
            usernameField.clear();
            passwordField.clear();
        });

        HBox actions = new HBox(12, loginButton, resetButton);
        HBox.setHgrow(loginButton, Priority.ALWAYS);

        Region divider = new Region();
        divider.setPrefHeight(1);
        divider.setStyle("-fx-background-color: #e2e8f0;");

        HBox footerLinks = new HBox();
        footerLinks.setAlignment(Pos.CENTER_LEFT);

        Label access = new Label("EXTERNAL ACCESS");
        access.getStyleClass().add("login-footer-links");

        HBox footerSpacer = new HBox();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);

        Label support = new Label("HELP CENTER");
        support.getStyleClass().add("login-footer-links");

        Label privacy = new Label("PRIVACY");
        privacy.getStyleClass().add("login-footer-links");

        footerLinks.getChildren().addAll(access, footerSpacer, support, new Label("   "), privacy);

        Label testHint = new Label("Demo password: Password123!  (ta001 / mo001 / admin)");
        testHint.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");

        content.getChildren().addAll(
                titleBlock,
                userLabel, usernameField,
                passwordHeader, passwordField,
                keepLogin,
                actions,
                divider,
                footerLinks,
                testHint
        );

        right.getChildren().add(content);
        return right;
    }

    private void doLogin() {
        LoginResult result = services.authenticationService().login(usernameField.getText(), passwordField.getText());
        if (!result.success()) {
            DialogControllerFactory.operationFailed("Login Failed", result.message(), view.getScene() == null
                    ? null : view.getScene().getWindow());
            return;
        }
        onLoginSuccess.accept(result.user());
    }
}
