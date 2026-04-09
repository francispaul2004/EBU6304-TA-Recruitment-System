package edu.bupt.ta.controller;

import edu.bupt.ta.enums.ApplicationStatus;
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
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class MyApplicationsController {

    private final ServiceRegistry services;
    private final User user;
    private final BorderPane view = new BorderPane();

    private final ListView<Application> applicationList = new ListView<>();

    private final Label detailTitle = new Label("Application Details");
    private final Label detailStage = new Label("-");
    private final Label detailJob = new Label("-");
    private final Label detailDate = new Label("-");
    private final Label detailScore = new Label("-");
    private final Label detailMissing = new Label("-");
    private final Label detailDecision = new Label("-");

    private String applicantId;
    private List<Application> allApplications = List.of();
    private String activeFilter = "ALL";

    public MyApplicationsController(ServiceRegistry services, User user) {
        this.services = services;
        this.user = user;
        initialize();
        refresh();
    }

    public Parent getView() {
        return view;
    }

    private void initialize() {
        view.getStyleClass().add("app-surface");

        HBox top = new HBox();
        top.setPadding(new Insets(24, 20, 16, 20));

        Label title = new Label("My Application");
        title.getStyleClass().add("page-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refreshButton = new Button("Refresh");
        refreshButton.getStyleClass().add("secondary-button");
        refreshButton.setOnAction(event -> refresh());

        top.getChildren().addAll(title, spacer, refreshButton);
        view.setTop(top);
    }

    private void refresh() {
        applicantId = services.applicantProfileService().getOrCreateProfile(user.getUserId()).getApplicantId();
        allApplications = new ArrayList<>(services.applicationService().getApplicationsByApplicant(applicantId));

        if (allApplications.isEmpty()) {
            view.setCenter(buildEmptyState());
            return;
        }

        view.setCenter(buildNormalState());
        updateApplicationList();
    }

    private Parent buildEmptyState() {
        VBox root = new VBox(20);
        root.setAlignment(Pos.TOP_CENTER);
        root.setPadding(new Insets(34, 20, 20, 20));

        VBox hero = new VBox(14);
        hero.setAlignment(Pos.CENTER);
        hero.getStyleClass().add("panel-card");
        hero.setPadding(new Insets(50));
        hero.setMaxWidth(760);

        Label icon = new Label("No applications yet");
        icon.setStyle("-fx-font-size: 40px; -fx-font-weight: 700; -fx-text-fill: #0f172a;");

        Label subtitle = new Label("You have not applied to any TA positions. Start your journey by exploring open roles.");
        subtitle.setWrapText(true);
        subtitle.setStyle("-fx-font-size: 16px; -fx-text-fill: #64748b;");

        Button browse = new Button("Browse Open Jobs");
        browse.getStyleClass().add("primary-button");

        Button guide = new Button("View Guide");
        guide.getStyleClass().add("secondary-button");

        HBox actions = new HBox(12, browse, guide);
        actions.setAlignment(Pos.CENTER);

        hero.getChildren().addAll(icon, subtitle, actions);

        HBox guideRow = new HBox(16,
                guideCard("Complete Profile", "Ensure your CV and academic background are up to date in settings."),
                guideCard("Find a Role", "Search for modules that match your major and available hours."),
                guideCard("Track Progress", "Once you apply, track your interview and selection status here.")
        );
        guideRow.setMaxWidth(1040);

        root.getChildren().addAll(hero, guideRow);
        return root;
    }

    private VBox guideCard(String title, String body) {
        VBox card = new VBox(8);
        card.getStyleClass().add("panel-card");
        card.setPadding(new Insets(16));
        card.setPrefWidth(320);

        Label titleNode = new Label(title);
        titleNode.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #0f172a;");

        Label bodyNode = new Label(body);
        bodyNode.setWrapText(true);
        bodyNode.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

        card.getChildren().addAll(titleNode, bodyNode);
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private Parent buildNormalState() {
        HBox body = new HBox(16);
        body.setPadding(new Insets(0, 20, 20, 20));

        VBox statusRail = buildStatusRail();
        VBox listPanel = buildListPanel();
        VBox detailPanel = buildDetailPanel();

        HBox.setHgrow(listPanel, Priority.ALWAYS);
        body.getChildren().addAll(statusRail, listPanel, detailPanel);
        return body;
    }

    private VBox buildStatusRail() {
        VBox rail = new VBox(10);
        rail.getStyleClass().add("panel-card");
        rail.setPadding(new Insets(14));
        rail.setPrefWidth(180);

        Label heading = new Label("STATUS OVERVIEW");
        heading.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #94a3b8;");

        // 排除已取消的申请进行统计
        Map<ApplicationStatus, Long> counts = allApplications.stream()
                .filter(app -> app.getStatus() != ApplicationStatus.CANCELLED)
                .collect(Collectors.groupingBy(Application::getStatus, Collectors.counting()));

        rail.getChildren().add(heading);
        rail.getChildren().add(filterButton("ALL", counts.values().stream().mapToInt(Long::intValue).sum()));
        rail.getChildren().add(filterButton("UNDER_REVIEW", counts.getOrDefault(ApplicationStatus.UNDER_REVIEW, 0L).intValue()));
        rail.getChildren().add(filterButton("ACCEPTED", counts.getOrDefault(ApplicationStatus.ACCEPTED, 0L).intValue()));
        rail.getChildren().add(filterButton("REJECTED", counts.getOrDefault(ApplicationStatus.REJECTED, 0L).intValue()));
        rail.getChildren().add(filterButton("SUBMITTED", counts.getOrDefault(ApplicationStatus.SUBMITTED, 0L).intValue()));

        return rail;
    }

    private Button filterButton(String filter, int count) {
        String label = ("ALL".equals(filter) ? "All Applications" : filter.replace('_', ' ')) + "   " + count;
        Button button = new Button(label);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
        button.setStyle(filter.equals(activeFilter)
                ? "-fx-background-color: #eef2f5; -fx-text-fill: #334155; -fx-font-size: 12px; -fx-font-weight: 700; -fx-background-radius: 8;"
                : "-fx-background-color: transparent; -fx-text-fill: #475569; -fx-font-size: 12px; -fx-font-weight: 600;");
        button.setOnAction(event -> {
            activeFilter = filter;
            view.setCenter(buildNormalState());
            updateApplicationList();
        });
        return button;
    }

    private VBox buildListPanel() {
        VBox panel = new VBox(12);
        panel.getStyleClass().add("panel-card");
        panel.setPadding(new Insets(14));

        HBox filters = new HBox(8);
        filters.getChildren().add(chip("All Status"));
        filters.getChildren().add(chip("Spring 2026"));
        filters.getChildren().add(chip("Computer Science"));

        applicationList.setCellFactory(param -> new ApplicationCell(services));
        applicationList.setPrefHeight(720);
        VBox.setVgrow(applicationList, Priority.ALWAYS);

        applicationList.getSelectionModel().selectedItemProperty().addListener((obs, oldItem, newItem) -> updateDetail(newItem));

        panel.getChildren().addAll(filters, applicationList);
        return panel;
    }

    private Label chip(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-background-color: #eef2f5; -fx-text-fill: #354a5f; -fx-font-size: 12px; -fx-font-weight: 700; -fx-background-radius: 999; -fx-padding: 6 10 6 10;");
        return label;
    }

    private VBox buildDetailPanel() {
        VBox panel = new VBox(14);
        panel.getStyleClass().add("panel-card");
        panel.setPadding(new Insets(16));
        panel.setPrefWidth(320);

        detailTitle.setStyle("-fx-font-size: 34px; -fx-font-weight: 800; -fx-text-fill: #0f172a;");

        panel.getChildren().addAll(
                detailTitle,
                detailField("Current Stage", detailStage),
                detailField("Job", detailJob),
                detailField("Applied Date", detailDate),
                detailField("Match Score", detailScore),
                detailField("Missing Skills", detailMissing),
                detailField("Decision Note", detailDecision)
        );
        return panel;
    }

    private VBox detailField(String label, Label valueNode) {
        VBox box = new VBox(4);
        Label title = new Label(label);
        title.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #94a3b8;");
        valueNode.setWrapText(true);
        valueNode.setStyle("-fx-font-size: 13px; -fx-text-fill: #334155;");
        box.getChildren().addAll(title, valueNode);
        return box;
    }

    private void updateApplicationList() {
        List<Application> filtered = allApplications;

        // 过滤掉 CANCELLED 状态的申请
        filtered = filtered.stream()
                .filter(application -> application.getStatus() != ApplicationStatus.CANCELLED)
                .collect(Collectors.toList());

        if (!"ALL".equals(activeFilter)) {
            ApplicationStatus status = ApplicationStatus.valueOf(activeFilter);
            filtered = filtered.stream()
                    .filter(application -> application.getStatus() == status)
                    .toList();
        }

        applicationList.setItems(FXCollections.observableArrayList(filtered));
        if (!filtered.isEmpty()) {
            applicationList.getSelectionModel().selectFirst();
        } else {
            updateDetail(null);
        }
    }

    private void updateDetail(Application application) {
        if (application == null) {
            detailStage.setText("-");
            detailJob.setText("-");
            detailDate.setText("-");
            detailScore.setText("-");
            detailMissing.setText("-");
            detailDecision.setText("-");
            return;
        }

        Optional<Job> jobOpt = services.jobService().getJob(application.getJobId());

        detailStage.setText(application.getStatus().name().replace('_', ' '));
        detailJob.setText(jobOpt.map(Job::getTitle).orElse(application.getJobId()));
        detailDate.setText(application.getApplyDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        detailScore.setText(application.getMatchScore() + "%");
        detailMissing.setText(application.getMissingSkills().isEmpty() ? "None" : String.join(", ", application.getMissingSkills()));
        detailDecision.setText(application.getDecisionNote() == null || application.getDecisionNote().isBlank()
                ? "No note"
                : application.getDecisionNote());
    }

    private static class ApplicationCell extends ListCell<Application> {
        private final ServiceRegistry services;

        private ApplicationCell(ServiceRegistry services) {
            this.services = services;
        }

        @Override
        protected void updateItem(Application item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                return;
            }

            VBox row = new VBox(6);
            row.setPadding(new Insets(12));
            row.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; -fx-border-radius: 10; -fx-background-radius: 10;");

            String title = services.jobService().getJob(item.getJobId()).map(Job::getTitle).orElse(item.getJobId());

            HBox top = new HBox();
            Label titleNode = new Label(title);
            titleNode.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: #0f172a;");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label status = new Label(item.getStatus().name().replace('_', ' '));
            status.setStyle(statusStyle(item.getStatus()));

            top.getChildren().addAll(titleNode, spacer, status);

            Label meta = new Label("ID: " + item.getApplicationId() + "   |   Applied "
                    + item.getApplyDate().format(DateTimeFormatter.ofPattern("MMM dd")));
            meta.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

            row.getChildren().addAll(top, meta);
            setGraphic(row);
        }

        private String statusStyle(ApplicationStatus status) {
            return switch (status) {
                case ACCEPTED -> "-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #047857; -fx-background-color: #ecfdf5; -fx-background-radius: 999; -fx-padding: 2 8 2 8;";
                case REJECTED -> "-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #b91c1c; -fx-background-color: #fef2f2; -fx-background-radius: 999; -fx-padding: 2 8 2 8;";
                case UNDER_REVIEW -> "-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #1d4ed8; -fx-background-color: #eff6ff; -fx-background-radius: 999; -fx-padding: 2 8 2 8;";
                case CANCELLED -> "-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #b45309; -fx-background-color: #fffbeb; -fx-background-radius: 999; -fx-padding: 2 8 2 8;";
                default -> "-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #b45309; -fx-background-color: #fffbeb; -fx-background-radius: 999; -fx-padding: 2 8 2 8;";
            };
        }
    }
}
