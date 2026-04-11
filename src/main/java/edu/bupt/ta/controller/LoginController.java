package edu.bupt.ta.controller;

import edu.bupt.ta.dto.LoginResult;
import edu.bupt.ta.model.User;
import edu.bupt.ta.service.ServiceRegistry;
import edu.bupt.ta.ui.IconFactory;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
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
import javafx.scene.layout.StackPane;
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
        view.getStyleClass().add("login-root");

        HBox root = new HBox();
        root.getStyleClass().add("login-shell");
        root.setFillHeight(true);
        root.setMaxWidth(1400);
        root.setPrefHeight(760);
        root.setMinHeight(680);

        VBox leftPane = buildLeftPane();
        VBox rightPane = buildRightPane();

        leftPane.setMinWidth(520);
        rightPane.setMinWidth(520);
        HBox.setHgrow(leftPane, Priority.ALWAYS);
        HBox.setHgrow(rightPane, Priority.ALWAYS);

        root.getChildren().addAll(leftPane, rightPane);
        view.setCenter(root);
    }

    private VBox buildLeftPane() {
        VBox left = new VBox();
        left.getStyleClass().add("login-left");
        left.setPadding(new Insets(64));

        HBox brandRow = new HBox(12);
        brandRow.setAlignment(Pos.CENTER_LEFT);

        StackPane brandIcon = IconFactory.badge(
                IconFactory.IconType.GRADUATION_CAP,
                36,
                Color.rgb(255, 255, 255, 0.12),
                Color.WHITE
        );
        brandIcon.getStyleClass().add("login-brand-icon");

        Label brandTitle = new Label("BUPT International School");
        brandTitle.getStyleClass().add("login-brand-title");

        brandRow.getChildren().addAll(brandIcon, brandTitle);

        Label hero = new Label("Teaching Assistant\nRecruitment System");
        hero.getStyleClass().add("login-hero-title");

        VBox topBlock = new VBox(48, brandRow, hero);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        HBox secureRow = new HBox(16);
        secureRow.setAlignment(Pos.CENTER_LEFT);

        StackPane secureIcon = IconFactory.glyph(
                IconFactory.IconType.SHIELD,
                13,
                Color.web("#cbd5e1")
        );
        secureIcon.getStyleClass().add("login-left-meta-icon");

        Label secure = new Label("Secure Academic Portal for Students & Faculty");
        secure.getStyleClass().add("login-left-meta");
        secureRow.getChildren().addAll(secureIcon, secure);

        Label copyright = new Label("© 2026 Beijing University of Posts and Telecommunications. All rights reserved.");
        copyright.getStyleClass().add("login-left-footer");

        VBox footer = new VBox(12, secureRow, copyright);

        left.getChildren().addAll(topBlock, spacer, footer);
        return left;
    }

    private VBox buildRightPane() {
        VBox right = new VBox();
        right.getStyleClass().add("login-right");
        right.setAlignment(Pos.CENTER);
        right.setPadding(new Insets(48));

        VBox content = new VBox(32);
        content.setMaxWidth(448);
        content.setPrefWidth(448);
        content.getStyleClass().add("login-content");

        Label heading = new Label("Portal Login");
        heading.getStyleClass().add("login-heading");

        Label subtitle = new Label("Enter your university credentials to continue");
        subtitle.getStyleClass().add("login-subheading");

        VBox titleBlock = new VBox(8, heading, subtitle);

        Label userLabel = new Label("University ID / Username");
        userLabel.getStyleClass().add("login-field-label");
        usernameField.setPromptText("e.g. 2023211000");
        usernameField.getStyleClass().add("login-input-field");
        HBox usernameInput = buildInputShell(
                IconFactory.glyph(IconFactory.IconType.USER, 13, Color.web("#94a3b8")),
                usernameField,
                null
        );
        VBox usernameBlock = new VBox(8, userLabel, usernameInput);

        Label passLabel = new Label("Password");
        passLabel.getStyleClass().add("login-field-label");

        Label forgotLabel = new Label("Forgot password?");
        forgotLabel.getStyleClass().add("login-forgot-link");

        HBox passwordHeader = new HBox();
        passwordHeader.setAlignment(Pos.CENTER_LEFT);
        Region passwordSpacer = new Region();
        HBox.setHgrow(passwordSpacer, Priority.ALWAYS);
        passwordHeader.getChildren().addAll(passLabel, passwordSpacer, forgotLabel);

        passwordField.setPromptText("••••••••");
        passwordField.getStyleClass().add("login-input-field");

        Button eyeButton = new Button();
        eyeButton.setFocusTraversable(false);
        eyeButton.setMouseTransparent(true);
        eyeButton.getStyleClass().add("login-eye-button");
        eyeButton.setGraphic(IconFactory.glyph(IconFactory.IconType.EYE, 13, Color.web("#94a3b8")));
        HBox passwordInput = buildInputShell(
                IconFactory.glyph(IconFactory.IconType.LOCK, 13, Color.web("#94a3b8")),
                passwordField,
                eyeButton
        );
        VBox passwordBlock = new VBox(8, passwordHeader, passwordInput);

        VBox fieldsBlock = new VBox(20, usernameBlock, passwordBlock);

        CheckBox keepLogin = new CheckBox("Keep me logged in on this device");
        keepLogin.getStyleClass().add("login-keep-checkbox");

        Button loginButton = new Button("LOGIN TO PORTAL");
        loginButton.getStyleClass().add("login-primary-button");
        loginButton.setPrefHeight(44);
        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setOnAction(event -> doLogin());

        Button resetButton = new Button("Reset");
        resetButton.getStyleClass().add("login-secondary-button");
        resetButton.setPrefHeight(44);
        resetButton.setPrefWidth(96);
        resetButton.setOnAction(event -> {
            usernameField.clear();
            passwordField.clear();
        });

        HBox actions = new HBox(12, loginButton, resetButton);
        HBox.setHgrow(loginButton, Priority.ALWAYS);
        actions.setMaxWidth(Double.MAX_VALUE);

        VBox form = new VBox(24, fieldsBlock, keepLogin, actions);

        Region divider = new Region();
        divider.getStyleClass().add("login-divider");
        divider.setPrefHeight(1);
        divider.setMaxWidth(Double.MAX_VALUE);

        HBox footerLinks = new HBox();
        footerLinks.setAlignment(Pos.CENTER_LEFT);
        footerLinks.setMaxWidth(Double.MAX_VALUE);

        Label access = new Label("EXTERNAL ACCESS");
        access.getStyleClass().add("login-footer-links");

        HBox footerSpacer = new HBox();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);

        Label support = new Label("HELP CENTER");
        support.getStyleClass().add("login-footer-links");

        Label privacy = new Label("PRIVACY");
        privacy.getStyleClass().add("login-footer-links");

        HBox rightLinks = new HBox(16, support, privacy);
        footerLinks.getChildren().addAll(access, footerSpacer, rightLinks);

        content.getChildren().addAll(
                titleBlock,
                form,
                divider,
                footerLinks
        );

        right.getChildren().add(content);
        return right;
    }

    private HBox buildInputShell(Node leftIcon, TextField field, Node rightNode) {
        HBox shell = new HBox(10);
        shell.getStyleClass().add("login-input-shell");
        shell.setAlignment(Pos.CENTER_LEFT);
        shell.setMaxWidth(Double.MAX_VALUE);

        StackPane leftIconBox = new StackPane(leftIcon);
        leftIconBox.getStyleClass().add("login-input-icon-left");
        shell.getChildren().add(leftIconBox);

        field.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(field, Priority.ALWAYS);
        shell.getChildren().add(field);

        if (rightNode != null) {
            StackPane rightIconBox = new StackPane(rightNode);
            rightIconBox.getStyleClass().add("login-input-icon-right");
            shell.getChildren().add(rightIconBox);
        }

        return shell;
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
