package edu.bupt.ta.controller;

import edu.bupt.ta.enums.JobStatus;
import edu.bupt.ta.model.Application;
import edu.bupt.ta.model.Job;
import edu.bupt.ta.model.User;
import edu.bupt.ta.service.ServiceRegistry;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

public class TADashboardController {

    private final ServiceRegistry services;
    private final User user;
    private final BorderPane view = new BorderPane();

    public TADashboardController(ServiceRegistry services, User user) {
        this.services = services;
        this.user = user;
        initialize();
    }

    public Parent getView() {
        return view;
    }

    private void initialize() {
        view.getStyleClass().add("app-surface");

        String applicantId = services.applicantProfileService().getOrCreateProfile(user.getUserId()).getApplicantId();
        int profileCompletion = services.applicantProfileService().calculateProfileCompletion(applicantId);
        int resumeCompletion = services.resumeService().calculateResumeCompletion(applicantId);
        List<Application> applications = services.applicationService().getApplicationsByApplicant(applicantId);
        int applicationCount = applications.size();
        int currentHours = services.workloadRepository().findByApplicantId(applicantId)
                .map(workload -> workload.getCurrentWeeklyHours())
                .orElse(0);

        VBox page = new VBox(24);
        page.setPadding(new Insets(32));

        page.getChildren().add(buildWelcome(applicationCount));
        page.getChildren().add(buildStats(profileCompletion, resumeCompletion, currentHours, applicationCount));
        page.getChildren().add(buildMainArea(currentHours));

        ScrollPane scrollPane = new ScrollPane(page);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");

        view.setCenter(scrollPane);
    }

    private HBox buildWelcome(int applicationCount) {
        Label title = new Label("Welcome back, " + firstName(user.getDisplayName()));
        title.getStyleClass().add("page-title");

        Label subtitle = new Label("You have " + applicationCount + " active applications for this recruitment cycle.");
        subtitle.getStyleClass().add("body-muted");

        VBox left = new VBox(4, title, subtitle);

        Button viewSchedule = new Button("View Schedule");
        viewSchedule.getStyleClass().add("secondary-button");

        Button browseJobs = new Button("Browse New Jobs");
        browseJobs.getStyleClass().add("primary-button");

        HBox right = new HBox(12, viewSchedule, browseJobs);
        right.setAlignment(Pos.CENTER_RIGHT);

        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(left, Priority.ALWAYS);
        row.getChildren().addAll(left, right);
        return row;
    }

    private HBox buildStats(int profileCompletion, int resumeCompletion, int currentHours, int applicationCount) {
        HBox stats = new HBox(16);
        stats.getChildren().addAll(
                statCard("PROFILE STATUS", profileCompletion + "%", profileCompletion >= 80 ? "Almost Done" : "In Progress"),
                statCard("CV STATUS", resumeCompletion >= 80 ? "Uploaded" : "Incomplete", resumeCompletion + "% complete"),
                statCard("CURRENT WORKLOAD", currentHours + " / 20 hrs/wk", currentHours > 14 ? "Heavy" : "Balanced"),
                statCard("MY APPLICATIONS", String.format("%02d", applicationCount), "Active")
        );
        return stats;
    }

    private VBox statCard(String label, String value, String subtext) {
        VBox card = new VBox(6);
        card.getStyleClass().add("panel-card");
        card.setPadding(new Insets(18));
        card.setMinWidth(220);

        Label labelNode = new Label(label);
        labelNode.getStyleClass().add("kpi-card-title");

        Label valueNode = new Label(value);
        valueNode.getStyleClass().add("kpi-card-value");
        valueNode.setStyle("-fx-font-size: 34px; -fx-font-weight: 800; -fx-text-fill: #334155;");

        Label subNode = new Label(subtext);
        subNode.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

        card.getChildren().addAll(labelNode, valueNode, subNode);
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private HBox buildMainArea(int currentHours) {
        VBox left = new VBox(20);
        VBox right = new VBox(20);

        HBox.setHgrow(left, Priority.ALWAYS);
        right.setMinWidth(320);
        right.setPrefWidth(320);

        left.getChildren().add(buildRecentJobsCard());
        left.getChildren().add(buildRecommendedCard());

        right.getChildren().add(buildQuickActionsCard());
        right.getChildren().add(buildStatusCheckCard(currentHours));
        right.getChildren().add(buildDeadlineCard());

        HBox body = new HBox(20, left, right);
        return body;
    }

    private VBox buildRecentJobsCard() {
        VBox card = new VBox(10);
        card.getStyleClass().add("panel-card");
        card.setPadding(new Insets(20));

        HBox header = new HBox();
        Label title = new Label("Recent Open Jobs");
        title.getStyleClass().add("section-title");

        Label viewAll = new Label("View All");
        viewAll.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #334155;");

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(title, spacer, viewAll);

        TableView<JobRow> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(250);

        TableColumn<JobRow, String> module = new TableColumn<>("COURSE CODE");
        module.setCellValueFactory(new PropertyValueFactory<>("moduleCode"));

        TableColumn<JobRow, String> titleCol = new TableColumn<>("JOB TITLE");
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));

        TableColumn<JobRow, String> hours = new TableColumn<>("HOURS");
        hours.setCellValueFactory(new PropertyValueFactory<>("hours"));

