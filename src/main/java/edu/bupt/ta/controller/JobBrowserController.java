package edu.bupt.ta.controller;

import edu.bupt.ta.dto.JobSearchCriteria;
import edu.bupt.ta.dto.MatchExplanationDTO;
import edu.bupt.ta.enums.ApplicationStatus;
import edu.bupt.ta.enums.JobStatus;
import edu.bupt.ta.enums.JobType;
import edu.bupt.ta.enums.Role;
import edu.bupt.ta.model.ApplicantProfile;
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
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class JobBrowserController {

    private static final String JOB_TYPE_ALL = "All types";

    private final ServiceRegistry services;
    private final User user;
    private final BorderPane view = new BorderPane();

    private final TextField keywordField = new TextField();
    private final ComboBox<String> statusFilter = new ComboBox<>();
    private final ComboBox<String> jobTypeFilter = new ComboBox<>();
    private final ListView<JobWithApplication> jobList = new ListView<>();
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
        listPanel.setPrefWidth(460);
        listPanel.setMinWidth(380);

        Label listTitle = new Label("Open Positions");
        listTitle.getStyleClass().add("section-title");

        jobList.setCellFactory(param -> new JobCardCell());
        jobList.setPrefHeight(760);
        jobList.getStyleClass().add("job-open-positions-list");
        VBox.setVgrow(jobList, Priority.ALWAYS);

        listPanel.getChildren().addAll(listTitle, jobList);

        HBox.setHgrow(jobDetailController.getView(), Priority.ALWAYS);
        content.getChildren().addAll(listPanel, jobDetailController.getView());
        view.setCenter(content);

        jobList.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                jobDetailController.setJobWithApplicationStatus(newValue.job, getCurrentApplicantId(), newValue.appStatus);
                updateMatchExplanation(newValue.job);
            } else {
                jobDetailController.setJob(null);
                updateMatchExplanation(null);
            }
        });

        jobDetailController.setOnApply(() -> {
            JobWithApplication selected = jobList.getSelectionModel().getSelectedItem();
            if (selected == null) {
                return;
            }
            Optional<String> applicantIdOpt = services.applicantProfileRepository()
                    .findByUserId(user.getUserId())
                    .map(profile -> profile.getApplicantId());
            if (applicantIdOpt.isEmpty()) {
                DialogControllerFactory.permissionDenied(
                        "Profile not found for current TA account. Please complete your profile first.",
                        view.getScene() == null ? null : view.getScene().getWindow());
                return;
            }
            Optional<ApplicantProfile> profileOpt = services.applicantProfileRepository()
                    .findById(applicantIdOpt.get());
            if (profileOpt.isEmpty()) {
                DialogControllerFactory.permissionDenied(
                        "Profile not found for current TA account. Please complete your profile first.",
                        view.getScene() == null ? null : view.getScene().getWindow());
                return;
            }
            Optional<String> statementOpt = JobApplyDialog.showAndWait(
                    selected.job,
                    profileOpt.get(),
                    services.resumeService(),
                    applicantIdOpt.get(),
                    view.getScene() == null ? null : view.getScene().getWindow());
            statementOpt.ifPresent(statement -> applyToJob(selected.job, statement));
        });

        jobDetailController.setOnCancel(applicantId -> {
            JobWithApplication selected = jobList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                cancelApplication(selected.job);
            }
        });

        if (user.getRole() != Role.TA) {
            jobDetailController.setOnApply(() -> {
            });
            jobDetailController.setOnCancel(applicantId -> {
            });
            jobDetailController.setMatchExplanation(null);
        }
    }

    private String getCurrentApplicantId() {
        if (user.getRole() != Role.TA) {
            return null;
        }

        Optional<String> applicantIdOpt = services.applicantProfileRepository()
                .findByUserId(user.getUserId())
                .map(profile -> profile.getApplicantId());

        return applicantIdOpt.orElse(null);
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

        statusFilter.getItems().addAll(
                "ALL",
                "OPEN",
                "CLOSED",
                "EXPIRED",
                "ACCEPTED",
                "REJECTED");
        statusFilter.setValue("ALL");
        statusFilter.setPromptText("Status");
        statusFilter.setMinWidth(150);
        statusFilter.setPrefWidth(160);
        statusFilter.setVisibleRowCount(10);
        if (!statusFilter.getStyleClass().contains("status-selector")) {
            statusFilter.getStyleClass().add("status-selector");
        }
        statusFilter.valueProperty().addListener((obs, oldV, newV) -> {
            if (oldV != null) {
                loadJobs();
            }
        });

        HBox filters = new HBox(10);
        filters.setAlignment(Pos.CENTER_LEFT);

        ComboBox<String> moduleCode = new ComboBox<>();
        moduleCode.getItems().addAll("Module Code");
        moduleCode.setValue("Module Code");

        jobTypeFilter.getItems().setAll(
                JOB_TYPE_ALL,
                "Module TA",
                "Invigilation",
                "Activity support",
                "Other");
        jobTypeFilter.setValue(JOB_TYPE_ALL);
        jobTypeFilter.setPromptText("Job Type");
        jobTypeFilter.setPrefWidth(150);
        jobTypeFilter.valueProperty().addListener((obs, oldV, newV) -> {
            if (oldV != null) {
                loadJobs();
            }
        });

        ComboBox<String> weeklyHours = new ComboBox<>();
        weeklyHours.getItems().addAll("Weekly Hours");
        weeklyHours.setValue("Weekly Hours");

        Button clear = new Button("CLEAR FILTERS");
        clear.setStyle("-fx-background-color: transparent; -fx-text-fill: #ef4444; -fx-font-size: 12px; -fx-font-weight: 700;");
        clear.setOnAction(event -> {
            keywordField.clear();
            statusFilter.setValue("ALL");
            jobTypeFilter.setValue(JOB_TYPE_ALL);
            loadJobs();
        });

        filters.getChildren().addAll(moduleCode, jobTypeFilter, weeklyHours, statusFilter, clear);

        wrapper.getChildren().addAll(heading, searchRow, filters);
        return wrapper;
    }

    private static JobType jobTypeToCriteriaValue(String display) {
        if (display == null || display.isBlank() || JOB_TYPE_ALL.equals(display)) {
            return null;
        }
        return switch (display) {
            case "Module TA" -> JobType.MODULE_TA;
            case "Invigilation" -> JobType.INVIGILATION;
            case "Activity support" -> JobType.ACTIVITY_SUPPORT;
            case "Other" -> JobType.OTHER;
            default -> null;
        };
    }

    private void loadJobs() {
        JobSearchCriteria criteria = new JobSearchCriteria();
        criteria.setKeyword(keywordField.getText());
        String statusSelection = statusFilter.getValue();
        ApplicationStatus applicationStatusFilter = null;
        if (!"ALL".equals(statusSelection)) {
            if ("ACCEPTED".equals(statusSelection)) {
                applicationStatusFilter = ApplicationStatus.ACCEPTED;
            } else if ("REJECTED".equals(statusSelection)) {
                applicationStatusFilter = ApplicationStatus.REJECTED;
            } else {
                criteria.setStatus(JobStatus.valueOf(statusSelection));
            }
        }

        JobType selectedType = jobTypeToCriteriaValue(jobTypeFilter.getValue());
        if (selectedType != null) {
            criteria.setType(selectedType);
        }

        List<Job> jobs = services.jobService().searchJobs(criteria);
        if (user.getRole() == Role.MO) {
            jobs = jobs.stream().filter(job -> user.getUserId().equals(job.getOrganiserId())).toList();
        }

        if (user.getRole() == Role.TA) {
            jobs = jobs.stream()
                    .filter(job -> job.getStatus() != JobStatus.DRAFT)
                    .collect(java.util.stream.Collectors.toList());
        }

        List<JobWithApplication> jobsWithApp = new ArrayList<>();
        for (Job job : jobs) {
            ApplicationStatus appStatus = getApplicationStatusForJob(job);
            jobsWithApp.add(new JobWithApplication(job, appStatus));
        }

        if (applicationStatusFilter != null) {
            final ApplicationStatus filterByAppStatus = applicationStatusFilter;
            jobsWithApp = new ArrayList<>(jobsWithApp.stream()
                    .filter(jwa -> filterByAppStatus.equals(jwa.appStatus()))
                    .toList());
        }

        if (user.getRole() == Role.TA && applicationStatusFilter == null && statusSelection != null) {
            String matchLabel = switch (statusSelection) {
                case "CLOSED" -> "Closed";
                case "EXPIRED" -> "Expired";
                default -> null;
            };
            if (matchLabel != null) {
                final String expected = matchLabel;
                jobsWithApp = new ArrayList<>(jobsWithApp.stream()
                        .filter(jwa -> expected.equals(cardStatusLabel(jwa.job(), jwa.appStatus())))
                        .toList());
            }
        }

        jobsWithApp.sort(
                Comparator.comparingInt((JobWithApplication jwa) -> getSortOrder(jwa.job(), jwa.appStatus()))
                        .thenComparing(jwa -> jwa.job().getTitle(), String.CASE_INSENSITIVE_ORDER));

        jobList.setItems(FXCollections.observableArrayList(jobsWithApp));

        if (!jobsWithApp.isEmpty()) {
            jobList.getSelectionModel().selectFirst();
        } else {
            jobDetailController.setJob(null);
            jobDetailController.setMatchExplanation(null);
        }
    }

    private record JobWithApplication(Job job, ApplicationStatus appStatus) {}

    private ApplicationStatus getApplicationStatusForJob(Job job) {
        if (user.getRole() != Role.TA) {
            return null;
        }

        Optional<String> applicantIdOpt = services.applicantProfileRepository()
                .findByUserId(user.getUserId())
                .map(profile -> profile.getApplicantId());

        if (applicantIdOpt.isEmpty()) {
            return null;
        }

        return services.applicationService().getApplicationStatus(applicantIdOpt.get(), job.getJobId())
                .orElse(null);
    }

    private int getSortOrder(Job job, ApplicationStatus appStatus) {
        if (appStatus == ApplicationStatus.ACCEPTED) {
            return 0;
        }
        if (appStatus == ApplicationStatus.REJECTED) {
            return 2;
        }
        if (appStatus == ApplicationStatus.SUBMITTED
                || appStatus == ApplicationStatus.UNDER_REVIEW
                || appStatus == ApplicationStatus.CANCELLED) {
            return 1;
        }
        return switch (job.getStatus()) {
            case OPEN -> 1;
            case EXPIRED -> 3;
            case CLOSED -> 4;
            case DRAFT -> 5;
        };
    }

    private static String cardStatusLabel(Job job, ApplicationStatus appStatus) {
        if (appStatus != null) {
            return switch (appStatus) {
                case ACCEPTED -> "Accepted";
                case SUBMITTED -> "Applied";
                case CANCELLED -> "Cancelled";
                case UNDER_REVIEW -> "Under Review";
                case REJECTED -> "Rejected";
            };
        }
        return switch (job.getStatus()) {
            case OPEN -> "Open";
            case CLOSED -> "Closed";
            case EXPIRED -> "Expired";
            case DRAFT -> "Draft";
        };
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

    private void cancelApplication(Job job) {
        Optional<String> applicantIdOpt = services.applicantProfileRepository()
                .findByUserId(user.getUserId())
                .map(profile -> profile.getApplicantId());

        if (applicantIdOpt.isEmpty()) {
            return;
        }

        ValidationResult result = services.applicationService().cancelApplication(applicantIdOpt.get(), job.getJobId());
        if (!result.isValid()) {
            DialogControllerFactory.operationFailed("Cancel Failed", String.join("\n", result.getErrors()),
                    view.getScene() == null ? null : view.getScene().getWindow());
            return;
        }

        DialogControllerFactory.success("Cancel Success", "Application cancelled successfully.",
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

    private static class JobCardCell extends ListCell<JobWithApplication> {
        @Override
        protected void updateItem(JobWithApplication item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                setPadding(Insets.EMPTY);
                return;
            }

            Job job = item.job();
            ApplicationStatus appStatus = item.appStatus();

            VBox card = new VBox(6);
            card.setPadding(new Insets(14, 14, 14, 14));
            card.setMaxWidth(Double.MAX_VALUE);
            card.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; -fx-border-radius: 12; -fx-background-radius: 12;");

            Label title = new Label(job.getTitle());
            title.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #0f172a;");
            title.setWrapText(true);
            title.setMaxWidth(Double.MAX_VALUE);

            String statusText = cardStatusLabel(job, appStatus);
            String statusStyle = getDynamicStatusStyle(statusText);

            Label status = new Label(statusText);
            status.setStyle(statusStyle);
            status.setMinWidth(Region.USE_PREF_SIZE);
            status.setMaxWidth(Region.USE_PREF_SIZE);
            status.setWrapText(false);

            BorderPane header = new BorderPane();
            header.setMaxWidth(Double.MAX_VALUE);
            BorderPane.setMargin(status, new Insets(0, 4, 0, 12));
            header.setCenter(title);
            header.setRight(status);
            BorderPane.setAlignment(title, Pos.TOP_LEFT);
            BorderPane.setAlignment(status, Pos.TOP_RIGHT);

            Label module = new Label(job.getModuleCode() + " | " + job.getModuleName());
            module.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #00a58a;");

            Label meta = new Label(job.getWeeklyHours() + "h/week   |   Deadline: " + job.getDeadline());
            meta.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

            card.getChildren().addAll(header, module, meta);
            setGraphic(card);
        }

        private String getDynamicStatusStyle(String statusText) {
            String color = switch (statusText) {
                case "Accepted" -> "#047857;#ecfdf5";
                case "Applied", "Under Review" -> "#1d4ed8;#eff6ff";
                case "Rejected" -> "#b91c1c;#fef2f2";
                case "Cancelled" -> "#b45309;#fffbeb";
                case "Open" -> "#047857;#ecfdf5";
                case "Closed", "Expired" -> "#64748b;#f1f5f9";
                default -> "#64748b;#f1f5f9";
            };
            String[] parts = color.split(";");
            return String.format("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: %s; -fx-background-color: %s; -fx-background-radius: 999; -fx-padding: 2 8 2 8;", parts[0], parts[1]);
        }
    }
}
