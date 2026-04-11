package edu.bupt.ta.controller;

import edu.bupt.ta.dto.MatchExplanationDTO;
import edu.bupt.ta.enums.ApplicationStatus;
import edu.bupt.ta.enums.JobStatus;
import edu.bupt.ta.model.ApplicantProfile;
import edu.bupt.ta.model.Application;
import edu.bupt.ta.model.Job;
import edu.bupt.ta.model.ResumeInfo;
import edu.bupt.ta.model.User;
import edu.bupt.ta.service.ServiceRegistry;
import edu.bupt.ta.ui.IconFactory;
import edu.bupt.ta.util.ValidationResult;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class TADashboardController {

    private static final DateTimeFormatter DEADLINE_FORMAT = DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH);

    private final ServiceRegistry services;
    private final User user;
    private final BorderPane view = new BorderPane();

    private ApplicantProfile profile;
    private ResumeInfo resume;
    private String applicantId;
    private List<Application> applications = List.of();

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
        reloadData();

        VBox page = new VBox(22);
        page.setPadding(new Insets(24));
        page.getChildren().addAll(
                buildWelcome(),
                buildStatRow(),
                buildMainArea()
        );

        ScrollPane scrollPane = new ScrollPane(page);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setPannable(true);
        scrollPane.setVvalue(0.0);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");

        view.setCenter(scrollPane);
    }

    private void reloadData() {
        profile = services.applicantProfileService().getOrCreateProfile(user.getUserId());
        applicantId = profile.getApplicantId();
        resume = services.resumeService().getOrCreateResume(applicantId);
        applications = services.applicationService().getApplicationsByApplicant(applicantId);
    }

    private HBox buildWelcome() {
        int activeApplications = (int) applications.stream()
                .filter(app -> app.getStatus() == ApplicationStatus.SUBMITTED
                        || app.getStatus() == ApplicationStatus.UNDER_REVIEW
                        || app.getStatus() == ApplicationStatus.ACCEPTED)
                .count();

        Label title = new Label("Welcome back, " + resolveApplicantDisplayName() + " 👋");
        title.getStyleClass().add("page-title");

        Label subtitle = new Label("You have " + activeApplications + " active applications for this recruitment cycle.");
        subtitle.setStyle("-fx-font-size: 14px; -fx-font-weight: 400; -fx-text-fill: #64748b;");

        VBox left = new VBox(4, title, subtitle);

        Button viewSchedule = new Button("View Schedule");
        viewSchedule.getStyleClass().add("secondary-button");
        viewSchedule.setOnAction(event -> openApplicationsModal());

        Button browseJobs = new Button("Browse New Jobs");
        browseJobs.getStyleClass().add("primary-button");
        browseJobs.setOnAction(event -> openJobBrowserModal());

        HBox right = new HBox(12, viewSchedule, browseJobs);
        right.setAlignment(Pos.CENTER_RIGHT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(18, left, spacer, right);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private HBox buildStatRow() {
        int profileCompletion = services.applicantProfileService().calculateProfileCompletion(applicantId);
        int resumeCompletion = services.resumeService().calculateResumeCompletion(applicantId);
        int applicationCount = applications.size();
        boolean hasCv = resume.getCvFileName() != null && !resume.getCvFileName().isBlank();

        HBox row = new HBox(14,
                buildProfileCard(profileCompletion),
                buildResumeCard(resumeCompletion, hasCv),
                buildApplicationCard(applicationCount)
        );
        return row;
    }

    private VBox buildProfileCard(int completion) {
        VBox card = baseStatCard();
        HBox top = new HBox(14, progressRing(completion, "#10bfa4"), profileTextBlock(
                "PROFILE STATUS",
                completion >= 85 ? "Almost Done" : completion >= 60 ? "In Progress" : "Needs Attention",
                completion + "% complete"
        ));
        top.setAlignment(Pos.CENTER_LEFT);
        card.getChildren().add(top);
        return card;
    }

    private VBox buildResumeCard(int completion, boolean hasCv) {
        VBox card = baseStatCard();

        Label pill = buildMiniPill(hasCv ? "UPLOADED" : "INCOMPLETE", hasCv ? "success" : "warning");
        HBox header = metricHeader("CV STATUS", pill);

        Label file = new Label(hasCv ? resume.getCvFileName() : "Add your latest CV");
        file.setStyle("-fx-font-size: 18px; -fx-font-weight: 600; -fx-text-fill: #0f172a;");
        file.setWrapText(true);

        Label meta = new Label(hasCv ? completion + "% completion" : "Resume completion " + completion + "%");
        meta.setStyle("-fx-font-size: 12px; -fx-font-weight: 400; -fx-text-fill: #64748b;");

        card.getChildren().addAll(header, file, meta);
        return card;
    }

    private VBox buildWorkloadCard(int currentHours, int maxHours) {
        VBox card = baseStatCard();
        double ratio = Math.min(1.0, currentHours / (double) maxHours);

        Label pill = buildMiniPill(currentHours >= maxHours ? "FULL" : "BALANCED", currentHours >= maxHours ? "danger" : "success");
        HBox header = metricHeader("CURRENT WORKLOAD", pill);

        HBox valueRow = new HBox(4);
        Label current = new Label(String.valueOf(currentHours));
        current.setStyle("-fx-font-size: 30px; -fx-font-weight: 900; -fx-text-fill: #0f172a;");
        Label max = new Label("/ " + maxHours + " hrs/wk");
        max.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #94a3b8;");
        valueRow.getChildren().addAll(current, max);
        valueRow.setAlignment(Pos.BASELINE_LEFT);

        Rectangle track = new Rectangle(110, 6);
        track.setArcWidth(6);
        track.setArcHeight(6);
        track.setFill(Color.web("#dfe7f1"));
        Rectangle fill = new Rectangle(Math.max(10, 110 * ratio), 6);
        fill.setArcWidth(6);
        fill.setArcHeight(6);
        fill.setFill(Color.web(currentHours >= maxHours ? "#ef4444" : "#10bfa4"));
        StackPane progress = new StackPane(track);
        progress.setAlignment(Pos.CENTER_LEFT);
        progress.getChildren().add(fill);

        card.getChildren().addAll(header, valueRow, progress);
        return card;
    }

    private VBox buildApplicationCard(int applicationCount) {
        VBox card = baseStatCard();

        Label pill = buildMiniPill("ACTIVE", "neutral");
        HBox header = metricHeader("MY APPLICATIONS", pill);

        Label count = new Label(String.format("%02d", applicationCount));
        count.setStyle("-fx-font-size: 30px; -fx-font-weight: 900; -fx-text-fill: #0f172a;");

        Label meta = new Label(applicationCount == 0 ? "No submissions yet" : "Track current recruitment progress");
        meta.setStyle("-fx-font-size: 12px; -fx-font-weight: 400; -fx-text-fill: #64748b;");

        card.getChildren().addAll(header, count, meta);
        return card;
    }

    private VBox baseStatCard() {
        VBox card = new VBox(12);
        card.getStyleClass().add("panel-card");
        card.setPadding(new Insets(16, 18, 16, 18));
        card.setMinWidth(0);
        card.setPrefWidth(0);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private HBox metricHeader(String titleText, Label pill) {
        Label kicker = new Label(titleText);
        kicker.setStyle("-fx-font-size: 14px; -fx-font-weight: 900; -fx-text-fill: #64748b;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(10, kicker, spacer, pill);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setMinHeight(24);
        return header;
    }

    private HBox buildMainArea() {
        VBox left = new VBox(18, buildRecentJobsCard(), buildRecommendedCard());
        VBox right = new VBox(18, buildQuickActionsCard(), buildStatusCheckCard(), buildDeadlineCard());

        HBox.setHgrow(left, Priority.ALWAYS);
        right.setMinWidth(260);
        right.setPrefWidth(260);

        return new HBox(18, left, right);
    }

    private VBox buildRecentJobsCard() {
        VBox card = new VBox(12);
        card.getStyleClass().add("panel-card");
        card.setPadding(new Insets(18));

        Label title = new Label("Recent Open Jobs");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: 900; -fx-text-fill: #0f172a;");

        Button viewAll = new Button("View All");
        viewAll.setStyle("-fx-background-color: transparent; -fx-text-fill: #475569; -fx-font-size: 12px; -fx-font-weight: 600;");
        viewAll.setOnAction(event -> openJobBrowserModal());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(title, spacer, viewAll);
        header.setAlignment(Pos.CENTER_LEFT);

        TableView<JobSummaryRow> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setFixedCellSize(72);
        table.setPrefHeight(250);
        table.setPlaceholder(new Label("No recent jobs available."));

        TableColumn<JobSummaryRow, String> courseCol = new TableColumn<>("COURSE CODE");
        courseCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().job().getModuleCode()));
        courseCol.setCellFactory(column -> mutedTableCell());

        TableColumn<JobSummaryRow, JobSummaryRow> titleCol = new TableColumn<>("JOB TITLE");
        titleCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue()));
        titleCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(JobSummaryRow item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label title = new Label(item.job().getTitle());
                title.setWrapText(false);
                title.setTextOverrun(OverrunStyle.ELLIPSIS);
                title.setMinWidth(0);
                title.setMaxWidth(Double.MAX_VALUE);
                title.prefWidthProperty().bind(getTableColumn().widthProperty().subtract(26));
                title.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #1f2937;");

                Label meta = new Label(item.job().getWeeklyHours() + " hrs/week");
                meta.setStyle("-fx-font-size: 11px; -fx-font-weight: 400; -fx-text-fill: #94a3b8;");

                VBox box = new VBox(2, title, meta);
                setGraphic(box);
                setText(null);
            }
        });

        TableColumn<JobSummaryRow, String> deptCol = new TableColumn<>("DEPARTMENT");
        deptCol.setCellValueFactory(cell -> new SimpleStringProperty(fallback(cell.getValue().job().getModuleName(), "-")));
        deptCol.setCellFactory(column -> mutedTableCell());

        TableColumn<JobSummaryRow, JobSummaryRow> statusCol = new TableColumn<>("STATUS");
        statusCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue()));
        statusCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(JobSummaryRow item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label chip = buildMiniPill(item.statusLabel(), item.statusTone());
                setGraphic(chip);
                setText(null);
            }
        });

        TableColumn<JobSummaryRow, JobSummaryRow> actionCol = new TableColumn<>("ACTION");
        actionCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue()));
        actionCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(JobSummaryRow item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Button action = new Button(item.actionLabel());
                action.setStyle(item.primaryAction()
                        ? "-fx-background-color: #354a5f; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: 600; -fx-background-radius: 8; -fx-padding: 7 14 7 14;"
                        : "-fx-background-color: white; -fx-border-color: #cbd5e1; -fx-text-fill: #334155; -fx-font-size: 12px; -fx-font-weight: 600; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 7 14 7 14;");
                action.setOnAction(event -> handleRecentJobAction(item));
                setGraphic(action);
                setText(null);
            }
        });

        courseCol.setPrefWidth(130);
        titleCol.setPrefWidth(380);
        deptCol.setPrefWidth(260);
        statusCol.setPrefWidth(120);
        actionCol.setPrefWidth(140);
        statusCol.setStyle("-fx-alignment: CENTER;");
        actionCol.setStyle("-fx-alignment: CENTER;");

        table.getColumns().setAll(courseCol, titleCol, deptCol, statusCol, actionCol);

        List<JobSummaryRow> rows = services.jobService().searchJobs(null).stream()
                .filter(job -> job.getStatus() == JobStatus.OPEN)
                .sorted(Comparator.comparing(Job::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(3)
                .map(this::toJobSummaryRow)
                .toList();
        table.setItems(FXCollections.observableArrayList(rows));

        card.getChildren().addAll(header, table);
        return card;
    }

    private VBox buildRecommendedCard() {
        VBox card = new VBox(14);
        card.getStyleClass().add("panel-card");
        card.setPadding(new Insets(18));

        Label title = new Label("Recommended for You");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: 900; -fx-text-fill: #0f172a;");

        List<Job> recommendations = services.jobService().searchJobs(null).stream()
                .filter(job -> job.getStatus() == JobStatus.OPEN)
                .filter(job -> services.applicationService().getApplicationStatus(applicantId, job.getJobId()).isEmpty())
                .sorted(Comparator.comparingInt((Job job) -> recommendationScore(job)).reversed())
                .limit(2)
                .toList();

        HBox row = new HBox(14);
        if (recommendations.isEmpty()) {
            VBox empty = new VBox(6);
            empty.getStyleClass().add("soft-info-card");
            empty.setPadding(new Insets(18));
            Label emptyTitle = new Label("More recommendations will appear here");
            emptyTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #334155;");
            Label emptyMeta = new Label("Complete your profile and CV to improve recommendations.");
            emptyMeta.setWrapText(true);
            emptyMeta.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
            empty.getChildren().addAll(emptyTitle, emptyMeta);
            row.getChildren().add(empty);
        } else {
            recommendations.forEach(job -> row.getChildren().add(recommendationCard(job)));
        }

        card.getChildren().addAll(title, row);
        return card;
    }

    private VBox recommendationCard(Job job) {
        VBox box = new VBox(10);
        box.getStyleClass().add("soft-info-card");
        box.setPadding(new Insets(14));
        box.setPrefWidth(260);

        Label tag = new Label(fallback(job.getModuleCode(), "BUPT"));
        tag.getStyleClass().addAll("mini-pill", "mini-pill-neutral");

        Label title = new Label(job.getTitle());
        title.setWrapText(true);
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: 600; -fx-text-fill: #0f172a;");

        Label desc = new Label(fallback(job.getDescription(), "Recommended based on your current skills and workload balance."));
        desc.setWrapText(true);
        desc.setStyle("-fx-font-size: 12px; -fx-font-weight: 400; -fx-text-fill: #64748b;");

        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_LEFT);

        Label hours = new Label(job.getWeeklyHours() + "h/week");
        hours.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #334155;");

        Label deadline = new Label("Deadline: " + formatDate(job.getDeadline()));
        deadline.setStyle("-fx-font-size: 11px; -fx-font-weight: 400; -fx-text-fill: #94a3b8;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        footer.getChildren().addAll(hours, spacer, deadline);

        box.getChildren().addAll(tag, title, desc, footer);
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private VBox buildQuickActionsCard() {
        VBox card = new VBox(12);
        card.getStyleClass().add("accent-panel");
        card.setPadding(new Insets(18));

        Label title = new Label("Quick Actions");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: 900; -fx-text-fill: white;");

        Button profileButton = quickActionButton("Complete Profile", IconFactory.IconType.PENCIL);
        profileButton.setOnAction(event -> openProfileModal());

        HBox profileRow = new HBox(profileButton);
        HBox.setHgrow(profileButton, Priority.ALWAYS);
        profileRow.setAlignment(Pos.CENTER_LEFT);

        Button uploadCv = quickActionButton("Upload CV / Portfolio", IconFactory.IconType.UPLOAD);
        uploadCv.setOnAction(event -> openCvModal());

        Button browseJobs = quickActionButton("Browse Jobs", IconFactory.IconType.SEARCH);
        browseJobs.setOnAction(event -> openJobBrowserModal());

        Button viewApplications = quickActionButton("View Applications", IconFactory.IconType.CLIPBOARD);
        viewApplications.setOnAction(event -> openApplicationsModal());

        card.getChildren().addAll(title, profileRow, uploadCv, browseJobs, viewApplications);
        return card;
    }

    private Button quickActionButton(String text, IconFactory.IconType iconType) {
        Button button = new Button(text);
        button.getStyleClass().add("dashboard-action-button");
        button.setGraphic(IconFactory.glyph(iconType, 16, Color.WHITE));
        button.setGraphicTextGap(10);
        button.setContentDisplay(ContentDisplay.LEFT);
        button.setMaxWidth(Double.MAX_VALUE);
        return button;
    }

    private VBox buildStatusCheckCard() {
        VBox card = new VBox(12);
        card.getStyleClass().add("panel-card");
        card.setPadding(new Insets(18));

        Label title = new Label("Status Check");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: 900; -fx-text-fill: #0f172a;");

        VBox body = new VBox(10);
        boolean hasCv = resume.getCvFileName() != null && !resume.getCvFileName().isBlank();
        List<String> missingSections = services.resumeService().getMissingResumeSections(applicantId);
        int profileCompletion = services.applicantProfileService().calculateProfileCompletion(applicantId);
        int currentHours = services.workloadService().getWorkload(applicantId).currentHours();
        int maxHours = Math.max(services.workloadService().getWorkload(applicantId).maxWeeklyHours(), 1);

        if (!hasCv) {
            body.getChildren().add(alertBox("Missing Documents", "Please upload your CV or portfolio before applying widely.", "#fff1f2", "#ef4444"));
        }
        if (!missingSections.isEmpty()) {
            body.getChildren().add(alertBox("Profile Incomplete", "Resume sections missing: " + String.join(", ", missingSections), "#fff7ed", "#f59e0b"));
        } else if (profileCompletion < 100) {
            body.getChildren().add(alertBox("Profile Incomplete", "Update your applicant profile to reach full completion.", "#fff7ed", "#f59e0b"));
        }
        if (currentHours >= maxHours || currentHours * 1.0 / maxHours > 0.8) {
            body.getChildren().add(alertBox("Workload Warning", "Current workload is close to your weekly limit.", "#eff6ff", "#2563eb"));
        }
        if (body.getChildren().isEmpty()) {
            body.getChildren().add(alertBox("All Set", "Your documents and workload status look healthy.", "#ecfdf3", "#10b981"));
        }

        card.getChildren().addAll(title, body);
        return card;
    }

    private VBox alertBox(String title, String body, String background, String accent) {
        VBox box = new VBox(5);
        box.setPadding(new Insets(12));
        box.setStyle("-fx-background-color: " + background + "; -fx-border-color: transparent; -fx-background-radius: 10; -fx-border-radius: 10;");

        IconFactory.IconType iconType = switch (title) {
            case "All Set" -> IconFactory.IconType.CHECK_CIRCLE;
            case "Workload Warning" -> IconFactory.IconType.ALERT_TRIANGLE;
            case "Missing Documents" -> IconFactory.IconType.ALERT_TRIANGLE;
            default -> IconFactory.IconType.INFO_CIRCLE;
        };

        HBox titleRow = new HBox(6,
                IconFactory.glyph(iconType, 14, Color.web(accent)),
                new Label(title)
        );
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label titleNode = (Label) titleRow.getChildren().get(1);
        titleNode.setStyle("-fx-font-size: 12px; -fx-font-weight: 900; -fx-text-fill: " + accent + ";");

        Label bodyNode = new Label(body);
        bodyNode.setWrapText(true);
        bodyNode.setStyle("-fx-font-size: 11px; -fx-font-weight: 400; -fx-text-fill: " + accent + ";");

        box.getChildren().addAll(titleRow, bodyNode);
        return box;
    }

    private VBox buildDeadlineCard() {
        VBox card = new VBox(12);
        card.getStyleClass().add("panel-card");
        card.setPadding(new Insets(18));

        Label title = new Label("Recruitment Deadlines");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: 900; -fx-text-fill: #0f172a;");

        VBox body = new VBox(10);
        List<Job> deadlines = services.jobService().searchJobs(null).stream()
                .filter(job -> job.getStatus() == JobStatus.OPEN && job.getDeadline() != null)
                .sorted(Comparator.comparing(Job::getDeadline))
                .limit(2)
                .toList();

        if (deadlines.isEmpty()) {
            Label empty = new Label("No upcoming deadlines right now.");
            empty.setStyle("-fx-font-size: 12px; -fx-font-weight: 400; -fx-text-fill: #94a3b8;");
            body.getChildren().add(empty);
        } else {
            deadlines.forEach(job -> body.getChildren().add(deadlineRow(job)));
        }

        card.getChildren().addAll(title, body);
        return card;
    }

    private HBox deadlineRow(Job job) {
        String day = job.getDeadline() == null ? "--" : String.format("%02d", job.getDeadline().getDayOfMonth());
        String month = job.getDeadline() == null ? "---" : job.getDeadline().getMonth().name().substring(0, 3);

        VBox dateBox = new VBox(
                styledLabel(day, "-fx-font-size: 18px; -fx-font-weight: 900; -fx-text-fill: #0f172a;"),
                styledLabel(month, "-fx-font-size: 10px; -fx-font-weight: 600; -fx-text-fill: #94a3b8;")
        );
        dateBox.setAlignment(Pos.CENTER);
        dateBox.setMinWidth(42);

        Label title = new Label(job.getTitle());
        title.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #334155;");
        title.setWrapText(true);

        Label meta = new Label(fallback(job.getModuleCode(), "-") + " • " + formatDate(job.getDeadline()));
        meta.setStyle("-fx-font-size: 11px; -fx-font-weight: 400; -fx-text-fill: #94a3b8;");

        VBox body = new VBox(2, title, meta);
        return new HBox(12, dateBox, body);
    }

    private StackPane progressRing(int percent, String accent) {
        Circle track = new Circle(24);
        track.setFill(Color.TRANSPARENT);
        track.setStroke(Color.web("#dfe7f1"));
        track.setStrokeWidth(5);

        Arc progress = new Arc(0, 0, 24, 24, 90, -360 * (percent / 100.0));
        progress.setType(ArcType.OPEN);
        progress.setFill(Color.TRANSPARENT);
        progress.setStroke(Color.web(accent));
        progress.setStrokeWidth(5);
        progress.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);

        Label value = new Label(percent + "%");
        value.setStyle("-fx-font-size: 12px; -fx-font-weight: 900; -fx-text-fill: #0f172a;");

        return new StackPane(track, progress, value);
    }

    private VBox profileTextBlock(String kickerText, String titleText, String subtitleText) {
        Label kicker = new Label(kickerText);
        kicker.getStyleClass().add("tiny-kicker");

        Label title = new Label(titleText);
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: 900; -fx-text-fill: #0f172a;");

        Label subtitle = new Label(subtitleText);
        subtitle.setStyle("-fx-font-size: 12px; -fx-font-weight: 400; -fx-text-fill: #94a3b8;");

        return new VBox(3, kicker, title, subtitle);
    }

    private StackPane statIconBox(String text) {
        StackPane icon = new StackPane(new Label(text));
        icon.setMinSize(36, 36);
        icon.setPrefSize(36, 36);
        icon.setMaxSize(36, 36);
        icon.setStyle("-fx-background-color: #f3f7fb; -fx-background-radius: 10; -fx-font-size: 11px; -fx-font-weight: 900; -fx-text-fill: #64748b;");
        return icon;
    }

    private Label buildMiniPill(String text, String tone) {
        Label label = new Label(text);
        label.getStyleClass().add("mini-pill");
        switch (tone) {
            case "success" -> label.getStyleClass().add("mini-pill-success");
            case "warning" -> label.getStyleClass().add("mini-pill-warning");
            case "danger" -> label.getStyleClass().add("mini-pill-danger");
            case "info" -> label.getStyleClass().add("mini-pill-info");
            default -> label.getStyleClass().add("mini-pill-neutral");
        }
        return label;
    }

    private TableCell<JobSummaryRow, String> mutedTableCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                if (!empty) {
                    setStyle("-fx-text-fill: #64748b; -fx-font-weight: 600;");
                } else {
                    setStyle("");
                }
                setGraphic(null);
            }
        };
    }

    private JobSummaryRow toJobSummaryRow(Job job) {
        Optional<ApplicationStatus> statusOpt = services.applicationService().getApplicationStatus(applicantId, job.getJobId());
        if (statusOpt.isPresent()) {
            ApplicationStatus status = statusOpt.get();
            String label = switch (status) {
                case SUBMITTED, UNDER_REVIEW -> "APPLIED";
                case ACCEPTED -> "ACCEPTED";
                case REJECTED -> "REJECTED";
                case CANCELLED -> "OPEN";
            };
            String tone = switch (status) {
                case ACCEPTED -> "success";
                case REJECTED -> "danger";
                case SUBMITTED, UNDER_REVIEW -> "info";
                case CANCELLED -> "neutral";
            };
            String action = status == ApplicationStatus.CANCELLED ? "Apply" : "Details";
            boolean primary = status == ApplicationStatus.CANCELLED;
            return new JobSummaryRow(job, label, tone, action, primary);
        }
        return new JobSummaryRow(job, "OPEN", "neutral", "Apply", true);
    }

    private void handleRecentJobAction(JobSummaryRow row) {
        if (row.primaryAction()) {
            applyToJob(row.job());
        } else {
            openJobDetailModal(row.job());
        }
    }

    private void applyToJob(Job job) {
        Optional<String> statementOpt = JobApplyDialog.showAndWait(
                job,
                profile,
                services.resumeService(),
                applicantId,
                view.getScene() == null ? null : view.getScene().getWindow()
        );
        if (statementOpt.isEmpty()) {
            return;
        }

        ValidationResult result = services.applicationService().apply(applicantId, job.getJobId(), statementOpt.get());
        if (!result.isValid()) {
            DialogControllerFactory.operationFailed("Apply Failed", String.join("\n", result.getErrors()),
                    view.getScene() == null ? null : view.getScene().getWindow());
            return;
        }
        DialogControllerFactory.success("Application Submitted", "Your application was submitted successfully.",
                view.getScene() == null ? null : view.getScene().getWindow());
        initialize();
    }

    private int recommendationScore(Job job) {
        try {
            MatchExplanationDTO match = services.matchingService().evaluateMatch(applicantId, job.getJobId());
            return match == null ? 0 : match.score();
        } catch (Exception ignored) {
            return 0;
        }
    }

    private void openJobBrowserModal() {
        showModal("Browse Jobs", new JobBrowserController(services, user).getView(), 1320, 860);
    }

    private void openJobDetailModal(Job job) {
        JobDetailController detailCtrl = new JobDetailController(services);
        ApplicationStatus appStatus = services.applicationService().getApplicationStatus(applicantId, job.getJobId()).orElse(null);
        detailCtrl.setJobWithApplicationStatus(job, applicantId, appStatus);

        detailCtrl.setOnApply(() -> {
            applyToJob(job);
        });
        detailCtrl.setOnCancel(cancelledApplicantId -> {
            cancelApplication(job);
        });

        showModal("Job Detail", detailCtrl.getView(), 800, 820);
    }

    private void cancelApplication(Job job) {
        ValidationResult result = services.applicationService().cancelApplication(applicantId, job.getJobId());
        if (!result.isValid()) {
            DialogControllerFactory.operationFailed("Cancel Failed", String.join("\n", result.getErrors()),
                    view.getScene() == null ? null : view.getScene().getWindow());
            return;
        }
        DialogControllerFactory.success("Cancel Success", "Application cancelled successfully.",
                view.getScene() == null ? null : view.getScene().getWindow());
        initialize();
    }

    private void openApplicationsModal() {
        showModal("My Applications", new MyApplicationsController(services, user).getView(), 1180, 820);
    }

    private void openProfileModal() {
        showModal("Profile", new ApplicantProfileController(services, user).getView(), 1100, 760);
    }

    private void openCvModal() {
        showModal("My CV", new MyCvController(services, user).getView(), 1160, 860);
    }

    private void showModal(String title, Parent content, double width, double height) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        if (view.getScene() != null) {
            stage.initOwner(view.getScene().getWindow());
        }
        stage.setTitle(title);
        BorderPane modalRoot = new BorderPane();
        modalRoot.getStyleClass().add("app-surface");
        modalRoot.setCenter(content);
        Scene scene = new Scene(modalRoot, width, height);
        if (TADashboardController.class.getResource("/styles/app.css") != null) {
            scene.getStylesheets().add(TADashboardController.class.getResource("/styles/app.css").toExternalForm());
        }
        stage.setScene(scene);
        stage.showAndWait();
        initialize();
    }

    private Label styledLabel(String text, String style) {
        Label label = new Label(text);
        label.setStyle(style);
        return label;
    }

    private String formatDate(LocalDateTime dateTime) {
        return dateTime == null ? "-" : dateTime.format(DEADLINE_FORMAT).toUpperCase(Locale.ROOT);
    }

    private String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String resolveApplicantDisplayName() {
        if (profile != null && profile.getFullName() != null && !profile.getFullName().isBlank()) {
            return profile.getFullName().trim();
        }
        if (user.getDisplayName() != null && !user.getDisplayName().isBlank()) {
            return user.getDisplayName().trim();
        }
        return "Student";
    }

    private record JobSummaryRow(Job job, String statusLabel, String statusTone, String actionLabel, boolean primaryAction) {
    }
}
