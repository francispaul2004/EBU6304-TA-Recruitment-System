package edu.bupt.ta.controller;

import edu.bupt.ta.enums.Role;
import edu.bupt.ta.model.Job;
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
import java.util.List;
import java.util.Map;

public class MainShellController {

    private final ServiceRegistry services;
    private final User user;
    private final Runnable onLogout;

    private final BorderPane view = new BorderPane();
    private final StackPane contentPane = new StackPane();
    private final Label breadcrumbBase = new Label("Recruitment System");
    private final Label breadcrumbCurrent = new Label();
    private final Map<String, Button> navButtons = new LinkedHashMap<>();
    private String preferredApplicantJobId;

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
        view.getStyleClass().add("shell-root");
        view.setLeft(buildSidebar());
        view.setTop(buildTopBar());

        BorderPane center = new BorderPane();
        center.getStyleClass().add("shell-content");
        center.setCenter(contentPane);
        view.setCenter(center);

        if (user.getRole() == Role.TA) {
            navigateTo("dashboard");
        } else if (user.getRole() == Role.MO) {
            navigateTo("jobManagement");
        } else {
            navigateTo("adminDashboard");
        }
    }

    private VBox buildSidebar() {
        VBox sidebar = new VBox();
        sidebar.getStyleClass().add("shell-sidebar");
        sidebar.setPrefWidth(256);
        sidebar.setMinWidth(256);

        VBox brandSection = new VBox(2);
        brandSection.setPadding(new Insets(24));

        Label brandTitle = new Label("BUPT IS RECRUITMENT");
        brandTitle.getStyleClass().add("shell-brand-title");

        Label brandSub = new Label(roleEditionText());
        brandSub.getStyleClass().add("shell-brand-sub");

        brandSection.getChildren().addAll(brandTitle, brandSub);

        VBox navArea = new VBox(8);
        navArea.setPadding(new Insets(8, 16, 8, 16));

        if (user.getRole() == Role.TA) {
            navArea.getChildren().add(buildSection("RECRUITMENT", List.of(
                    new NavEntry("Dashboard", "dashboard"),
                    new NavEntry("Browse Jobs", "browseJobs"),
                    new NavEntry("My Applications", "myApplications"),
                    new NavEntry("CV Management", "myCv")
            )));
        } else if (user.getRole() == Role.MO) {
            navArea.getChildren().add(buildSection("RECRUITMENT", List.of(
                    new NavEntry("Job Management", "jobManagement"),
                    new NavEntry("Applicant List", "applicantList"),
                    new NavEntry("Profile", "moProfile")
            )));
        } else {
            navArea.getChildren().add(buildSection("RECRUITMENT", List.of(
                    new NavEntry("Dashboard", "adminDashboard"),
                    new NavEntry("Jobs", "adminJobs"),
                    new NavEntry("Applications", "adminApplications")
            )));
        }

        navArea.getChildren().add(buildSection("SUPPORT", List.of(
                new NavEntry("Help Center", "helpCenter"),
                new NavEntry("Settings", "settings")
        )));

        VBox footer = new VBox(12);
        footer.setPadding(new Insets(16));

        VBox profileCard = new VBox(2);
        profileCard.getStyleClass().add("shell-profile-card");
        profileCard.setPadding(new Insets(10));

        Label profileName = new Label(user.getDisplayName());
        profileName.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #0f172a;");

        Label profileId = new Label("ID: " + user.getUserId());
        profileId.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748b;");

        profileCard.getChildren().addAll(profileName, profileId);

        Button logout = new Button("Logout");
        logout.getStyleClass().add("secondary-button");
        logout.setMaxWidth(Double.MAX_VALUE);
        logout.setOnAction(event -> onLogout.run());

        footer.getChildren().addAll(profileCard, logout);

        VBox.setVgrow(navArea, Priority.ALWAYS);
        sidebar.getChildren().addAll(brandSection, navArea, footer);
        return sidebar;
    }

    private VBox buildSection(String title, List<NavEntry> items) {
        VBox section = new VBox(8);

        Label header = new Label(title);
        header.getStyleClass().add("shell-nav-header");
        header.setPadding(new Insets(12, 12, 0, 12));

        VBox links = new VBox(6);
        for (NavEntry item : items) {
            links.getChildren().add(navButton(item.label(), item.pageId()));
        }

        section.getChildren().addAll(header, links);
        return section;
    }

    private Button navButton(String label, String pageId) {
        Button button = new Button(label);
        button.getStyleClass().add("shell-nav-item");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setPrefHeight(36);
        button.setOnAction(event -> navigateTo(pageId));
        navButtons.put(pageId, button);
        return button;
    }

    private HBox buildTopBar() {
        HBox topBar = new HBox();
        topBar.getStyleClass().add("shell-topbar");
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(16, 24, 16, 24));

        breadcrumbBase.getStyleClass().add("shell-breadcrumb-base");
        breadcrumbCurrent.getStyleClass().add("shell-breadcrumb-current");

        Label arrow = new Label("  >  ");
        arrow.getStyleClass().add("shell-breadcrumb-base");

        HBox breadcrumb = new HBox(breadcrumbBase, arrow, breadcrumbCurrent);
        breadcrumb.setAlignment(Pos.CENTER_LEFT);

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox termInfo = new VBox(1);
        termInfo.setAlignment(Pos.CENTER_RIGHT);

        Label termMain = new Label("Spring Semester 2026");
        termMain.getStyleClass().add("shell-term-main");

        Label termSub = new Label("BUPT International School");
        termSub.getStyleClass().add("shell-term-sub");

        termInfo.getChildren().addAll(termMain, termSub);

        topBar.getChildren().addAll(breadcrumb, spacer, termInfo);
        return topBar;
    }

    private void navigateTo(String pageId) {
        navButtons.values().forEach(this::deactivateButton);
        Button active = navButtons.get(pageId);
        if (active != null) {
            if (!active.getStyleClass().contains("shell-nav-item-active")) {
                active.getStyleClass().add("shell-nav-item-active");
            }
        }

        Parent page;
        switch (pageId) {
            case "dashboard" -> {
                breadcrumbCurrent.setText("Dashboard");
                page = new TADashboardController(services, user).getView();
            }
            case "browseJobs" -> {
                breadcrumbCurrent.setText("Browse Jobs");
                page = new JobBrowserController(services, user).getView();
            }
            case "myApplications" -> {
                breadcrumbCurrent.setText("My Application");
                page = new MyApplicationsController(services, user).getView();
            }
            case "myCv" -> {
                breadcrumbCurrent.setText("CV Management");
                page = new MyCvController(services, user, () -> navigateTo("browseJobs")).getView();
            }
            case "jobManagement" -> {
                breadcrumbCurrent.setText("Job Management");
                page = new JobManagementController(services, user, this::openApplicantListForJob).getView();
            }
            case "applicantList" -> {
                breadcrumbCurrent.setText("CS101 TA Applicants");
                page = new ApplicantListController(services, user, preferredApplicantJobId).getView();
            }
            case "adminDashboard" -> {
                breadcrumbCurrent.setText("Workload Monitoring");
                page = new AdminDashboardController(services, user).getView();
            }
            case "adminJobs" -> {
                breadcrumbCurrent.setText("Jobs");
                page = new AdminJobsController(services, user).getView();
            }
            case "adminApplications" -> {
                breadcrumbCurrent.setText("Applications");
                page = new AdminApplicationsController(services, user).getView();
            }
            case "moProfile" -> {
                breadcrumbCurrent.setText("Profile");
                page = PlaceholderPage.simple("MO Profile", "Planned in next visual alignment phase.");
            }
            case "helpCenter" -> {
                breadcrumbCurrent.setText("Help Center");
                page = PlaceholderPage.simple("Help Center", "Support documentation panel will be added in Phase C.");
            }
            case "settings" -> {
                breadcrumbCurrent.setText("Settings");
                page = PlaceholderPage.simple("Settings", "Settings panel will be added in Phase D.");
            }
            default -> {
                breadcrumbCurrent.setText("Dashboard");
                page = PlaceholderPage.simple("Dashboard", "Page not found.");
            }
        }

        contentPane.getChildren().setAll(page);
    }

    private void openApplicantListForJob(Job job) {
        preferredApplicantJobId = job == null ? null : job.getJobId();
        navigateTo("applicantList");
    }

    private void deactivateButton(Button button) {
        button.getStyleClass().remove("shell-nav-item-active");
    }

    private String roleEditionText() {
        if (user.getRole() == Role.TA) {
            return "TA EDITION";
        }
        if (user.getRole() == Role.MO) {
            return "MO EDITION";
        }
        return "ADMIN EDITION";
    }

    private record NavEntry(String label, String pageId) {
    }
}