        TableColumn<JobRow, String> action = new TableColumn<>("ACTION");
        action.setCellValueFactory(new PropertyValueFactory<>("action"));

        table.getColumns().setAll(module, titleCol, hours, action);

        List<JobRow> rows = services.jobService().searchJobs(null).stream()
                .filter(job -> job.getStatus() == JobStatus.OPEN)
                .limit(4)
                .map(job -> new JobRow(job.getModuleCode(), job.getTitle(), job.getWeeklyHours() + "h/week", "Apply"))
                .toList();
        table.setItems(FXCollections.observableArrayList(rows));

        card.getChildren().addAll(header, table);
        return card;
    }

    private VBox buildRecommendedCard() {
        VBox card = new VBox(14);
        card.getStyleClass().add("panel-card");
        card.setPadding(new Insets(20));

        Label title = new Label("Recommended for You");
        title.getStyleClass().add("section-title");

        HBox cards = new HBox(12,
                recommendation("Data Structures Lab Assistant", "$25/hr"),
                recommendation("Python Workshop Lead", "$30/hr")
        );

        card.getChildren().addAll(title, cards);
        return card;
    }

    private VBox recommendation(String title, String pay) {
        VBox rec = new VBox(8);
        rec.getStyleClass().add("soft-card");
        rec.setPadding(new Insets(14));
        rec.setPrefWidth(260);

        Label tag = new Label("BUPT-IS");
        tag.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #354a5f; -fx-background-color: #eef2f5; -fx-background-radius: 4; -fx-padding: 2 8 2 8;");

        Label titleNode = new Label(title);
        titleNode.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #0f172a;");

        Label description = new Label("Recommended by your profile, skill overlap, and workload balance.");
        description.setWrapText(true);
        description.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

        Label payNode = new Label(pay);
        payNode.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #334155;");

        rec.getChildren().addAll(tag, titleNode, description, payNode);
        HBox.setHgrow(rec, Priority.ALWAYS);
        return rec;
    }

    private VBox buildQuickActionsCard() {
        VBox card = new VBox(12);
        card.setPadding(new Insets(20));
        card.setStyle("-fx-background-color: #00c29f; -fx-background-radius: 12;");

        Label title = new Label("Quick Actions");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: 700; -fx-text-fill: white;");

        card.getChildren().add(title);
        card.getChildren().add(actionButton("Complete Profile"));
        card.getChildren().add(actionButton("Upload CV / Portfolio"));
        card.getChildren().add(actionButton("Browse Jobs"));
        card.getChildren().add(actionButton("View Applications"));
        return card;
    }

    private Button actionButton(String text) {
        Button button = new Button(text);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setStyle("-fx-background-color: rgba(255,255,255,0.14); -fx-background-radius: 8; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: 600; -fx-alignment: CENTER_LEFT;");
        return button;
    }

    private VBox buildStatusCheckCard(int currentHours) {
        VBox card = new VBox(12);
        card.getStyleClass().add("panel-card");
        card.setPadding(new Insets(20));

        Label title = new Label("Status Check");
        title.getStyleClass().add("section-title");

        VBox warning1 = messageCard("Missing Documents", "Transcript of Records is required.", "#fef2f2", "#991b1b");
        VBox warning2 = messageCard(
                currentHours > 14 ? "Workload Warning" : "Profile Incomplete",
                currentHours > 14 ? "Your weekly hours are close to limit." : "Add your teaching philosophy.",
                currentHours > 14 ? "#fff7ed" : "#fffbeb",
                currentHours > 14 ? "#c2410c" : "#92400e"
        );

        card.getChildren().addAll(title, warning1, warning2);
        return card;
    }

    private VBox messageCard(String title, String body, String bg, String fg) {
        VBox box = new VBox(2);
        box.setPadding(new Insets(10));
        box.setStyle("-fx-background-color: " + bg + "; -fx-border-color: #fde68a; -fx-border-radius: 8; -fx-background-radius: 8;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: " + fg + ";");

        Label bodyLabel = new Label(body);
        bodyLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + fg + ";");

        box.getChildren().addAll(titleLabel, bodyLabel);
        return box;
    }

    private VBox buildDeadlineCard() {
        VBox card = new VBox(12);
        card.getStyleClass().add("panel-card");
        card.setPadding(new Insets(20));

        Label title = new Label("Recruitment Deadlines");
        title.getStyleClass().add("section-title");

        Label d1 = new Label("15 OCT  Priority Application");
        d1.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #334155;");

        Label d2 = new Label("30 SEP  System Orientation (Passed)");
        d2.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8;");

        card.getChildren().addAll(title, d1, d2);
        return card;
    }

    private String firstName(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            return "User";
        }
        String[] parts = displayName.trim().split("\\s+");
        return parts.length == 0 ? displayName : parts[0];
    }

    public static class JobRow {
        private final String moduleCode;
        private final String title;
        private final String hours;
        private final String action;

        public JobRow(String moduleCode, String title, String hours, String action) {
            this.moduleCode = moduleCode;
            this.title = title;
            this.hours = hours;
            this.action = action;
        }

        public String getModuleCode() {
            return moduleCode;
        }

        public String getTitle() {
            return title;
        }

        public String getHours() {
            return hours;
        }

        public String getAction() {
            return action;
        }
    }
}
