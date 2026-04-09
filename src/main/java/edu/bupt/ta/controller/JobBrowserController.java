package edu.bupt.ta.controller;

import edu.bupt.ta.dto.JobSearchCriteria;
import edu.bupt.ta.dto.MatchExplanationDTO;
import edu.bupt.ta.enums.JobStatus;
import edu.bupt.ta.enums.Role;
import edu.bupt.ta.model.Job;
import edu.bupt.ta.model.User;
import edu.bupt.ta.service.ServiceRegistry;
import edu.bupt.ta.util.ValidationResult;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class JobBrowserController {
    private static final DateTimeFormatter DEADLINE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");


    private final ServiceRegistry services;
    private final User user;
    private final BorderPane view = new BorderPane();

    private final TextField keywordField = new TextField();
    private final ComboBox<String> statusFilter = new ComboBox<>();
    private final ListView<Job> jobList = new ListView<>();
    private final JobDetailController jobDetailController = new JobDetailController();

    public JobBrowserController(ServiceRegistry services, User user) {
        this.services = services;
        this.user = user;
        initialize();
        loadJobs();
    }

    public Parent getView() {
        return view;
    }

    private void initialize() {
        view.getStyleClass().add("app-surface");
        view.setTop(buildFilters());

        HBox content = new HBox(20);
        content.setPadding(new Insets(20));

        VBox listPanel = new VBox(12);
        listPanel.setPrefWidth(420);
        listPanel.setMinWidth(360);

        Label listTitle = new Label("Open Positions");
        listTitle.getStyleClass().add("section-title");

        jobList.setCellFactory(param -> new JobCardCell());
        jobList.setPrefHeight(760);
        VBox.setVgrow(jobList, Priority.ALWAYS);

        listPanel.getChildren().addAll(listTitle, jobList);

        HBox.setHgrow(jobDetailController.getView(), Priority.ALWAYS);
        content.getChildren().addAll(listPanel, jobDetailController.getView());
        view.setCenter(content);

        jobList.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            jobDetailController.setJob(newValue);
            updateMatchExplanation(newValue);
        });

        jobDetailController.setOnApply(statement -> {
            Job selected = jobList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                applyToJob(selected, statement);
            }
        });

        if (user.getRole() != Role.TA) {
            jobDetailController.setOnApply(statement -> {
            });
            jobDetailController.setMatchExplanation(null);
        }
    }

    private Parent buildFilters() {
        VBox wrapper = new VBox(14);
        wrapper.setPadding(new Insets(24, 20, 14, 20));
        wrapper.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; -fx-border-width: 0 0 1 0;");

        Label heading = new Label("Browse Opportunities");
        heading.getStyleClass().add("page-title");

        HBox searchRow = new HBox(12);
        keywordField.setPromptText("Search by Keyword (e.g. CS101, Python, Web Dev)");
        keywordField.setPrefHeight(42);

        Button searchButton = new Button("SEARCH");
        searchButton.getStyleClass().add("primary-button");
        searchButton.setOnAction(event -> loadJobs());

        HBox.setHgrow(keywordField, Priority.ALWAYS);
        searchRow.getChildren().addAll(keywordField, searchButton);

        statusFilter.getItems().addAll("ALL", "OPEN", "CLOSED", "EXPIRED", "DRAFT");
        statusFilter.setValue("ALL");

        HBox filters = new HBox(10);
        filters.setAlignment(Pos.CENTER_LEFT);

        ComboBox<String> moduleCode = new ComboBox<>();
        moduleCode.getItems().addAll("Module Code");
        moduleCode.setValue("Module Code");

        ComboBox<String> jobType = new ComboBox<>();
        jobType.getItems().addAll("Job Type");
        jobType.setValue("Job Type");

        ComboBox<String> weeklyHours = new ComboBox<>();
        weeklyHours.getItems().addAll("Weekly Hours");
        weeklyHours.setValue("Weekly Hours");

        Button clear = new Button("CLEAR FILTERS");
        clear.setStyle("-fx-background-color: transparent; -fx-text-fill: #ef4444; -fx-font-size: 12px; -fx-font-weight: 700;");
        clear.setOnAction(event -> {
            keywordField.clear();
            statusFilter.setValue("ALL");
            loadJobs();
        });

        filters.getChildren().addAll(moduleCode, jobType, weeklyHours, statusFilter, clear);

        wrapper.getChildren().addAll(heading, searchRow, filters);
        return wrapper;
    }

    private void loadJobs() {
        JobSearchCriteria criteria = new JobSearchCriteria();
        criteria.setKeyword(keywordField.getText());
        if (!"ALL".equals(statusFilter.getValue())) {
            criteria.setStatus(JobStatus.valueOf(statusFilter.getValue()));
        }

        List<Job> jobs = services.jobService().searchJobs(criteria);
        if (user.getRole() == Role.MO) {
            jobs = jobs.stream().filter(job -> user.getUserId().equals(job.getOrganiserId())).toList();
        }

        jobList.setItems(FXCollections.observableArrayList(jobs));

        if (!jobs.isEmpty()) {
            jobList.getSelectionModel().selectFirst();
        } else {
            jobDetailController.setJob(null);
            jobDetailController.setMatchExplanation(null);
        }
    }

    private void applyToJob(Job job, String statement) {
        Optional<String> applicantIdOpt = services.applicantProfileRepository()
                .findByUserId(user.getUserId())
                .map(profile -> profile.getApplicantId());

        if (applicantIdOpt.isEmpty()) {
            DialogControllerFactory.permissionDenied(
                    "Profile not found for current TA account. Please complete your profile first.",
                    view.getScene() == null ? null : view.getScene().getWindow());
            return;
        }

        ValidationResult result = services.applicationService().apply(applicantIdOpt.get(), job.getJobId(), statement);
        if (!result.isValid()) {
            DialogControllerFactory.operationFailed("Apply Failed", String.join("\n", result.getErrors()),
                    view.getScene() == null ? null : view.getScene().getWindow());
            return;
        }

        DialogControllerFactory.success("Apply Success", "Application submitted successfully.",
                view.getScene() == null ? null : view.getScene().getWindow());
        loadJobs();
    }

    private void updateMatchExplanation(Job selectedJob) {
        if (selectedJob == null || user.getRole() != Role.TA) {
            jobDetailController.setMatchExplanation(null);
            return;
        }

        Optional<String> applicantIdOpt = services.applicantProfileRepository()
                .findByUserId(user.getUserId())
                .map(profile -> profile.getApplicantId());
        if (applicantIdOpt.isEmpty()) {
            jobDetailController.setMatchExplanation(null);
            return;
        }

        MatchExplanationDTO explanation = services.matchingService().evaluateMatch(applicantIdOpt.get(), selectedJob.getJobId());
        jobDetailController.setMatchExplanation(explanation);
    }

    private static class JobCardCell extends ListCell<Job> {
        @Override
        protected void updateItem(Job item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                return;
            }

            VBox card = new VBox(6);
            card.setPadding(new Insets(14));
            card.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; -fx-border-radius: 12; -fx-background-radius: 12;");

            HBox header = new HBox();

            Label title = new Label(item.getTitle());
            title.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #0f172a;");
            title.setWrapText(true);

            Label status = new Label(item.getStatus().name());
            status.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #047857; -fx-background-color: #ecfdf5; -fx-background-radius: 999; -fx-padding: 2 8 2 8;");

            HBox spacer = new HBox();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            header.getChildren().addAll(title, spacer, status);

            Label module = new Label(item.getModuleCode() + " | " + item.getModuleName());
            module.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #00a58a;");

            Label meta = new Label(item.getWeeklyHours() + "h/week   |   Deadline: " + formatDeadline(item.getDeadline()));
            meta.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

            card.getChildren().addAll(header, module, meta);
            setGraphic(card);
        }
    }

    private static String formatDeadline(LocalDateTime deadline) {
        return deadline == null ? "-" : deadline.format(DEADLINE_FORMAT);
    }
}
