package edu.bupt.ta.controller;

import edu.bupt.ta.enums.ApplicationStatus;
import edu.bupt.ta.enums.JobStatus;
import edu.bupt.ta.model.Application;
import edu.bupt.ta.model.Job;
import edu.bupt.ta.model.User;
import edu.bupt.ta.service.ServiceRegistry;
import edu.bupt.ta.util.ValidationResult;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;

public class JobManagementController {
    private static final Duration AUTO_REFRESH_INTERVAL = Duration.seconds(5);
    private static final DateTimeFormatter DETAIL_TIME_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy • HH:mm", Locale.ENGLISH);

    private final ServiceRegistry services;
    private final User user;
    private final Consumer<Job> onViewApplicants;

    private final VBox view = new VBox(18);
    private final TableView<JobTableRow> table = new TableView<>();
    private final Timeline autoRefreshTimeline = new Timeline();

    private HBox kpiRow;
    private JobStatus currentFilterStatus;
    private VBox listPanel;
    private VBox emptyState;

    private final Label detailTitle = new Label("Select a job");
    private final Label detailSubtitle = new Label("Choose a row to view job details and applicant activity.");
    private final HBox detailBadges = new HBox(8);
    private final Label moduleTitle = new Label("-");
    private final Label moduleMeta = new Label("-");
    private final Label moduleBody = new Label("-");
    private final Label appliedCount = new Label("0");
    private final Label reviewCount = new Label("0");
    private final Label hiredCount = new Label("0");
    private final HBox avatarRow = new HBox(6);
    private final VBox activityLog = new VBox(10);
    private final Button viewApplicantsButton = new Button("View All Applicants");
    private final Button editButton = new Button("Edit Job Details");
    private final Button closeButton = new Button("Close Job");

    public JobManagementController(ServiceRegistry services, User user, Consumer<Job> onViewApplicants) {
        this.services = services;
        this.user = user;
        this.onViewApplicants = onViewApplicants;
        initialize();
        refresh();
    }

    public Parent getView() {
        return view;
    }

    private void initialize() {
        view.getStyleClass().add("app-surface");
        view.setPadding(new Insets(24));

        kpiRow = buildKpiRow(List.of());
        listPanel = buildListPanel();
        VBox detailPanel = buildDetailPanel();

        HBox body = new HBox(18, listPanel, detailPanel);
        HBox.setHgrow(listPanel, Priority.ALWAYS);

        view.getChildren().addAll(buildHeader(), kpiRow, body);
        configureAutoRefresh();
    }

