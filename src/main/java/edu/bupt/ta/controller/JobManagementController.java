package edu.bupt.ta.controller;

import edu.bupt.ta.enums.ApplicationStatus;
import edu.bupt.ta.enums.JobStatus;
import edu.bupt.ta.enums.JobType;
import edu.bupt.ta.model.Application;
import edu.bupt.ta.model.Job;
import edu.bupt.ta.model.User;
import edu.bupt.ta.service.ServiceRegistry;
import edu.bupt.ta.util.ValidationResult;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableNumberValue;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
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
    private static final Duration JOB_STATUS_POLL_INTERVAL = Duration.seconds(5);

    private static final DateTimeFormatter LIST_DATE_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter DETAIL_TIME_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy • HH:mm", Locale.ENGLISH);

    private final ServiceRegistry services;
    private final User user;
    private final Consumer<Job> onViewApplicants;

    private final VBox view = new VBox(18);
    private final TableView<JobTableRow> table = new TableView<>();

    private HBox kpiRow;
    private JobStatus currentFilterStatus;
    private VBox listPanel;
    private VBox emptyState;
    private VBox jobListContent;
    private String selectedJobId;

    private final Label detailTitle = new Label("Select a job");
    private final Label detailSubtitle = new Label("Choose a row to view job details and applicant activity.");
    private final HBox detailBadges = new HBox(8);
    private final Label ovModuleName = new Label("-");
    private final Label ovModuleCode = new Label("-");
    private final Label ovSemester = new Label("-");
    private final Label ovJobType = new Label("-");
    private final Label ovCampuses = new Label("-");
    private final Label ovSlots = new Label("-");
    private final Label ovJobId = new Label("-");
    private final Label ovStatus = new Label("-");
    private final Label ovCreated = new Label("-");
    private final Label ovApplyBy = new Label("-");
    private final Label ovApplications = new Label("-");
    private final Label descriptionBody = new Label("-");
    private final Label appliedCount = new Label("0");
    private final Label reviewCount = new Label("0");
    private final Label hiredCount = new Label("0");
    private final HBox avatarRow = new HBox(6);
    private final Button viewApplicantsButton = new Button("View All Applicants");
    private final Button editButton = new Button("Edit Job Details");
    private final Button closeButton = new Button("Close Job");

    private Timeline jobStatusRefreshTimeline;

    /** Snapshot of list content; unchanged polls skip rebuilding the table to avoid flicker. */
    private String lastPollFingerprint = "";

    public JobManagementController(ServiceRegistry services, User user, Consumer<Job> onViewApplicants) {
        this.services = services;
        this.user = user;
        this.onViewApplicants = onViewApplicants;
        initialize();
        refresh();
        installPeriodicJobStatusRefresh();
    }

    public Parent getView() {
        return view;
    }

    private void initialize() {
        view.getStyleClass().add("app-surface");
        view.setPadding(new Insets(24));
        view.setFillWidth(true);

        kpiRow = buildKpiRow(List.of());
        listPanel = buildListPanelV3();
        VBox detailPanel = buildDetailPanel();

        HBox body = new HBox(18, listPanel, detailPanel);
        body.setFillHeight(true);
        body.setMinWidth(0);
        HBox.setHgrow(listPanel, Priority.ALWAYS);
        HBox.setHgrow(detailPanel, Priority.ALWAYS);

        listPanel.prefWidthProperty().bind(body.widthProperty().subtract(18).multiply(0.6));
        detailPanel.prefWidthProperty().bind(body.widthProperty().subtract(18).multiply(0.4));

        view.getChildren().addAll(buildHeader(), kpiRow, body);
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
        long totalJobs = jobs.size();
        int totalApplicants = jobs.stream()
                .mapToInt(job -> services.applicationService().getApplicationsByJob(job.getJobId()).size())
                .sum();
        long draftJobs = jobs.stream().filter(job -> job.getStatus() == JobStatus.DRAFT).count();
        long reviewedApplicants = jobs.stream()
                .flatMap(job -> services.applicationService().getApplicationsByJob(job.getJobId()).stream())
                .filter(app -> app.getStatus() == ApplicationStatus.UNDER_REVIEW || app.getStatus() == ApplicationStatus.ACCEPTED)
                .count();
        int reviewRate = totalApplicants == 0 ? 0 : (int) Math.round(reviewedApplicants * 100.0 / totalApplicants);
        long openJobs = jobs.stream().filter(job -> job.getStatus() == JobStatus.OPEN).count();

        HBox row = new HBox(16,
                kpiCard("Total Jobs", String.valueOf(totalJobs),
                        jobs.isEmpty() ? "No postings yet" : openJobs + " currently open"),
                kpiCard("Total Applicants", String.valueOf(totalApplicants), "Across all managed jobs"),
                kpiCard("Draft Jobs", String.valueOf(draftJobs),
                        draftJobs == 0 ? "No unpublished jobs" : "Need publishing before applicants can apply"),
                kpiCard("Under Review", reviewRate + "%",
                        totalApplicants == 0 ? "No review activity yet" : "Active screening progress")
        );
        row.setFillHeight(true);
        row.setMinWidth(0);
        row.getChildren().forEach(child -> {
            if (child instanceof VBox card) {
                card.prefWidthProperty().bind(row.widthProperty().subtract(48).divide(4));
                card.maxWidthProperty().bind(card.prefWidthProperty());
            }
        });
        return row;
    }

    private VBox kpiCard(String titleText, String valueText, String metaText) {
        VBox card = new VBox(8);
        card.getStyleClass().add("panel-card");
        card.setPadding(new Insets(16));
        card.setMinWidth(0);
        card.setPrefHeight(132);
        HBox.setHgrow(card, Priority.ALWAYS);

        Label title = new Label(titleText);
        title.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #94a3b8;");

        Label value = new Label(valueText);
        value.setWrapText(true);
        value.setStyle("-fx-font-size: 32px; -fx-font-weight: 900; -fx-text-fill: #0f172a;");

        Label meta = new Label(metaText);
        meta.setWrapText(true);
        meta.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #10b981;");

        card.getChildren().addAll(title, value, meta);
        return card;
    }

    private VBox buildListPanel() {
        VBox panel = new VBox(10);
        panel.getStyleClass().add("panel-card");
        panel.setPadding(new Insets(12));
        panel.setMinWidth(0);
        panel.setPrefWidth(0);
        panel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(panel, Priority.ALWAYS);

        TableColumn<JobTableRow, JobTableRow> titleCol = new TableColumn<>("JOB TITLE");
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
                title.setTextOverrun(OverrunStyle.CLIP);
                title.setMinHeight(Region.USE_PREF_SIZE);
                title.setMaxHeight(Region.USE_PREF_SIZE);
                title.setStyle("-fx-font-size: 15px; -fx-font-weight: 600; -fx-text-fill: #1f2937;");

                Label id = new Label("ID: " + fallback(item.job().getJobId(), "-")
                        + " • Created " + formatListDate(item.job().getCreatedAt()));
                id.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #94a3b8;");
                id.setWrapText(true);
                id.setTextOverrun(OverrunStyle.CLIP);

                VBox box = new VBox(3, title, id);
                box.setFillWidth(true);
                title.prefWidthProperty().bind(column.widthProperty().subtract(28));
                id.prefWidthProperty().bind(column.widthProperty().subtract(28));
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
                StackPane wrapper = new StackPane(badge);
                wrapper.setAlignment(Pos.CENTER);
                wrapper.setMaxWidth(Double.MAX_VALUE);
                setAlignment(Pos.CENTER);
                setGraphic(wrapper);
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
                StackPane wrapper = new StackPane(chip);
                wrapper.setAlignment(Pos.CENTER);
                wrapper.setMaxWidth(Double.MAX_VALUE);
                setAlignment(Pos.CENTER);
                setGraphic(wrapper);
                setText(null);
            }
        });

        table.getColumns().setAll(titleCol, applicantsCol, statusCol);
        table.getStyleClass().add("job-table-spaced");
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        table.setFixedCellSize(104);
        table.setPrefHeight(640);
        table.setMinWidth(0);
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldRow, newRow) -> {
            Job job = newRow == null ? null : newRow.job();
            updateDetail(job);
            updateActionButtons(job);
        });

        titleCol.setMinWidth(260);
        applicantsCol.setMinWidth(110);
        statusCol.setMinWidth(120);

        titleCol.prefWidthProperty().bind(Bindings.max(260, panel.widthProperty().subtract(38).multiply(0.62)));
        applicantsCol.prefWidthProperty().bind(Bindings.max(110, panel.widthProperty().subtract(38).multiply(0.18)));
        statusCol.prefWidthProperty().bind(Bindings.max(120, panel.widthProperty().subtract(38).multiply(0.20)));

        emptyState = buildEmptyState();

        panel.getChildren().addAll(table, emptyState);
        return panel;
    }

    private VBox buildListPanelV2() {
        VBox panel = new VBox(10);
        panel.getStyleClass().add("panel-card");
        panel.setPadding(new Insets(12));
        panel.setMinWidth(0);
        panel.setPrefWidth(0);
        panel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(panel, Priority.ALWAYS);

        TableColumn<JobTableRow, JobTableRow> titleCol = new TableColumn<>("JOB TITLE");
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
                title.setTextOverrun(OverrunStyle.CLIP);
                title.setMaxWidth(Double.MAX_VALUE);
                title.setStyle("-fx-font-size: 15px; -fx-font-weight: 600; -fx-text-fill: #1f2937;");

                Label id = new Label("ID: " + fallback(item.job().getJobId(), "-"));
                id.setWrapText(true);
                id.setTextOverrun(OverrunStyle.CLIP);
                id.setMaxWidth(Double.MAX_VALUE);
                id.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #94a3b8;");

                VBox box = new VBox(3, title, id);
                box.setFillWidth(true);
                box.setMaxWidth(Double.MAX_VALUE);
                box.prefWidthProperty().bind(column.widthProperty().subtract(24));
                title.prefWidthProperty().bind(box.widthProperty());
                id.prefWidthProperty().bind(box.widthProperty());
                setGraphic(box);
                setText(null);
            }
        });

        TableColumn<JobTableRow, String> createdCol = new TableColumn<>("CREATED");
        createdCol.setCellValueFactory(cell -> new SimpleStringProperty(formatListDate(cell.getValue().job().getCreatedAt())));
        createdCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                Label label = new Label(item);
                label.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #64748b;");
                StackPane wrapper = new StackPane(label);
                wrapper.setAlignment(Pos.CENTER);
                wrapper.setMaxWidth(Double.MAX_VALUE);
                setAlignment(Pos.CENTER);
                setGraphic(wrapper);
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
                StackPane wrapper = new StackPane(badge);
                wrapper.setAlignment(Pos.CENTER);
                wrapper.setMaxWidth(Double.MAX_VALUE);
                setAlignment(Pos.CENTER);
                setGraphic(wrapper);
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
                StackPane wrapper = new StackPane(chip);
                wrapper.setAlignment(Pos.CENTER);
                wrapper.setMaxWidth(Double.MAX_VALUE);
                setAlignment(Pos.CENTER);
                setGraphic(wrapper);
                setText(null);
            }
        });

        table.getColumns().setAll(titleCol, createdCol, applicantsCol, statusCol);
        table.getStyleClass().add("job-table-spaced");
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        table.setFixedCellSize(120);
        table.setPrefHeight(640);
        table.setMinWidth(0);
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldRow, newRow) -> {
            Job job = newRow == null ? null : newRow.job();
            updateDetail(job);
            updateActionButtons(job);
        });

        titleCol.setMinWidth(170);
        createdCol.setMinWidth(120);
        applicantsCol.setMinWidth(110);
        statusCol.setMinWidth(110);

        titleCol.prefWidthProperty().bind(Bindings.max(170, panel.widthProperty().subtract(52).multiply(0.38)));
        createdCol.prefWidthProperty().bind(Bindings.max(120, panel.widthProperty().subtract(52).multiply(0.22)));
        applicantsCol.prefWidthProperty().bind(Bindings.max(110, panel.widthProperty().subtract(52).multiply(0.18)));
        statusCol.prefWidthProperty().bind(Bindings.max(110, panel.widthProperty().subtract(52).multiply(0.18)));

        emptyState = buildEmptyState();

        panel.getChildren().addAll(table, emptyState);
        return panel;
    }

    private VBox buildListPanelV3() {
        VBox panel = new VBox(10);
        panel.getStyleClass().add("panel-card");
        panel.setPadding(new Insets(12));
        panel.setMinWidth(0);
        panel.setPrefWidth(0);
        panel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(panel, Priority.ALWAYS);

        HBox header = buildListHeader(panel);

        jobListContent = new VBox(0);
        jobListContent.setFillWidth(true);

        ScrollPane scrollPane = new ScrollPane(jobListContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.getStyleClass().add("detail-scroll-plain");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        emptyState = buildEmptyState();
        emptyState.setVisible(false);
        emptyState.setManaged(false);

        panel.getChildren().addAll(header, scrollPane, emptyState);
        return panel;
    }

    private HBox buildListHeader(VBox host) {
        HBox header = new HBox(0,
                headerCell("JOB TITLE", host.widthProperty().subtract(24).multiply(0.38), Pos.CENTER_LEFT),
                headerCell("CREATED", host.widthProperty().subtract(24).multiply(0.22), Pos.CENTER),
                headerCell("APPLICANTS", host.widthProperty().subtract(24).multiply(0.18), Pos.CENTER),
                headerCell("STATUS", host.widthProperty().subtract(24).multiply(0.18), Pos.CENTER)
        );
        header.setStyle("-fx-background-color: #fbfcfe; -fx-background-radius: 12 12 0 0; "
                + "-fx-border-color: transparent transparent #edf2f7 transparent;");
        return header;
    }

    private StackPane headerCell(String text, javafx.beans.value.ObservableNumberValue widthBinding, Pos alignment) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 11px; -fx-font-weight: 600; -fx-text-fill: #94a3b8;");

        StackPane cell = new StackPane(label);
        cell.setAlignment(alignment);
        cell.setPadding(new Insets(12, 12, 12, 12));
        cell.prefWidthProperty().bind(widthBinding);
        return cell;
    }

    private HBox buildJobListRow(JobTableRow row, VBox host) {
        Job job = row.job();

        Label title = new Label(job.getTitle());
        title.setWrapText(true);
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: 600; -fx-text-fill: #1f2937;");

        Label id = new Label("ID: " + fallback(job.getJobId(), "-"));
        id.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #94a3b8;");

        VBox titleBox = new VBox(4, title, id);
        titleBox.setAlignment(Pos.CENTER_LEFT);
        titleBox.setPadding(new Insets(12, 12, 12, 12));
        titleBox.prefWidthProperty().bind(host.widthProperty().subtract(24).multiply(0.38));
        title.maxWidthProperty().bind(titleBox.widthProperty().subtract(24));

        Label created = new Label(formatListDate(job.getCreatedAt()));
        created.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #64748b;");
        StackPane createdBox = centeredListCell(created, host.widthProperty().subtract(24).multiply(0.22));

        Label applicants = new Label(row.appliedApplicantCount() + "/" + row.targetApplicantCount());
        applicants.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-font-size: 12px; "
                + "-fx-font-weight: 600; -fx-background-radius: 999; -fx-padding: 5 11 5 11;");
        StackPane applicantsBox = centeredListCell(applicants, host.widthProperty().subtract(24).multiply(0.18));

        Label statusChip = buildStatusChip(job.getStatus());
        StackPane statusBox = centeredListCell(statusChip, host.widthProperty().subtract(24).multiply(0.18));

        HBox rowBox = new HBox(0, titleBox, createdBox, applicantsBox, statusBox);
        rowBox.setAlignment(Pos.CENTER_LEFT);
        rowBox.setFillHeight(true);
        rowBox.setMinHeight(84);
        rowBox.setUserData(job.getJobId());
        rowBox.setStyle(rowStyle(job.getJobId().equals(selectedJobId)));
        rowBox.setOnMouseClicked(event -> {
            selectedJobId = job.getJobId();
            updateDetail(job);
            updateActionButtons(job);
            refreshListStyles();
        });
        return rowBox;
    }

    private StackPane centeredListCell(Node node, javafx.beans.value.ObservableNumberValue widthBinding) {
        StackPane cell = new StackPane(node);
        cell.setAlignment(Pos.CENTER);
        cell.setPadding(new Insets(12, 12, 12, 12));
        cell.prefWidthProperty().bind(widthBinding);
        return cell;
    }

    private void refreshListStyles() {
        if (jobListContent == null) {
            return;
        }
        for (Node child : jobListContent.getChildren()) {
            if (child instanceof HBox rowBox && rowBox.getUserData() instanceof String jobId) {
                rowBox.setStyle(rowStyle(jobId.equals(selectedJobId)));
            }
        }
    }

    private String rowStyle(boolean selected) {
        String background = selected ? "#f8fbfd" : "white";
        return "-fx-background-color: " + background + ";"
                + "-fx-border-color: transparent transparent #f1f5f9 transparent;"
                + "-fx-border-width: 0 0 1 0;";
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
        shell.setMinWidth(0);
        shell.setMaxWidth(Double.MAX_VALUE);

        Label kicker = new Label("JOB DETAILS");
        kicker.getStyleClass().add("tiny-kicker");

        detailTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: 900; -fx-text-fill: #0f172a;");
        detailTitle.setWrapText(true);

        detailSubtitle.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #94a3b8;");
        detailSubtitle.setWrapText(true);

        detailBadges.setAlignment(Pos.CENTER_LEFT);
        VBox hero = new VBox(12, kicker, detailTitle, detailBadges, detailSubtitle);

        VBox overviewCard = new VBox(12);
        overviewCard.getStyleClass().add("soft-info-card");
        overviewCard.setPadding(new Insets(16));

        Label overviewKicker = new Label("JOB INFORMATION");
        overviewKicker.getStyleClass().add("tiny-kicker");

        GridPane overviewGrid = new GridPane();
        overviewGrid.setHgap(20);
        overviewGrid.setVgap(14);
        ColumnConstraints oc1 = new ColumnConstraints();
        oc1.setPercentWidth(50);
        oc1.setHgrow(Priority.ALWAYS);
        ColumnConstraints oc2 = new ColumnConstraints();
        oc2.setPercentWidth(50);
        oc2.setHgrow(Priority.ALWAYS);
        overviewGrid.getColumnConstraints().setAll(oc1, oc2);

        overviewGrid.add(detailSpecCell("Module name", ovModuleName), 0, 0, 2, 1);
        overviewGrid.add(detailSpecCell("Module code", ovModuleCode), 0, 1);
        overviewGrid.add(detailSpecCell("Semester", ovSemester), 1, 1);
        overviewGrid.add(detailSpecCell("Job type", ovJobType), 0, 2);
        overviewGrid.add(detailSpecCell("Open slots", ovSlots), 1, 2);
        overviewGrid.add(detailSpecCell("Campus", ovCampuses), 0, 3, 2, 1);
        overviewGrid.add(detailSpecCell("Job ID", ovJobId), 0, 4);
        overviewGrid.add(detailSpecCell("Publication status", ovStatus), 1, 4);
        overviewGrid.add(detailSpecCell("Created", ovCreated), 0, 5);
        overviewGrid.add(detailSpecCell("Deadline", ovApplyBy), 1, 5);
        overviewGrid.add(detailSpecCell("Applications", ovApplications), 0, 6, 2, 1);

        overviewCard.getChildren().addAll(overviewKicker, overviewGrid);

        VBox descriptionCard = new VBox(10);
        descriptionCard.getStyleClass().add("soft-info-card");
        descriptionCard.setPadding(new Insets(16));

        Label descriptionKicker = new Label("DESCRIPTION");
        descriptionKicker.getStyleClass().add("tiny-kicker");

        descriptionBody.setWrapText(true);
        descriptionBody.setMaxWidth(Double.MAX_VALUE);
        descriptionBody.setStyle("-fx-font-size: 13px; -fx-font-weight: 400; -fx-text-fill: #475569; -fx-line-spacing: 2px;");

        descriptionCard.getChildren().addAll(descriptionKicker, descriptionBody);

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

        viewApplicantsButton.getStyleClass().add("primary-button");
        viewApplicantsButton.setMaxWidth(Double.MAX_VALUE);
        viewApplicantsButton.setOnAction(event -> onViewApplicants());

        editButton.getStyleClass().add("secondary-button");
        editButton.setMaxWidth(Double.MAX_VALUE);
        editButton.setOnAction(event -> onEdit());

        closeButton.getStyleClass().add("secondary-button");
        closeButton.setMaxWidth(Double.MAX_VALUE);
        closeButton.setOnAction(event -> onClose());

        VBox detailContent = new VBox(22, hero, overviewCard, descriptionCard, applicantCard);
        ScrollPane scrollPane = new ScrollPane(detailContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.getStyleClass().add("detail-scroll-plain");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        shell.getChildren().addAll(scrollPane, viewApplicantsButton, editButton, closeButton);
        return shell;
    }

    private VBox detailSpecCell(String keyText, Label valueLabel) {
        Label key = new Label(keyText.toUpperCase(Locale.ENGLISH));
        key.getStyleClass().add("tiny-kicker");
        key.setWrapText(true);
        key.setMinWidth(0);
        key.setMaxWidth(Double.MAX_VALUE);

        valueLabel.setMinWidth(0);
        valueLabel.setMaxWidth(Double.MAX_VALUE);
        valueLabel.setWrapText(true);
        valueLabel.setTextOverrun(OverrunStyle.CLIP);
        valueLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #0f172a;");

        VBox box = new VBox(5, key, valueLabel);
        box.setFillWidth(true);
        box.setMinWidth(0);
        box.setMaxWidth(Double.MAX_VALUE);
        valueLabel.prefWidthProperty().bind(box.widthProperty());
        GridPane.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private HBox statLine(String labelText, Label valueLabel, String accent) {
        Label label = new Label(labelText);
        label.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #475569;");

        valueLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 900; -fx-text-fill: " + accent + ";");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return new HBox(10, label, spacer, valueLabel);
    }

    private void installPeriodicJobStatusRefresh() {
        jobStatusRefreshTimeline = new Timeline(new KeyFrame(JOB_STATUS_POLL_INTERVAL, event -> {
            if (view.getScene() != null) {
                refreshFromPoll();
            }
        }));
        jobStatusRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        view.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) {
                jobStatusRefreshTimeline.stop();
            } else {
                jobStatusRefreshTimeline.play();
            }
        });
        if (view.getScene() != null) {
            jobStatusRefreshTimeline.play();
        }
    }

    private void refreshFromPoll() {
        refresh(true);
    }

    private void refresh() {
        refresh(false);
    }

    /**
     * @param pollOnly when true, skips rebuilding UI if job list data matches {@link #lastPollFingerprint}
     *                 (still loads from service so deadlines can be persisted server-side).
     */
    private void refresh(boolean pollOnly) {
        String activeSelectedJobId = preferredJobId != null ? preferredJobId : selectedJobId;

        List<Job> jobs = services.jobService().getJobsByOrganiser(user.getUserId());
        if (currentFilterStatus != null) {
            jobs = jobs.stream().filter(job -> job.getStatus() == currentFilterStatus).toList();
        }
        jobs = jobs.stream()
                .sorted(Comparator.comparing(Job::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        List<JobTableRow> rows = jobs.stream()
                .map(job -> {
                    int appliedApplicantCount = (int) services.applicationService().getApplicationsByJob(job.getJobId()).stream()
                            .filter(app -> app.getStatus() != ApplicationStatus.CANCELLED)
                            .count();
                    int targetApplicantCount = Math.max(job.getPositions(), 0);
                    return new JobTableRow(job, appliedApplicantCount, targetApplicantCount);
                })
                .toList();

        String fingerprint = buildJobListFingerprint(rows);
        if (pollOnly && fingerprint.equals(lastPollFingerprint)) {
            return;
        }
        lastPollFingerprint = fingerprint;

        view.getChildren().set(1, buildKpiRow(jobs));

        if (jobListContent != null) {
            jobListContent.getChildren().setAll(rows.stream()
                    .map(row -> buildJobListRow(row, jobListContent))
                    .toList());
        }

        if (rows.isEmpty()) {
            if (jobListContent != null) {
                jobListContent.setVisible(false);
                jobListContent.setManaged(false);
            }
            emptyState.setVisible(true);
            emptyState.setManaged(true);
            selectedJobId = null;
            updateDetail(null);
            updateActionButtons(null);
            return;
        }

        if (jobListContent != null) {
            jobListContent.setVisible(true);
            jobListContent.setManaged(true);
        }
        emptyState.setVisible(false);
        emptyState.setManaged(false);

        if (activeSelectedJobId != null) {
            rows.stream()
                    .filter(row -> activeSelectedJobId.equals(row.job().getJobId()))
                    .findFirst()
                    .ifPresentOrElse(row -> {
                        selectedJobId = row.job().getJobId();
                        updateDetail(row.job());
                        updateActionButtons(row.job());
                    }, () -> {
                        selectedJobId = rows.get(0).job().getJobId();
                        updateDetail(rows.get(0).job());
                        updateActionButtons(rows.get(0).job());
                    });
        } else {
            selectedJobId = rows.get(0).job().getJobId();
            updateDetail(rows.get(0).job());
            updateActionButtons(rows.get(0).job());
        }
        refreshListStyles();
    }

    private String buildJobListFingerprint(List<JobTableRow> rows) {
        String filter = currentFilterStatus == null ? "ALL" : currentFilterStatus.name();
        if (rows.isEmpty()) {
            return filter + "|EMPTY";
        }
        StringBuilder sb = new StringBuilder(filter).append('|');
        for (JobTableRow row : rows) {
            Job j = row.job();
            sb.append(j.getJobId()).append(':').append(j.getStatus()).append(':')
                    .append(row.appliedApplicantCount()).append(':')
                    .append(row.targetApplicantCount()).append(':')
                    .append(sanitizeFingerprintPart(j.getTitle())).append(':')
                    .append(String.join(",", j.getCampuses())).append('#');
        }
        return sb.toString();
    }

    private static String sanitizeFingerprintPart(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.replace('|', ' ').replace('#', ' ');
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

        if (job == null) {
            detailTitle.setText("Select a job");
            detailSubtitle.setText("Choose a row to view job details.");
            detailBadges.getChildren().clear();
            clearOverviewFields();
            descriptionBody.setText("—");
            appliedCount.setText("0");
            reviewCount.setText("0");
            hiredCount.setText("0");
            return;
        }

        List<Application> applications = services.applicationService().getApplicationsByJob(job.getJobId());
        long applied = applications.stream()
                .filter(app -> app.getStatus() == ApplicationStatus.SUBMITTED || app.getStatus() == ApplicationStatus.UNDER_REVIEW)
                .count();
        long underReview = applications.stream().filter(app -> app.getStatus() == ApplicationStatus.UNDER_REVIEW).count();
        long hired = applications.stream().filter(app -> app.getStatus() == ApplicationStatus.ACCEPTED).count();

        detailTitle.setText(fallback(job.getTitle(), "Untitled Job"));
        detailSubtitle.setText(detailHeroSubtitle(job));
        populateOverviewFields(job, applications.size());
        detailBadges.getChildren().addAll(
                buildMiniChip(fallback(job.getModuleCode(), "MODULE"), "#eff6ff", "#2563eb"),
                buildJobTypeChip(job.getType()),
                buildStatusChip(job.getStatus())
        );
        String desc = job.getDescription();
        descriptionBody.setText(desc == null || desc.isBlank()
                ? "No description provided for this job."
                : desc.trim());

        appliedCount.setText(String.valueOf(applied));
        reviewCount.setText(String.valueOf(underReview));
        hiredCount.setText(String.valueOf(hired));

        applications.stream().limit(4).forEach(app -> avatarRow.getChildren().add(applicantAvatar(app)));
        if (avatarRow.getChildren().isEmpty()) {
            avatarRow.getChildren().add(styledLabel("No applicants yet", "-fx-font-size: 12px; -fx-font-weight: 400; -fx-text-fill: #94a3b8;"));
        }
    }

    private Label buildStatusChip(JobStatus status) {
        if (status == null) {
            return buildMiniChip("UNKNOWN", "#f1f5f9", "#64748b");
        }
        return switch (status) {
            case OPEN -> buildMiniChip("OPEN", "#ecfdf3", "#10b981");
            case DRAFT -> buildMiniChip("DRAFT", "#eff6ff", "#2563eb");
            case EXPIRED -> buildMiniChip("EXPIRED", "#fff7ed", "#f59e0b");
            case CLOSED -> buildMiniChip("CLOSED", "#f1f5f9", "#64748b");
        };
    }

    private Label buildJobTypeChip(JobType type) {
        if (type == null) {
            return buildMiniChip("TYPE", "#f8fafc", "#64748b");
        }
        String label = type.name().replace('_', ' ');
        return switch (type) {
            case MODULE_TA -> buildMiniChip(label, "#eff6ff", "#2563eb");
            case INVIGILATION -> buildMiniChip(label, "#fef3c7", "#b45309");
            case ACTIVITY_SUPPORT -> buildMiniChip(label, "#fce7f3", "#be185d");
            case OTHER -> buildMiniChip(label, "#f1f5f9", "#475569");
        };
    }

    private void clearOverviewFields() {
        ovModuleName.setText("—");
        ovModuleCode.setText("—");
        ovSemester.setText("—");
        ovJobType.setText("—");
        ovCampuses.setText("—");
        ovSlots.setText("—");
        ovJobId.setText("—");
        ovStatus.setText("—");
        ovCreated.setText("—");
        ovApplyBy.setText("—");
        ovApplications.setText("—");
    }

    private void populateOverviewFields(Job job, int applicationCount) {
        ovModuleName.setText(fallback(job.getModuleName(), "—"));
        ovModuleCode.setText(fallback(job.getModuleCode(), "—"));
        ovSemester.setText(fallback(job.getSemester(), "—"));
        ovJobType.setText(job.getType() == null ? "—" : job.getType().name().replace('_', ' '));
        ovCampuses.setText(job.getCampuses().isEmpty() ? "—" : String.join(", ", job.getCampuses()));
        int slots = Math.max(job.getPositions(), 0);
        ovSlots.setText(slots <= 0 ? "—" : String.valueOf(slots));
        ovJobId.setText(fallback(job.getJobId(), "—"));
        ovStatus.setText(job.getStatus() == null ? "—" : job.getStatus().name());
        ovCreated.setText(formatTimestamp(job.getCreatedAt()));
        ovApplyBy.setText(formatTimestamp(job.getDeadline()));
        ovApplications.setText(formatApplicationsSummary(applicationCount, slots));
    }

    private static String formatApplicationsSummary(int received, int slots) {
        if (slots <= 0) {
            return received + " received";
        }
        return received + " received · " + slots + " open slot" + (slots == 1 ? "" : "s");
    }

    private String detailHeroSubtitle(Job job) {
        String module = fallback(job.getModuleName(), "Module");
        String semester = fallback(job.getSemester(), "Semester TBD");
        return module + " · " + semester + " · Deadline " + formatTimestamp(job.getDeadline());
    }

    private Label buildMiniChip(String text, String background, String color) {
        Label chip = new Label(text);
        chip.setStyle("-fx-background-color: " + background + "; -fx-text-fill: " + color + "; -fx-font-size: 10px; -fx-font-weight: 900; -fx-background-radius: 999; -fx-padding: 4 9 4 9;");
        return chip;
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
        if (selectedJobId == null) {
            return null;
        }
        return services.jobService().getJobsByOrganiser(user.getUserId()).stream()
                .filter(job -> selectedJobId.equals(job.getJobId()))
                .findFirst()
                .orElse(null);
    }

    private String formatListDate(LocalDateTime time) {
        return time == null ? "-" : time.format(LIST_DATE_FORMAT);
    }

    private String formatTimestamp(LocalDateTime time) {
        return time == null ? "-" : time.format(DETAIL_TIME_FORMAT);
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
