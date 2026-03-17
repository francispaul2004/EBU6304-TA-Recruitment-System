package edu.bupt.ta.controller;

import edu.bupt.ta.enums.Role;
import edu.bupt.ta.model.User;
import edu.bupt.ta.service.ServiceRegistry;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.LinkedHashMap;
import java.util.Map;

public class MainShellController {

    private final ServiceRegistry services;
    private final User user;
    private final Runnable onLogout;

    private final BorderPane view = new BorderPane();
    private final StackPane contentPane = new StackPane();
    private final Label breadcrumb = new Label();
    private final Map<String, Button> navButtons = new LinkedHashMap<>();

    public MainShellController(ServiceRegistry services, User user, Runnable onLogout) {
        this.services = services;
        this.user = user;
        this.onLogout = onLogout;
        initialize();
    }

    public Parent getView() {
        return view;
    }

    private void initialize() {
        view.setLeft(buildSidebar());
        view.setTop(buildTopBar());

        BorderPane center = new BorderPane();
        center.setCenter(contentPane);
        center.setPadding(new Insets(0));
        center.setStyle("-fx-background-color: #FFFFFF;");
        view.setCenter(center);

        if (user.getRole() == Role.TA) {
            navigateTo("browseJobs");
        } else if (user.getRole() == Role.MO) {
            navigateTo("jobManagement");
        } else {
            navigateTo("adminDashboard");
        }
    }

    private VBox buildSidebar() {
        VBox sidebar = new VBox(10);
        sidebar.setPrefWidth(240);
        sidebar.setPadding(new Insets(20));
        sidebar.setStyle("-fx-background-color: #EEF2F5; -fx-border-color: #E2E8F0; -fx-border-width: 0 1 0 0;");

        Label brand = new Label("BUPT IS RECRUITMENT");
        brand.setStyle("-fx-font-size: 14px; -fx-font-weight: 800; -fx-text-fill: #354A5F;");

        Label edition = new Label(user.getRole() + " EDITION");
        edition.setStyle("-fx-font-size: 11px; -fx-font-weight: 600; -fx-text-fill: #64748B;");

        VBox menuBox = new VBox(6);
        menuBox.getChildren().addAll(buildRoleMenu());

        VBox profileCard = new VBox(2);
        profileCard.setPadding(new Insets(12));
        profileCard.setStyle("-fx-background-color: white; -fx-background-radius: 8;");
        Label name = new Label(user.getDisplayName());
        name.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #0F172A;");
        Label id = new Label("ID: " + user.getUserId());
        id.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748B;");
        profileCard.getChildren().addAll(name, id);

        Button logout = new Button("Logout");
        logout.getStyleClass().add("secondary-button");
        logout.setMaxWidth(Double.MAX_VALUE);
        logout.setOnAction(event -> onLogout.run());

        VBox.setVgrow(menuBox, Priority.ALWAYS);
        sidebar.getChildren().addAll(brand, edition, menuBox, profileCard, logout);
        return sidebar;
    }

    private VBox buildRoleMenu() {
        VBox menu = new VBox(6);
        if (user.getRole() == Role.TA) {
            menu.getChildren().addAll(
                    navButton("TA Dashboard", "dashboard"),
                    navButton("Browse Jobs", "browseJobs"),
                    navButton("My Applications", "myApplications"),
                    navButton("My CV", "myCv")
            );
        } else if (user.getRole() == Role.MO) {
            menu.getChildren().addAll(
                    navButton("Job Management", "jobManagement"),
                    navButton("Applicant List", "applicantList")
            );
        } else {
            menu.getChildren().add(navButton("Admin Dashboard", "adminDashboard"));
        }
        return menu;
    }

    private Button navButton(String label, String pageId) {
        Button button = new Button(label);
        button.setAlignment(Pos.CENTER_LEFT);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setPrefHeight(36);
        button.setStyle("-fx-background-color: transparent; -fx-text-fill: #475569; -fx-font-size: 14px;");
        button.setOnAction(event -> navigateTo(pageId));
        navButtons.put(pageId, button);
        return button;
    }

    private HBox buildTopBar() {
        HBox top = new HBox();
        top.setAlignment(Pos.CENTER_LEFT);
        top.setPadding(new Insets(16, 24, 16, 24));
        top.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #E2E8F0; -fx-border-width: 0 0 1 0;");

        breadcrumb.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #0F172A;");

        Label termInfo = new Label("Spring Semester 2026");
        termInfo.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #354A5F;");

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        top.getChildren().addAll(breadcrumb, spacer, termInfo);
        return top;
    }

    private void navigateTo(String pageId) {
        navButtons.forEach((id, btn) -> btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #475569; -fx-font-size: 14px;"));
        Button active = navButtons.get(pageId);
        if (active != null) {
            active.setStyle("-fx-background-color: white; -fx-text-fill: #354A5F; -fx-font-size: 14px; -fx-font-weight: 700; -fx-background-radius: 8;");
        }

        Parent page;
        switch (pageId) {
            case "browseJobs" -> {
                breadcrumb.setText("Recruitment System  >  Browse Jobs");
                page = new JobBrowserController(services, user).getView();
            }
            case "jobManagement" -> {
                breadcrumb.setText("Recruitment System  >  Job Management");
                page = new JobManagementController(services, user).getView();
            }
            case "dashboard" -> {
                breadcrumb.setText("Recruitment System  >  TA Dashboard");
                page = new TADashboardController(services, user).getView();
            }
            case "myApplications" -> {
                breadcrumb.setText("Recruitment System  >  My Applications");
                page = new MyApplicationsController(services, user).getView();
            }
            case "myCv" -> {
                breadcrumb.setText("Recruitment System  >  My CV");
                page = new MyCvController(services, user).getView();
            }
            case "applicantList" -> {
                breadcrumb.setText("Recruitment System  >  Applicant List");
                page = new ApplicantListController(services, user).getView();
            }
            case "adminDashboard" -> {
                breadcrumb.setText("Recruitment System  >  Admin Dashboard");
                page = new AdminDashboardController(services, user).getView();
            }
            default -> {
                breadcrumb.setText("Recruitment System");
                page = PlaceholderPage.simple("Page", "Not implemented yet.");
            }
        }

        contentPane.getChildren().setAll(page);
    }
}