    private void configureAutoRefresh() {
        autoRefreshTimeline.getKeyFrames().setAll(new KeyFrame(AUTO_REFRESH_INTERVAL, event -> refresh()));
        autoRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        autoRefreshTimeline.play();

        view.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) {
                autoRefreshTimeline.stop();
            } else {
                autoRefreshTimeline.play();
            }
        });
    }

    private HBox buildHeader() {
        Label title = new Label("My Jobs");
        title.getStyleClass().add("page-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button filter = new Button("Filter");
        filter.getStyleClass().add("secondary-button");
        filter.setOnAction(event -> onFilter());

        Button create = new Button("+ Create New Job");
        create.getStyleClass().add("primary-button");
        create.setOnAction(event -> onCreate());

        HBox row = new HBox(12, title, spacer, filter, create);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private HBox buildKpiRow(List<Job> jobs) {
        int totalOpenings = jobs.stream().mapToInt(Job::getPositions).sum();
        int totalApplicants = jobs.stream()
                .mapToInt(job -> services.applicationService().getApplicationsByJob(job.getJobId()).size())
                .sum();
        long reviewedApplicants = jobs.stream()
                .flatMap(job -> services.applicationService().getApplicationsByJob(job.getJobId()).stream())
                .filter(app -> app.getStatus() == ApplicationStatus.UNDER_REVIEW || app.getStatus() == ApplicationStatus.ACCEPTED)
                .count();
        int reviewRate = totalApplicants == 0 ? 0 : (int) Math.round(reviewedApplicants * 100.0 / totalApplicants);

        HBox row = new HBox(16,
                kpiCard("Total Openings", String.valueOf(totalOpenings), jobs.isEmpty() ? "No active postings yet" : jobs.size() + " job posts"),
                kpiCard("Total Applicants", String.valueOf(totalApplicants), "Across all managed jobs"),
                kpiCard("Interview Rate", reviewRate + "%", totalApplicants == 0 ? "Waiting for applications" : "Pipeline coverage")
        );
        return row;
    }

    private VBox kpiCard(String titleText, String valueText, String metaText) {
        VBox card = new VBox(6);
        card.getStyleClass().add("panel-card");
        card.setPadding(new Insets(16));
        card.setMinWidth(180);
        HBox.setHgrow(card, Priority.ALWAYS);

        Label title = new Label(titleText);
        title.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #94a3b8;");

        HBox valueRow = new HBox(8);
        valueRow.setAlignment(Pos.BASELINE_LEFT);

        Label value = new Label(valueText);
        value.setStyle("-fx-font-size: 32px; -fx-font-weight: 900; -fx-text-fill: #0f172a;");

        Label meta = new Label(metaText);
        meta.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #10b981;");

        valueRow.getChildren().addAll(value, meta);
        card.getChildren().addAll(title, valueRow);
        return card;
    }

    private VBox buildListPanel() {
        VBox panel = new VBox(10);
        panel.getStyleClass().add("panel-card");
        panel.setPadding(new Insets(12));
        HBox.setHgrow(panel, Priority.ALWAYS);

        TableColumn<JobTableRow, JobTableRow> titleCol = new TableColumn<>("JOB TITLE & ID");
        titleCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue()));
        titleCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(JobTableRow item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                Label title = new Label(item.job().getTitle());
                title.setWrapText(true);
                title.setStyle("-fx-font-size: 15px; -fx-font-weight: 600; -fx-text-fill: #1f2937;");

                Label id = new Label("ID: " + fallback(item.job().getJobId(), "-"));
                id.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #94a3b8;");
                id.setWrapText(true);

                VBox box = new VBox(3, title, id);
                box.setFillWidth(true);
                title.maxWidthProperty().bind(column.widthProperty().subtract(28));
                id.maxWidthProperty().bind(column.widthProperty().subtract(28));
                setGraphic(box);
                setText(null);
            }
        });

        TableColumn<JobTableRow, String> applicantsCol = new TableColumn<>("APPLICANTS");
        applicantsCol.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().appliedApplicantCount() + "/" + cell.getValue().targetApplicantCount()));
        applicantsCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label badge = new Label(item);
                badge.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-font-size: 12px; -fx-font-weight: 600; -fx-background-radius: 999; -fx-padding: 5 11 5 11;");
                setGraphic(badge);
                setText(null);
            }
        });

        TableColumn<JobTableRow, String> statusCol = new TableColumn<>("STATUS");
        statusCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().job().getStatus().name()));
        statusCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label chip = buildStatusChip(JobStatus.valueOf(item));
                setGraphic(chip);
                setText(null);
            }
        });

        table.getColumns().setAll(titleCol, applicantsCol, statusCol);
        table.getStyleClass().add("job-table-spaced");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setFixedCellSize(92);
        table.setPrefHeight(640);
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldRow, newRow) -> {
            Job job = newRow == null ? null : newRow.job();
            updateDetail(job);
            updateActionButtons(job);
        });

        titleCol.setMinWidth(420);
        titleCol.setPrefWidth(560);
        applicantsCol.setMinWidth(90);
        applicantsCol.setPrefWidth(100);
        applicantsCol.setMaxWidth(120);
        statusCol.setMinWidth(100);
        statusCol.setPrefWidth(110);
        statusCol.setMaxWidth(130);

        emptyState = buildEmptyState();

        panel.getChildren().addAll(table, emptyState);
        return panel;
    }

    private VBox buildEmptyState() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(48, 20, 48, 20));
        box.setAlignment(Pos.CENTER);
        box.setVisible(false);
        box.setManaged(false);

        StackPane ghost = new StackPane();
        ghost.getStyleClass().add("ghost-empty-shell");
        ghost.setMinSize(120, 120);
        ghost.setPrefSize(120, 120);
        ghost.setMaxSize(120, 120);
        ghost.getChildren().add(styledLabel("JOBS", "-fx-font-size: 24px; -fx-font-weight: 900; -fx-text-fill: #d4dde8;"));

        Label title = new Label("No job posts yet");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: 900; -fx-text-fill: #0f172a;");

        Label subtitle = new Label("Create your first job post to start collecting applications.");
        subtitle.setWrapText(true);
        subtitle.setStyle("-fx-font-size: 14px; -fx-font-weight: 400; -fx-text-fill: #64748b;");

        Button create = new Button("+ Create New Job");
        create.getStyleClass().add("primary-button");
        create.setOnAction(event -> onCreate());

        box.getChildren().addAll(ghost, title, subtitle, create);
        return box;
    }

    private VBox buildDetailPanel() {
        VBox shell = new VBox(16);
        shell.getStyleClass().add("panel-card");
        shell.setPadding(new Insets(20));
        shell.setPrefWidth(420);
        shell.setMinWidth(420);

        Label kicker = new Label("JOB DETAILS");
        kicker.getStyleClass().add("tiny-kicker");

        detailTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: 900; -fx-text-fill: #0f172a;");
        detailTitle.setWrapText(true);

        detailSubtitle.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #94a3b8;");
        detailSubtitle.setWrapText(true);

        detailBadges.setAlignment(Pos.CENTER_LEFT);
        VBox hero = new VBox(12, kicker, detailTitle, detailBadges, detailSubtitle);

        VBox moduleCard = new VBox(8);
        moduleCard.getStyleClass().add("soft-info-card");
        moduleCard.setPadding(new Insets(16));

        Label moduleKicker = new Label("ASSOCIATED MODULE");
        moduleKicker.getStyleClass().add("tiny-kicker");

        moduleTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: 600; -fx-text-fill: #334155;");
        moduleTitle.setWrapText(true);

        moduleMeta.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #94a3b8;");
        moduleBody.setWrapText(true);
        moduleBody.setStyle("-fx-font-size: 12px; -fx-font-weight: 400; -fx-text-fill: #64748b;");

        moduleCard.getChildren().addAll(moduleKicker, moduleTitle, moduleMeta, moduleBody);

        VBox applicantCard = new VBox(12);
        applicantCard.getStyleClass().add("soft-info-card");
        applicantCard.setPadding(new Insets(16));

        Label applicantKicker = new Label("APPLICANT STATUS");
        applicantKicker.getStyleClass().add("tiny-kicker");

        applicantCard.getChildren().addAll(
                applicantKicker,
                statLine("Applied", appliedCount, "#2563eb"),
                statLine("Under Review", reviewCount, "#8b5cf6"),
                statLine("Hired", hiredCount, "#10b981"),
                avatarRow
        );

        Label activityKicker = new Label("ACTIVITY LOG");
        activityKicker.getStyleClass().add("tiny-kicker");

        activityLog.setPadding(new Insets(2, 0, 0, 0));
        VBox activityCard = new VBox(12, activityKicker, activityLog);
        activityCard.getStyleClass().add("soft-info-card");
        activityCard.setPadding(new Insets(16));

        viewApplicantsButton.getStyleClass().add("primary-button");
        viewApplicantsButton.setMaxWidth(Double.MAX_VALUE);
        viewApplicantsButton.setOnAction(event -> onViewApplicants());

        editButton.getStyleClass().add("secondary-button");
        editButton.setMaxWidth(Double.MAX_VALUE);
        editButton.setOnAction(event -> onEdit());

        closeButton.getStyleClass().add("secondary-button");
        closeButton.setMaxWidth(Double.MAX_VALUE);
        closeButton.setOnAction(event -> onClose());

        VBox detailContent = new VBox(22, hero, moduleCard, applicantCard, activityCard);
        ScrollPane scrollPane = new ScrollPane(detailContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.getStyleClass().add("detail-scroll-plain");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        shell.getChildren().addAll(scrollPane, viewApplicantsButton, editButton, closeButton);
        return shell;
    }

    private HBox statLine(String labelText, Label valueLabel, String accent) {
        Label label = new Label(labelText);
        label.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #475569;");

        valueLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 900; -fx-text-fill: " + accent + ";");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return new HBox(10, label, spacer, valueLabel);
    }

    private void refresh() {
        Job selected = table.getSelectionModel().getSelectedItem() == null ? null : table.getSelectionModel().getSelectedItem().job();
        String selectedJobId = selected == null ? null : selected.getJobId();

        List<Job> jobs = services.jobService().getJobsByOrganiser(user.getUserId());
        if (currentFilterStatus != null) {
            jobs = jobs.stream().filter(job -> job.getStatus() == currentFilterStatus).toList();
        }
        jobs = jobs.stream()
                .sorted(Comparator.comparing(Job::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        view.getChildren().set(1, buildKpiRow(jobs));

        List<JobTableRow> rows = jobs.stream()
                .map(job -> {
                    int appliedApplicantCount = (int) services.applicationService().getApplicationsByJob(job.getJobId()).stream()
                            .filter(app -> app.getStatus() != ApplicationStatus.CANCELLED)
                            .count();
                    int targetApplicantCount = Math.max(job.getPositions(), 0);
                    return new JobTableRow(job, appliedApplicantCount, targetApplicantCount);
                })
                .toList();
        table.setItems(FXCollections.observableArrayList(rows));

        if (rows.isEmpty()) {
            table.setVisible(false);
            table.setManaged(false);
            emptyState.setVisible(true);
            emptyState.setManaged(true);
            updateDetail(null);
            updateActionButtons(null);
            return;
        }

        table.setVisible(true);
        table.setManaged(true);
        emptyState.setVisible(false);
        emptyState.setManaged(false);

        if (selectedJobId != null) {
            rows.stream()
                    .filter(row -> selectedJobId.equals(row.job().getJobId()))
                    .findFirst()
                    .ifPresentOrElse(row -> table.getSelectionModel().select(row), () -> table.getSelectionModel().selectFirst());
        } else {
            table.getSelectionModel().selectFirst();
        }
    }

    private void updateActionButtons(Job selectedJob) {
        boolean hasSelection = selectedJob != null;
        viewApplicantsButton.setDisable(!hasSelection);
        editButton.setDisable(!hasSelection);
        closeButton.setDisable(!hasSelection || selectedJob.getStatus() != JobStatus.OPEN);
        closeButton.setManaged(hasSelection && selectedJob.getStatus() == JobStatus.OPEN);
        closeButton.setVisible(hasSelection && selectedJob.getStatus() == JobStatus.OPEN);
    }

    private void updateDetail(Job job) {
        detailBadges.getChildren().clear();
        avatarRow.getChildren().clear();
        activityLog.getChildren().clear();

        if (job == null) {
            detailTitle.setText("Select a job");
            detailSubtitle.setText("Choose a row to view job details and applicant activity.");
            moduleTitle.setText("-");
            moduleMeta.setText("-");
            moduleBody.setText("-");
            appliedCount.setText("0");
            reviewCount.setText("0");
            hiredCount.setText("0");
            activityLog.getChildren().add(activityLine("No activity yet", "Select one job to inspect its timeline."));
            return;
        }

        List<Application> applications = services.applicationService().getApplicationsByJob(job.getJobId());
        long applied = applications.stream()
                .filter(app -> app.getStatus() == ApplicationStatus.SUBMITTED || app.getStatus() == ApplicationStatus.UNDER_REVIEW)
                .count();
        long underReview = applications.stream().filter(app -> app.getStatus() == ApplicationStatus.UNDER_REVIEW).count();
        long hired = applications.stream().filter(app -> app.getStatus() == ApplicationStatus.ACCEPTED).count();

        detailTitle.setText(fallback(job.getTitle(), "Untitled Job"));
        detailSubtitle.setText("Recruitment Period: " + formatTimeline(job));
        detailBadges.getChildren().addAll(
                buildMiniChip(fallback(job.getModuleCode(), "MODULE"), "#eff6ff", "#2563eb"),
                buildStatusChip(job.getStatus())
        );
        moduleTitle.setText(fallback(job.getModuleName(), "Associated module not set"));
        moduleMeta.setText(fallback(job.getModuleCode(), "-") + " • " + fallback(job.getSemester(), "-"));
        moduleBody.setText(descriptionSnippet(job.getDescription()));

        appliedCount.setText(String.valueOf(applied));
        reviewCount.setText(String.valueOf(underReview));
        hiredCount.setText(String.valueOf(hired));

        applications.stream().limit(4).forEach(app -> avatarRow.getChildren().add(applicantAvatar(app)));
        if (avatarRow.getChildren().isEmpty()) {
            avatarRow.getChildren().add(styledLabel("No applicants yet", "-fx-font-size: 12px; -fx-font-weight: 400; -fx-text-fill: #94a3b8;"));
        }

        activityLog.getChildren().addAll(
                activityLine("Job created", formatTimestamp(job.getCreatedAt())),
                activityLine("Deadline", formatTimestamp(job.getDeadline()))
        );
    }

    private Label buildStatusChip(JobStatus status) {
        if (status == null) {
            return buildMiniChip("UNKNOWN", "#f1f5f9", "#64748b");
        }
        return switch (status) {
            case OPEN -> buildMiniChip("ACTIVE", "#ecfdf3", "#10b981");
            case DRAFT -> buildMiniChip("DRAFT", "#eff6ff", "#2563eb");
            case EXPIRED -> buildMiniChip("EXPIRED", "#fff7ed", "#f59e0b");
            case CLOSED -> buildMiniChip("CLOSED", "#f1f5f9", "#64748b");
        };
    }

    private Label buildMiniChip(String text, String background, String color) {
        Label chip = new Label(text);
        chip.setStyle("-fx-background-color: " + background + "; -fx-text-fill: " + color + "; -fx-font-size: 10px; -fx-font-weight: 900; -fx-background-radius: 999; -fx-padding: 4 9 4 9;");
        return chip;
    }

    private VBox activityLine(String title, String body) {
        VBox line = new VBox(2);
        Label titleLabel = new Label("• " + title);
        titleLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #334155;");

        Label bodyLabel = new Label(body);
        bodyLabel.setWrapText(true);
        bodyLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 400; -fx-text-fill: #94a3b8;");

        line.getChildren().addAll(titleLabel, bodyLabel);
        return line;
    }

    private StackPane applicantAvatar(Application application) {
        String applicantName = services.applicantProfileRepository().findById(application.getApplicantId())
                .map(profile -> fallback(profile.getFullName(), application.getApplicantId()))
                .orElse(application.getApplicantId());

        Label initials = new Label(initials(applicantName));
        initials.setStyle("-fx-font-size: 11px; -fx-font-weight: 900; -fx-text-fill: #475569;");

        StackPane avatar = new StackPane(initials);
        avatar.setMinSize(28, 28);
        avatar.setPrefSize(28, 28);
        avatar.setMaxSize(28, 28);
        avatar.setStyle("-fx-background-color: #eef2f7; -fx-background-radius: 999;");
        return avatar;
    }

    private void onCreate() {
        JobEditorController editor = new JobEditorController();
        editor.show(null, user.getUserId()).ifPresent(job -> {
            ValidationResult result = services.jobService().createJob(job);
            if (!result.isValid()) {
                showError(String.join("\n", result.getErrors()));
                return;
            }
            refresh();
        });
    }

    private void onEdit() {
        Job selected = selectedJob();
        if (selected == null) {
            showError("Please select one job first.");
            return;
        }

        JobEditorController editor = new JobEditorController();
        editor.show(selected, user.getUserId()).ifPresent(job -> {
            ValidationResult result = services.jobService().updateJob(job);
            if (!result.isValid()) {
                showError(String.join("\n", result.getErrors()));
                return;
            }
            refresh();
        });
    }

    private void onClose() {
        Job selected = selectedJob();
        if (selected == null) {
            showError("Please select one job first.");
            return;
        }
        boolean confirmed = DialogControllerFactory.confirmAction(
                "Close Job",
                "Close \"" + selected.getTitle() + "\" now? Closed jobs cannot receive new applications.",
                view.getScene() == null ? null : view.getScene().getWindow());
        if (!confirmed) {
            return;
        }
        ValidationResult result = services.jobService().closeJobWithValidation(selected.getJobId(), user.getUserId());
        if (!result.isValid()) {
            showError(String.join("\n", result.getErrors()));
            refresh();
            return;
        }
        DialogControllerFactory.success("Job Closed", "The job was set to CLOSED successfully.",
                view.getScene() == null ? null : view.getScene().getWindow());
        refresh();
    }

    private void onFilter() {
        List<String> options = List.of("All Statuses", "OPEN", "DRAFT", "CLOSED", "EXPIRED");
        String currentSelection = currentFilterStatus == null ? "All Statuses" : currentFilterStatus.name();
        ChoiceDialog<String> dialog = new ChoiceDialog<>(currentSelection, options);
        dialog.setTitle("Filter Jobs");
        dialog.setHeaderText("Filter jobs by status");
        dialog.setContentText("Status:");
        if (view.getScene() != null && view.getScene().getWindow() != null) {
            dialog.initOwner(view.getScene().getWindow());
        }

        Optional<String> selected = dialog.showAndWait();
        if (selected.isEmpty()) {
            return;
        }
        currentFilterStatus = "All Statuses".equals(selected.get()) ? null : JobStatus.valueOf(selected.get());
        refresh();
    }

    private void onViewApplicants() {
        Job selected = selectedJob();
        if (selected == null) {
            showError("Please select one job first.");
            return;
        }
        if (onViewApplicants != null) {
            onViewApplicants.accept(selected);
        }
    }

    private Job selectedJob() {
        JobTableRow row = table.getSelectionModel().getSelectedItem();
        return row == null ? null : row.job();
    }

    private String formatTimestamp(LocalDateTime time) {
        return time == null ? "-" : time.format(DETAIL_TIME_FORMAT);
    }

    private String formatTimeline(Job job) {
        String start = job.getCreatedAt() == null ? "-" : job.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH));
        String end = job.getDeadline() == null ? "-" : job.getDeadline().format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH));
        return start + " - " + end + " • ID: " + fallback(job.getJobId(), "-");
    }

    private String descriptionSnippet(String description) {
        String text = fallback(description, "Add a short description to explain the role scope and expectations.");
        return text.length() <= 110 ? text : text.substring(0, 107) + "...";
    }

    private String fallback(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private String initials(String name) {
        if (name == null || name.isBlank()) {
            return "MO";
        }
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        }
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }

    private Label styledLabel(String text, String style) {
        Label label = new Label(text);
        label.setStyle(style);
        return label;
    }

    private void showError(String message) {
        DialogControllerFactory.validationError(message, view.getScene() == null ? null : view.getScene().getWindow());
    }

    private record JobTableRow(Job job, int appliedApplicantCount, int targetApplicantCount) {
    }
}
