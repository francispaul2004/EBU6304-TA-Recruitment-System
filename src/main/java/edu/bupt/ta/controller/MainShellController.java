package edu.bupt.ta.controller;

import edu.bupt.ta.enums.Role;
import edu.bupt.ta.model.ApplicantProfile;
import edu.bupt.ta.model.Job;
import edu.bupt.ta.model.User;
import edu.bupt.ta.service.ServiceRegistry;
import edu.bupt.ta.ui.IconFactory;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MainShellController {

    private static final double SIDEBAR_WIDTH = 220;
    private static final Insets SIDEBAR_BRAND_PADDING = new Insets(18, 14, 16, 14);
    private static final Insets SIDEBAR_NAV_PADDING = new Insets(8, 8, 8, 8);
    private static final Insets SIDEBAR_FOOTER_PADDING = new Insets(10, 8, 10, 8);

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

        BorderPane mainArea = new BorderPane();
        mainArea.getStyleClass().add("shell-main-area");
        mainArea.setTop(buildTopBar());

        BorderPane center = new BorderPane();
        center.getStyleClass().add("shell-content");
        center.setCenter(contentPane);
        mainArea.setCenter(center);
        view.setCenter(mainArea);

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
        sidebar.setPrefWidth(SIDEBAR_WIDTH);
        sidebar.setMinWidth(SIDEBAR_WIDTH);
        sidebar.setMaxWidth(SIDEBAR_WIDTH);

        VBox brandSection = new VBox();
        brandSection.setPadding(SIDEBAR_BRAND_PADDING);

        StackPane brandIcon = IconFactory.badge(
                IconFactory.IconType.GRADUATION_CAP,
                46,
                Color.web("#354a5f"),
                Color.WHITE
        );
        brandIcon.setStyle("-fx-background-color: #354a5f; -fx-background-radius: 999;");

        Label brandTitle = new Label("BUPT IS RECRUITMENT");
        brandTitle.getStyleClass().add("shell-brand-title");
        brandTitle.setWrapText(true);
        brandTitle.setTextOverrun(OverrunStyle.CLIP);
        brandTitle.setMaxWidth(Double.MAX_VALUE);

        Label brandSub = new Label(roleEditionText());
        brandSub.getStyleClass().add("shell-brand-sub");
        brandSub.setMaxWidth(Double.MAX_VALUE);

        VBox brandText = new VBox(2, brandTitle, brandSub);
        brandText.setFillWidth(true);
        brandText.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(brandText, Priority.ALWAYS);

        HBox brandRow = new HBox(8, brandIcon, brandText);
        brandRow.setAlignment(Pos.CENTER_LEFT);

        brandSection.getChildren().add(brandRow);

        VBox navArea = new VBox(8);
        navArea.setPadding(SIDEBAR_NAV_PADDING);

        if (user.getRole() == Role.TA) {
            navArea.getChildren().add(buildSection("RECRUITMENT", List.of(
                    new NavEntry("Dashboard", "dashboard", IconFactory.IconType.DASHBOARD),
                    new NavEntry("Browse Jobs", "browseJobs", IconFactory.IconType.SEARCH),
                    new NavEntry("My Applications", "myApplications", IconFactory.IconType.CLIPBOARD),
                    new NavEntry("My CV", "myCv", IconFactory.IconType.FILE)
            )));
        } else if (user.getRole() == Role.MO) {
            navArea.getChildren().add(buildSection("RECRUITMENT", List.of(
                    new NavEntry("Job Management", "jobManagement", IconFactory.IconType.BRIEFCASE),
                    new NavEntry("Applicant List", "applicantList", IconFactory.IconType.USERS),
                    new NavEntry("Profile", "moProfile", IconFactory.IconType.USER)
            )));
        } else {
            navArea.getChildren().add(buildSection("RECRUITMENT", List.of(
                    new NavEntry("Dashboard", "adminDashboard", IconFactory.IconType.DASHBOARD),
                    new NavEntry("Jobs", "adminJobs", IconFactory.IconType.BRIEFCASE),
                    new NavEntry("Applications", "adminApplications", IconFactory.IconType.CLIPBOARD)
            )));
        }

        navArea.getChildren().add(buildSection("SUPPORT", List.of(
                new NavEntry("Help Center", "helpCenter", IconFactory.IconType.HELP),
                new NavEntry("Settings", "settings", IconFactory.IconType.SETTINGS)
        )));

        VBox footer = new VBox(12);
        footer.setPadding(SIDEBAR_FOOTER_PADDING);

        VBox profileCard = new VBox(10);
        profileCard.getStyleClass().add("shell-profile-card");
        profileCard.setPadding(new Insets(10));

        StackPane avatar = IconFactory.badge(
                IconFactory.IconType.USER,
                40,
                Color.web("#eef2f7"),
                Color.web("#64748b")
        );

        Label profileName = new Label(resolveProfileCardName());
        profileName.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #0f172a;");

        Label profileId = new Label("ID: " + user.getUserId());
        profileId.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748b;");

        VBox profileText = new VBox(2, profileName, profileId);
        HBox profileRow = new HBox(10, avatar, profileText);
        profileRow.setAlignment(Pos.CENTER_LEFT);
        profileCard.getChildren().add(profileRow);

        Button logout = new Button("Logout");
        logout.getStyleClass().add("secondary-button");
        logout.setMaxWidth(Double.MAX_VALUE);
        logout.setGraphic(IconFactory.glyph(IconFactory.IconType.LOGOUT, 14, Color.web("#64748b")));
        logout.setGraphicTextGap(8);
        logout.setContentDisplay(ContentDisplay.RIGHT);
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
        header.setPadding(new Insets(10, 8, 0, 8));

        VBox links = new VBox(6);
        for (NavEntry item : items) {
            links.getChildren().add(navButton(item));
        }

        section.getChildren().addAll(header, links);
        return section;
    }

    private Button navButton(NavEntry entry) {
        Button button = new Button(entry.label());
        button.getStyleClass().add("shell-nav-item");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setPrefHeight(36);
        button.setGraphic(IconFactory.glyph(entry.icon(), 18, Color.web("#475569")));
        button.setGraphicTextGap(7);
        button.setContentDisplay(ContentDisplay.LEFT);
        button.setOnAction(event -> navigateTo(entry.pageId()));
        navButtons.put(entry.pageId(), button);
        return button;
    }

    private HBox buildTopBar() {
        HBox topBar = new HBox();
        topBar.getStyleClass().add("shell-topbar");
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(16, 24, 16, 24));

        breadcrumbBase.getStyleClass().add("shell-breadcrumb-base");
        breadcrumbCurrent.getStyleClass().add("shell-breadcrumb-current");

        StackPane chevron = IconFactory.glyph(IconFactory.IconType.CHEVRON_RIGHT, 14, Color.web("#64748b"));

        HBox breadcrumb = new HBox(8, breadcrumbBase, chevron, breadcrumbCurrent);
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

        StackPane notification = IconFactory.notificationBell(24, Color.web("#64748b"), Color.web("#ef4444"));

        Region divider = new Region();
        divider.getStyleClass().add("shell-topbar-divider");
        divider.setMinWidth(1);
        divider.setPrefWidth(1);
        divider.setMaxWidth(1);
        divider.setPrefHeight(34);

        HBox right = new HBox(16, notification, divider, termInfo);
        right.setAlignment(Pos.CENTER_RIGHT);

        topBar.getChildren().addAll(breadcrumb, spacer, right);
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
                breadcrumbCurrent.setText("My CV");
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

    private String resolveProfileCardName() {
        if (user.getRole() == Role.TA) {
            ApplicantProfile profile = services.applicantProfileService().getOrCreateProfile(user.getUserId());
            if (profile.getFullName() != null && !profile.getFullName().isBlank()) {
                return profile.getFullName().trim();
            }
        }
        if (user.getDisplayName() != null && !user.getDisplayName().isBlank()) {
            return user.getDisplayName().trim();
        }
        return "User";
    }

    private record NavEntry(String label, String pageId, IconFactory.IconType icon) {
    }
}
