package edu.bupt.ta.controller;

import edu.bupt.ta.dto.MatchExplanationDTO;
import edu.bupt.ta.enums.ApplicationStatus;
import edu.bupt.ta.enums.JobStatus;
import edu.bupt.ta.model.Job;
import edu.bupt.ta.model.User;
import edu.bupt.ta.service.ServiceRegistry;
import edu.bupt.ta.ui.IconFactory;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class JobDetailController {
    private static final DateTimeFormatter CARD_DEADLINE_FORMAT = DateTimeFormatter.ofPattern("MMM dd", Locale.ENGLISH);
    private final ServiceRegistry services;

    private final ScrollPane view = new ScrollPane();
    private final VBox content = new VBox(40);

    private final HBox warningBanner = new HBox(36);
    private final Label warningTitle = new Label("Action Required: Profile Incomplete");
    private final Label warningBody = new Label("Please complete your profile and upload your CV to apply for this position.");
    private final Button completeProfileButton = new Button("Complete Profile");

    private final VBox detailCard = new VBox(40);
    private final Label titleLabel = new Label("Select a position");
    private final Label subtitleLabel = new Label("-");
    private final Label organiserNameLabel = new Label("-");
    private final Label organiserDeptLabel = new Label("-");
    private final Label organiserAvatarLabel = new Label("U");

    private final Label seatsMetricValue = new Label("-");
    private final Label deadlineMetricValue = new Label("-");

    private final Label descLabel = new Label("Choose a job card on the left to preview details.");
    private final VBox responsibilitiesList = new VBox(8);

    private final Label infoCodeValue = new Label("-");
    private final Label infoProfessorValue = new Label("-");
    private final Label infoCampusValue = new Label("-");
    private final Label infoTermValue = new Label("-");

    private final Button applyButton = new Button("APPLY NOW");

    private Job currentJob;
    private String currentApplicantId;
    private ApplicationStatus currentApplicationStatus;
    private Runnable onApplyAction;
    private Consumer<String> onCancel;

    public JobDetailController(ServiceRegistry services) {
        this.services = services;
        initialize();
    }

    public Parent getView() {
        return view;
    }

    public void setOnApply(Runnable onApplyAction) {
        this.onApplyAction = onApplyAction;
    }

    public void setOnCancel(Consumer<String> onCancel) {
        this.onCancel = onCancel;
    }

    public void setJob(Job job) {
        setJobWithApplicationStatus(job, null, null);
    }

    public void setJobWithApplicationStatus(Job job, String applicantId, ApplicationStatus appStatus) {
        this.currentJob = job;
        this.currentApplicantId = applicantId;
        this.currentApplicationStatus = appStatus;

        if (job == null) {
            showEmptyState();
            return;
        }

        warningBanner.setVisible(applicantId == null || applicantId.isBlank());
        warningBanner.setManaged(applicantId == null || applicantId.isBlank());

        titleLabel.setText(job.getTitle() == null || job.getTitle().isBlank() ? "-" : job.getTitle());
        subtitleLabel.setText(buildSubtitle(job));
        organiserNameLabel.setText(resolveOrganiserName(job));
        organiserDeptLabel.setText(resolveOrganiserDepartment(job));
        organiserAvatarLabel.setText(initialsFromName(organiserNameLabel.getText()));

        seatsMetricValue.setText(job.getPositions() + " Posts");
        deadlineMetricValue.setText(formatMetricDeadline(job.getDeadline()));

        descLabel.setText(job.getDescription() == null || job.getDescription().isBlank()
                ? "No job description provided."
                : job.getDescription());

        populateResponsibilities(job);
        populateModuleInfo(job);
        updateApplyButton();
        view.setVvalue(0);
    }

    public void setMatchExplanation(MatchExplanationDTO dto) {
        // Kept for compatibility with caller flow.
    }

    private void initialize() {
        view.setFitToWidth(true);
        view.setPannable(true);
        view.getStyleClass().add("position-detail-scroll");
        view.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        content.getStyleClass().add("position-detail-root");
        content.setSpacing(24);
        content.setPadding(new Insets(24));
        content.setFillWidth(true);
        view.setContent(content);

        buildWarningBanner();
        buildDetailCard();
        content.getChildren().addAll(warningBanner, detailCard);

        showEmptyState();
    }

    private void buildWarningBanner() {
        warningBanner.getStyleClass().add("position-warning-banner");
        warningBanner.setAlignment(Pos.CENTER_LEFT);
        warningBanner.setPadding(new Insets(21));

        Label warningIcon = new Label("!");
        warningIcon.getStyleClass().add("position-warning-icon");

        warningTitle.getStyleClass().add("position-warning-title");
        warningBody.getStyleClass().add("position-warning-body");
        warningBody.setWrapText(true);
        VBox warningCopy = new VBox(4, warningTitle, warningBody);
        HBox.setHgrow(warningCopy, Priority.ALWAYS);

        completeProfileButton.getStyleClass().add("position-warning-button");
        completeProfileButton.setOnAction(event -> {
            // UI-only action for this panel.
        });

        warningBanner.getChildren().addAll(warningIcon, warningCopy, completeProfileButton);
    }

    private void buildDetailCard() {
        detailCard.getStyleClass().add("position-detail-card");
        detailCard.setPadding(new Insets(28));
        detailCard.setFillWidth(true);
        detailCard.setMinWidth(0);

        HBox header = buildHeader();
        HBox metrics = buildMetrics();
        VBox description = section("Job Description", buildDescriptionBlock());
        VBox responsibilities = section("Key Responsibilities", buildResponsibilitiesBlock());
        VBox moduleInfo = section("Module Info", buildModuleInfoBlock());
        Region divider = new Region();
        divider.getStyleClass().add("position-detail-divider");
        divider.setPrefHeight(1);
        divider.setMinHeight(1);
        divider.setMaxHeight(1);

        applyButton.getStyleClass().add("position-apply-button");
        applyButton.setMaxWidth(Double.MAX_VALUE);
        applyButton.setMinWidth(0);
        applyButton.setPrefHeight(44);
        applyButton.setOnAction(event -> {
            if (onApplyAction != null && currentJob != null) {
                onApplyAction.run();
            }
        });

        detailCard.getChildren().addAll(header, metrics, description, responsibilities, moduleInfo, divider, applyButton);
    }

    private HBox buildHeader() {
        Label heart = new Label("♡");
        heart.getStyleClass().add("position-favorite-icon");
        HBox favButton = new HBox(heart);
        favButton.getStyleClass().add("position-favorite-button");
        favButton.setAlignment(Pos.CENTER);
        favButton.setMinSize(52, 52);
        favButton.setPrefSize(52, 52);
        favButton.setMaxSize(52, 52);

        titleLabel.getStyleClass().add("position-detail-title");
        titleLabel.setWrapText(true);

        subtitleLabel.getStyleClass().add("position-detail-subtitle");
        subtitleLabel.setWrapText(true);

        organiserAvatarLabel.getStyleClass().add("position-organiser-avatar");
        organiserAvatarLabel.setMinSize(46, 46);
        organiserAvatarLabel.setPrefSize(46, 46);
        organiserAvatarLabel.setMaxSize(46, 46);

        organiserNameLabel.getStyleClass().add("position-organiser-name");
        organiserDeptLabel.getStyleClass().add("position-organiser-dept");
        organiserNameLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
        organiserDeptLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
        VBox organiserCopy = new VBox(6, organiserNameLabel, organiserDeptLabel);
        HBox organiserRow = new HBox(16, organiserAvatarLabel, organiserCopy);
        organiserRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(organiserCopy, Priority.ALWAYS);

        VBox left = new VBox(16, titleLabel, subtitleLabel, organiserRow);
        left.setPrefWidth(0);
        left.setMinWidth(0);
        HBox.setHgrow(left, Priority.ALWAYS);

        HBox header = new HBox(16, left, favButton);
        header.setAlignment(Pos.TOP_LEFT);
        return header;
    }

    private HBox buildMetrics() {
        VBox seats = metricCard("AVAILABLE", "SEATS", seatsMetricValue, IconFactory.IconType.USERS);
        VBox deadline = metricCard("APPLICATION", "DEADLINE", deadlineMetricValue, IconFactory.IconType.CALENDAR);

        HBox row = new HBox(14, seats, deadline);
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(seats, Priority.ALWAYS);
        HBox.setHgrow(deadline, Priority.ALWAYS);
        return row;
    }

    private VBox metricCard(String topLabel, String bottomLabel, Label valueLabel, IconFactory.IconType iconType) {
        Label labelTop = new Label(topLabel);
        labelTop.getStyleClass().add("position-metric-kicker");
        Label labelBottom = new Label(bottomLabel);
        labelBottom.getStyleClass().add("position-metric-kicker");
        VBox kicker = new VBox(0, labelTop, labelBottom);

        valueLabel.getStyleClass().add("position-metric-value");
        valueLabel.setMinWidth(0);
        valueLabel.setMaxWidth(Double.MAX_VALUE);
        valueLabel.setTextOverrun(OverrunStyle.ELLIPSIS);

        HBox valueRow = new HBox(6, IconFactory.glyph(iconType, 15, Color.web("#00c29f")), valueLabel);
        valueRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(valueLabel, Priority.ALWAYS);

        VBox card = new VBox(6, kicker, valueRow);
        card.getStyleClass().add("position-metric-card");
        card.setPadding(new Insets(12, 14, 12, 14));
        card.setMinHeight(104);
        card.setMinWidth(0);
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private VBox section(String title, VBox body) {
        Region accent = new Region();
        accent.getStyleClass().add("position-section-accent");
        accent.setPrefSize(6, 24);
        accent.setMinSize(6, 24);
        accent.setMaxSize(6, 24);

        Label heading = new Label(title.toUpperCase(Locale.ENGLISH));
        heading.getStyleClass().add("position-section-heading");

        HBox top = new HBox(12, accent, heading);
        top.setAlignment(Pos.CENTER_LEFT);

        VBox block = new VBox(16, top, body);
        return block;
    }

    private VBox buildDescriptionBlock() {
        descLabel.getStyleClass().add("position-desc-text");
        descLabel.setWrapText(true);
        descLabel.setMinWidth(0);
        descLabel.setMaxWidth(Double.MAX_VALUE);

        VBox box = new VBox(descLabel);
        box.getStyleClass().add("position-section-card");
        box.setPadding(new Insets(20));
        return box;
    }

    private VBox buildResponsibilitiesBlock() {
        responsibilitiesList.getStyleClass().add("position-responsibilities-list");

        VBox box = new VBox(responsibilitiesList);
        box.getStyleClass().add("position-section-card");
        box.setPadding(new Insets(20));
        return box;
    }

    private VBox buildModuleInfoBlock() {
        GridPane grid = new GridPane();
        grid.setHgap(24);
        grid.setVgap(16);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setHgrow(Priority.ALWAYS);
        c1.setPercentWidth(50);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setHgrow(Priority.ALWAYS);
        c2.setPercentWidth(50);
        grid.getColumnConstraints().setAll(c1, c2);

        grid.add(infoCell("Code", infoCodeValue), 0, 0);
        grid.add(infoCell("Professor", infoProfessorValue), 1, 0);
        grid.add(infoCell("Campus", infoCampusValue), 0, 1);
        grid.add(infoCell("Term", infoTermValue), 1, 1);

        VBox box = new VBox(grid);
        box.getStyleClass().add("position-module-card");
        box.setPadding(new Insets(20));
        return box;
    }

    private VBox infoCell(String key, Label value) {
        Label keyLabel = new Label(key.toUpperCase(Locale.ENGLISH));
        keyLabel.getStyleClass().add("position-info-key");
        value.getStyleClass().add("position-info-value");
        value.setWrapText(true);
        value.setMinWidth(0);
        value.setMaxWidth(Double.MAX_VALUE);
        VBox cell = new VBox(2, keyLabel, value);
        cell.setMinWidth(0);
        HBox.setHgrow(cell, Priority.ALWAYS);
        return cell;
    }

    private void populateResponsibilities(Job job) {
        responsibilitiesList.getChildren().clear();

        List<String> items = extractResponsibilities(job);
        for (String line : items) {
            Label tick = new Label("✓");
            tick.getStyleClass().add("position-check-icon");
            Label text = new Label(line);
            text.getStyleClass().add("position-responsibility-text");
            text.setWrapText(true);

            HBox row = new HBox(12, tick, text);
            row.setAlignment(Pos.TOP_LEFT);
            HBox.setHgrow(text, Priority.ALWAYS);
            responsibilitiesList.getChildren().add(row);
        }
    }

    private void populateModuleInfo(Job job) {
        int year = job.getDeadline() == null ? LocalDateTime.now().getYear() : job.getDeadline().getYear();
        infoCodeValue.setText((safe(job.getModuleCode()) + "-" + year).replace("--", "-"));
        infoProfessorValue.setText(resolveOrganiserName(job));
        infoCampusValue.setText("Shahe Campus");
        infoTermValue.setText(resolveTerm(job));
    }

    private void showEmptyState() {
        warningBanner.setVisible(false);
        warningBanner.setManaged(false);

        titleLabel.setText("Select a position");
        subtitleLabel.setText("-");
        organiserNameLabel.setText("-");
        organiserDeptLabel.setText("-");
        organiserAvatarLabel.setText("U");
        seatsMetricValue.setText("-");
        deadlineMetricValue.setText("-");
        descLabel.setText("Choose a job card on the left to preview details.");

        responsibilitiesList.getChildren().clear();
        responsibilitiesList.getChildren().add(
                responsibilityRow("Select an open position to view responsibilities and requirements.")
        );

        infoCodeValue.setText("-");
        infoProfessorValue.setText("-");
        infoCampusValue.setText("-");
        infoTermValue.setText("-");

        applyButton.setDisable(true);
        applyButton.setText("APPLY NOW");
        applyButton.getStyleClass().setAll("button", "position-apply-button");
        applyButton.setOnAction(null);
    }

    private HBox responsibilityRow(String textValue) {
        Label tick = new Label("✓");
        tick.getStyleClass().add("position-check-icon");
        Label text = new Label(textValue);
        text.getStyleClass().add("position-responsibility-text");
        text.setWrapText(true);
        HBox row = new HBox(12, tick, text);
        row.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(text, Priority.ALWAYS);
        return row;
    }

    private void updateApplyButton() {
        applyButton.getStyleClass().setAll("button", "position-apply-button");

        if (currentApplicationStatus == ApplicationStatus.ACCEPTED) {
            applyButton.setText("ACCEPTED");
            applyButton.setDisable(true);
            applyButton.getStyleClass().add("position-apply-button-disabled");
            applyButton.setOnAction(null);
            return;
        }

        if (currentApplicationStatus == ApplicationStatus.SUBMITTED) {
            applyButton.setText("CANCEL APPLICATION");
            applyButton.setDisable(false);
            applyButton.getStyleClass().add("position-apply-button-danger");
            applyButton.setOnAction(event -> {
                if (onCancel != null && currentJob != null) {
                    onCancel.accept(currentApplicantId);
                }
            });
            return;
        }

        if (currentJob.getStatus() != JobStatus.OPEN) {
            applyButton.setText(currentJob.getStatus().name());
            applyButton.setDisable(true);
            applyButton.getStyleClass().add("position-apply-button-disabled");
            applyButton.setOnAction(null);
            return;
        }

        applyButton.setText("APPLY NOW");
        applyButton.setDisable(false);
        applyButton.setOnAction(event -> {
            if (onApplyAction != null && currentJob != null) {
                onApplyAction.run();
            }
        });
    }

    private static String buildSubtitle(Job job) {
        String department = safe(job.getModuleName()).toUpperCase(Locale.ENGLISH);
        String semester = normalizeTerm(resolveTerm(job)).toUpperCase(Locale.ENGLISH);
        return department + " • " + semester;
    }

    private static String resolveTerm(Job job) {
        if (job.getSemester() != null && !job.getSemester().isBlank()) {
            return job.getSemester();
        }
        int year = job.getDeadline() == null ? LocalDateTime.now().getYear() : job.getDeadline().getYear();
        int month = job.getDeadline() == null ? 1 : job.getDeadline().getMonthValue();
        String season = month >= 8 ? "Fall Semester" : "Spring Semester";
        return season + " " + year;
    }

    private String resolveOrganiserName(Job job) {
        if (job.getOrganiserId() == null || job.getOrganiserId().isBlank()) {
            return "-";
        }
        return services.userRepository()
                .findById(job.getOrganiserId())
                .map(User::getDisplayName)
                .filter(name -> name != null && !name.isBlank())
                .orElse(job.getOrganiserId());
    }

    private static String resolveOrganiserDepartment(Job job) {
        if (job.getModuleName() == null || job.getModuleName().isBlank()) {
            return "Department";
        }
        return job.getModuleName() + " Department";
    }

    private static String normalizeTerm(String term) {
        if (term == null || term.isBlank()) {
            return "-";
        }
        return term.replace('_', ' ').replace('-', ' ').replaceAll("\\s+", " ").trim();
    }

    private static String initialsFromName(String name) {
        if (name == null || name.isBlank()) {
            return "U";
        }
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase(Locale.ENGLISH);
        }
        return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase(Locale.ENGLISH);
    }

    private static List<String> extractResponsibilities(Job job) {
        String source = job.getDescription();
        if (source == null || source.isBlank()) {
            return List.of(
                    "Assist in weekly teaching sessions and student consultations.",
                    "Support grading and provide timely, constructive feedback.",
                    "Coordinate with the course organiser on teaching tasks."
            );
        }

        String[] raw = source.replace('\n', '.').split("[.;]");
        List<String> items = new ArrayList<>();
        for (String part : raw) {
            String cleaned = part.trim();
            if (!cleaned.isEmpty()) {
                items.add(cleaned.endsWith(".") ? cleaned : cleaned + ".");
            }
            if (items.size() == 3) {
                break;
            }
        }
        if (items.isEmpty()) {
            items.add("Support course activities and scheduled teaching duties.");
        }
        return items;
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private static String formatMetricDeadline(LocalDateTime deadline) {
        return deadline == null ? "-" : deadline.format(CARD_DEADLINE_FORMAT);
    }
}
