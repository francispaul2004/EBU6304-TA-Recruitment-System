package edu.bupt.ta.controller;

import edu.bupt.ta.model.ApplicantProfile;
import edu.bupt.ta.model.ResumeInfo;
import edu.bupt.ta.model.User;
import edu.bupt.ta.service.ServiceRegistry;
import edu.bupt.ta.util.DateTimeUtils;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import javafx.stage.FileChooser;

public class MyCvController {

    private static final DateTimeFormatter UPDATED_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm");

    private final ServiceRegistry services;
    private final User user;
    private final Runnable browseJobsAction;
    private final BorderPane view = new BorderPane();
    private final ScrollPane scrollPane = new ScrollPane();
    private final String applicantId;
    private VBox pageRoot;
    private final VBox editorSection = new VBox(16);

    public MyCvController(ServiceRegistry services, User user) {
        this(services, user, () -> {
        });
    }

    public MyCvController(ServiceRegistry services, User user, Runnable browseJobsAction) {
        this.services = services;
        this.user = user;
        this.applicantId = services.applicantProfileService().getOrCreateProfile(user.getUserId()).getApplicantId();
        this.browseJobsAction = browseJobsAction == null ? () -> {
        } : browseJobsAction;
        initialize();
    }

    public Parent getView() {
        return view;
    }

    private void initialize() {
        view.getStyleClass().add("app-surface");
        editorSection.setManaged(false);
        editorSection.setVisible(false);
        editorSection.setMaxWidth(Double.MAX_VALUE);

        ApplicantProfile profile = services.applicantProfileService().getOrCreateProfile(user.getUserId());
        ResumeInfo resume = services.resumeService().getOrCreateResume(applicantId);
        int resumeCompletion = services.resumeService().calculateResumeCompletion(applicantId);

        pageRoot = new VBox(24);
        pageRoot.getStyleClass().add("cv-page");
        pageRoot.setPadding(new Insets(32, 64, 32, 64));
        pageRoot.setFillWidth(true);
        pageRoot.setMaxWidth(896);

        pageRoot.getChildren().add(buildTitleBlock());
        pageRoot.getChildren().add(buildBasicInfoCard(profile, resume, resumeCompletion));
        pageRoot.getChildren().add(buildActionColumns(profile, resume, resumeCompletion));
        pageRoot.getChildren().add(editorSection);
        pageRoot.getChildren().add(buildGuidelineCard());

        StackPane pageShell = new StackPane(pageRoot);
        StackPane.setAlignment(pageRoot, Pos.TOP_CENTER);

        scrollPane.setContent(pageShell);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");

        view.setCenter(scrollPane);
    }

    private VBox buildTitleBlock() {
        Label heading = new Label("CV Management");
        heading.getStyleClass().add("page-title");

        Label subtitle = new Label("Upload and manage your curriculum vitae to apply for Teaching Assistant positions.");
        subtitle.getStyleClass().add("body-muted");
        subtitle.setStyle("-fx-font-size: 16px;");

        VBox titleBlock = new VBox(4, heading, subtitle);
        titleBlock.setMaxWidth(Double.MAX_VALUE);
        return titleBlock;
    }

    private VBox buildBasicInfoCard(ApplicantProfile profile, ResumeInfo resume, int resumeCompletion) {
        VBox card = new VBox(0);
        card.getStyleClass().add("cv-card");
        card.setMaxWidth(Double.MAX_VALUE);

        HBox header = new HBox();
        header.getStyleClass().add("cv-basic-card-header");
        header.setPadding(new Insets(16, 24, 16, 24));
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Basic Information");
        title.getStyleClass().add("cv-card-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label more = new Label("...");
        more.getStyleClass().add("cv-card-menu");

        header.getChildren().addAll(title, spacer, more);

        VBox body = new VBox(24);
        body.setPadding(new Insets(24));

        HBox row1 = new HBox(24,
                infoCell("FULL NAME", safe(profile.getFullName())),
                infoCell("STUDENT ID", safe(profile.getStudentId())),
                infoCell("DEGREE PROGRAM", safe(profile.getProgramme()))
        );

        HBox row2 = new HBox(24,
                infoCell("SCHOOL EMAIL", safe(profile.getEmail())),
                infoCell("CV COMPLETION", resumeCompletion + "% complete"),
                infoCell("PHONE", safe(profile.getPhone()))
        );

        HBox row3 = new HBox(24,
                infoCell("CURRENT CAMPUS", safe(profile.getCurrentCampus())),
                infoCell("WILLING TO CROSS CAMPUS", yesNo(profile.getWillingToCrossCampus())),
                infoCell("ACADEMIC YEAR", profile.getYear() > 0 ? String.valueOf(profile.getYear()) : "-")
        );

        body.getChildren().addAll(row1, row2, row3);
        card.getChildren().addAll(header, body);
        return card;
    }

    private HBox buildActionColumns(ApplicantProfile profile, ResumeInfo resume, int resumeCompletion) {
        VBox uploadCard = buildUploadCard();
        VBox statusCard = buildStatusCard(profile, resume, resumeCompletion);

        HBox row = new HBox(32, uploadCard, statusCard);
        row.setFillHeight(true);
        HBox.setHgrow(uploadCard, Priority.ALWAYS);
        HBox.setHgrow(statusCard, Priority.ALWAYS);
        return row;
    }

    private VBox buildUploadCard() {
        VBox card = new VBox(20);
        card.getStyleClass().add("cv-card");
        card.getStyleClass().add("cv-upload-card");
        card.setPadding(new Insets(24));
        card.setPrefHeight(470);
        card.setMinWidth(420);
        card.setMaxWidth(Double.MAX_VALUE);

        VBox header = new VBox(4);
        Label title = new Label("Upload New CV");
        title.getStyleClass().add("cv-card-heading");

        Label subtitle = new Label("Supported formats: PDF, DOCX (Max 10MB)");
        subtitle.getStyleClass().add("cv-card-subtitle");

        header.getChildren().addAll(title, subtitle);

        StackPane dropZone = new StackPane();
        dropZone.getStyleClass().add("cv-upload-zone");
        dropZone.setMinHeight(340);
        dropZone.setPrefHeight(340);
        dropZone.setMinWidth(372);
        dropZone.setMaxWidth(Double.MAX_VALUE);

        VBox dropContent = new VBox(12);
        dropContent.setAlignment(Pos.CENTER);
        dropContent.setMaxWidth(360);

        StackPane uploadIcon = iconBubble("↑", "cv-drop-icon", "cv-drop-icon-label");
        uploadIcon.setPrefSize(64, 64);
        uploadIcon.setMinSize(64, 64);
        uploadIcon.setMaxSize(64, 64);

        Label prompt = new Label("Click to upload or drag and drop");
        prompt.getStyleClass().add("cv-upload-copy");
        prompt.setWrapText(true);
        prompt.setMaxWidth(320);

        Label helper = new Label("Your file will be automatically parsed for your profile");
        helper.getStyleClass().add("cv-upload-hint");
        helper.setWrapText(true);
        helper.setMaxWidth(320);

        Button selectFile = new Button("Select File");
        selectFile.getStyleClass().add("cv-primary-button");
        selectFile.setOnAction(event -> handleUploadCv());

        dropContent.getChildren().addAll(uploadIcon, prompt, helper, selectFile);
        dropZone.getChildren().add(dropContent);

        card.getChildren().addAll(header, dropZone);
        return card;
    }

    private VBox buildStatusCard(ApplicantProfile profile, ResumeInfo resume, int resumeCompletion) {
        VBox card = new VBox(20);
        card.getStyleClass().add("cv-card");
        card.getStyleClass().add("cv-status-card");
        card.setPadding(new Insets(32));
        card.setPrefHeight(414);
        card.setMaxWidth(Double.MAX_VALUE);

        HBox statusHeader = new HBox(16);
        statusHeader.setAlignment(Pos.CENTER_LEFT);

        StackPane badge = iconBubble("✓", "cv-status-badge", "cv-status-badge-label");
        badge.setPrefSize(48, 48);
        badge.setMinSize(48, 48);
        badge.setMaxSize(48, 48);

        VBox headerCopy = new VBox(2);
        boolean hasUploadedCv = hasUploadedCv(resume);
        Label statusTitle = new Label(hasUploadedCv ? "CV Uploaded Successfully" : "CV In Progress");
        statusTitle.getStyleClass().add("cv-status-title");

        Label statusSubtitle = new Label(hasUploadedCv ? "Verification Complete" : "Awaiting Completion");
        statusSubtitle.getStyleClass().add("cv-status-subtitle");

        headerCopy.getChildren().addAll(statusTitle, statusSubtitle);
        statusHeader.getChildren().addAll(badge, headerCopy);

        HBox fileRow = new HBox(12);
        fileRow.getStyleClass().add("cv-file-row");
        fileRow.setAlignment(Pos.CENTER_LEFT);
        fileRow.setPadding(new Insets(21));

        StackPane fileIcon = iconBubble("PDF", "cv-file-icon", "cv-file-icon-label");
        fileIcon.setPrefSize(40, 40);
        fileIcon.setMinSize(40, 40);
        fileIcon.setMaxSize(40, 40);

        VBox fileCopy = new VBox(2);
        fileCopy.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(fileCopy, Priority.ALWAYS);
        Label fileTitle = new Label(buildResumeLabel(profile, resume));
        fileTitle.getStyleClass().add("cv-file-title");

        Label fileMeta = new Label(buildResumeMeta(resume, resumeCompletion));
        fileMeta.getStyleClass().add("cv-file-meta");

        fileCopy.getChildren().addAll(fileTitle, fileMeta);
        Button openFile = new Button("👁");
        openFile.getStyleClass().add("secondary-button");
        openFile.setTooltip(new Tooltip("Open CV file"));
        openFile.setOnAction(event -> openCvFile());
        openFile.setDisable(!hasUploadedCv);

        Button deleteFile = new Button("🗑");
        deleteFile.getStyleClass().add("danger-outline");
        deleteFile.setTooltip(new Tooltip("Delete CV file"));
        deleteFile.setOnAction(event -> deleteCvFile());
        deleteFile.setDisable(!hasUploadedCv);

        fileRow.getChildren().addAll(fileIcon, fileCopy, openFile, deleteFile);

        VBox nextSteps = new VBox(16);

        Label nextStepsTitle = new Label("NEXT STEPS");
        nextStepsTitle.getStyleClass().add("cv-step-section-title");

        VBox stepList = new VBox(12);
        stepList.getChildren().addAll(
                stepButton("Browse Available Positions", "↗", () -> browseJobsAction.run()),
                stepButton("Complete Profile Details", "✎", this::showProfileEditor)
        );

        nextSteps.getChildren().addAll(nextStepsTitle, stepList);
        card.getChildren().addAll(statusHeader, fileRow, nextSteps);
        return card;
    }

    private VBox buildGuidelineCard() {
        VBox card = new VBox(0);
        card.getStyleClass().add("cv-guideline-card");
        card.setPadding(new Insets(25));
        card.setMaxWidth(Double.MAX_VALUE);

        HBox row = new HBox(16);
        row.setAlignment(Pos.TOP_LEFT);

        StackPane icon = iconBubble("i", "cv-guideline-icon", "cv-guideline-icon-label");
        icon.setPrefSize(24, 24);
        icon.setMinSize(24, 24);
        icon.setMaxSize(24, 24);

        VBox copy = new VBox(8);
        copy.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(copy, Priority.ALWAYS);
        Label title = new Label("CV Guidelines for Applicants");
        title.getStyleClass().add("cv-guideline-title");

        VBox lines = new VBox(8);
        lines.setMaxWidth(Double.MAX_VALUE);
        List<String> guidance = List.of(
                "Ensure your GPA and relevant course grades are clearly visible.",
                "List any previous teaching assistant or research assistant experience.",
                "Include your proficiency in English and any other required languages for the specific course.",
                "Keep the file size under 10MB to ensure smooth processing by our automated system."
        );

        for (String line : guidance) {
            Label item = new Label(line);
            item.getStyleClass().add("cv-guideline-line");
            item.setWrapText(true);
            lines.getChildren().add(item);
        }

        copy.getChildren().addAll(title, lines);
        row.getChildren().addAll(icon, copy);
        card.getChildren().add(row);
        return card;
    }

    private VBox infoCell(String label, String value) {
        VBox cell = new VBox(4);
        cell.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(cell, Priority.ALWAYS);

        Label name = new Label(label);
        name.getStyleClass().add("cv-meta-label");

        Label data = new Label(value);
        data.getStyleClass().add("cv-meta-value");
        data.setWrapText(true);

        cell.getChildren().addAll(name, data);
        return cell;
    }

    private Button stepButton(String text, String glyph, Runnable action) {
        Button button = new Button();
        button.getStyleClass().add("cv-step-button");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setMinHeight(50);
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        button.setOnAction(event -> {
            if (action != null) {
                action.run();
            }
        });

        StackPane icon = iconBubble(glyph, "cv-step-icon", "cv-step-icon-label");
        icon.setPrefSize(20, 20);
        icon.setMinSize(20, 20);
        icon.setMaxSize(20, 20);

        Label label = new Label(text);
        label.getStyleClass().add("cv-step-label");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label arrow = new Label(">");
        arrow.getStyleClass().add("cv-step-arrow");

        HBox row = new HBox(12, icon, label, spacer, arrow);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMaxWidth(Double.MAX_VALUE);

        button.setGraphic(row);
        return button;
    }

    private StackPane iconBubble(String glyph, String bubbleStyleClass, String glyphStyleClass) {
        StackPane bubble = new StackPane();
        bubble.getStyleClass().add(bubbleStyleClass);

        Label label = new Label(glyph);
        label.getStyleClass().add(glyphStyleClass);

        bubble.getChildren().add(label);
        return bubble;
    }

    private void showProfileEditor() {
        showEditor("Edit Basic Information", new ApplicantProfileController(services, user).getView());
    }

    private void showEditor(String titleText, Parent editorView) {
        editorSection.getChildren().clear();
        editorSection.setManaged(true);
        editorSection.setVisible(true);

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label(titleText);
        title.getStyleClass().add("section-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button close = new Button("Close");
        close.getStyleClass().add("secondary-button");
        close.setOnAction(event -> hideEditor());

        header.getChildren().addAll(title, spacer, close);

        editorSection.getChildren().addAll(header, editorView);
        scrollEditorIntoView();
    }

    private void hideEditor() {
        editorSection.getChildren().clear();
        editorSection.setVisible(false);
        editorSection.setManaged(false);
    }

    private void scrollEditorIntoView() {
        Platform.runLater(() -> {
            if (pageRoot == null || scrollPane.getContent() == null || !editorSection.isVisible()) {
                return;
            }
            Bounds viewportBounds = scrollPane.getViewportBounds();
            double viewportHeight = viewportBounds == null ? 0 : viewportBounds.getHeight();
            double contentHeight = pageRoot.getBoundsInLocal().getHeight();
            double maxScroll = Math.max(1, contentHeight - viewportHeight);
            double targetY = editorSection.getBoundsInParent().getMinY();
            scrollPane.setVvalue(Math.max(0, Math.min(1, targetY / maxScroll)));
        });
    }

    private String buildResumeMeta(ResumeInfo resume, int resumeCompletion) {
        LocalDateTime updated = resume.getCvUploadedAt() == null ? resume.getLastUpdated() : resume.getCvUploadedAt();
        String updatedText = updated == null ? "Not updated yet" : formatUpdated(updated);
        if (hasUploadedCv(resume)) {
            return readableSize(resume.getCvFileSizeBytes()) + " • " + updatedText;
        }
        return resumeCompletion + "% complete • " + updatedText;
    }

    private String formatUpdated(LocalDateTime updated) {
        long minutes = ChronoUnit.MINUTES.between(updated, DateTimeUtils.now());
        if (minutes < 1) {
            return "Updated just now";
        }
        if (minutes < 60) {
            return "Updated " + minutes + " mins ago";
        }
        if (minutes < 24 * 60) {
            long hours = ChronoUnit.HOURS.between(updated, DateTimeUtils.now());
            if (hours <= 1) {
                return "Updated 1 hour ago";
            }
            return "Updated " + hours + " hours ago";
        }
        return "Updated " + UPDATED_FORMAT.format(updated);
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String yesNo(Boolean value) {
        if (value == null) {
            return "-";
        }
        return value ? "Yes" : "No";
    }

    private void handleUploadCv() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select CV File");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("CV files", "*.pdf", "*.docx"),
                new FileChooser.ExtensionFilter("PDF files", "*.pdf"),
                new FileChooser.ExtensionFilter("DOCX files", "*.docx")
        );
        Path selected = Optional.ofNullable(chooser.showOpenDialog(
                        view.getScene() == null ? null : view.getScene().getWindow()))
                .map(java.io.File::toPath)
                .orElse(null);
        if (selected == null) {
            return;
        }
        var result = services.resumeService().uploadCvFile(applicantId, selected);
        if (!result.isValid()) {
            DialogControllerFactory.validationError(String.join("\n", result.getErrors()),
                    view.getScene() == null ? null : view.getScene().getWindow());
            return;
        }
        DialogControllerFactory.success("CV Uploaded", "CV uploaded successfully.",
                view.getScene() == null ? null : view.getScene().getWindow());
        reloadPage();
    }

    private void openCvFile() {
        Optional<Path> filePath = services.resumeService().getCvFilePath(applicantId);
        if (filePath.isEmpty()) {
            DialogControllerFactory.info("CV Not Found", "No uploaded CV file exists for this account.",
                    view.getScene() == null ? null : view.getScene().getWindow());
            reloadPage();
            return;
        }
        try {
            if (!Desktop.isDesktopSupported()) {
                DialogControllerFactory.operationFailed("Open CV Failed",
                        "Desktop open action is not supported in this environment.",
                        view.getScene() == null ? null : view.getScene().getWindow());
                return;
            }
            Desktop.getDesktop().open(filePath.get().toFile());
        } catch (IOException e) {
            DialogControllerFactory.operationFailed("Open CV Failed",
                    "Unable to open file: " + e.getMessage(),
                    view.getScene() == null ? null : view.getScene().getWindow());
        }
    }

    private void deleteCvFile() {
        boolean confirmed = DialogControllerFactory.confirmAction("Delete CV",
                "Are you sure you want to remove the uploaded CV file?",
                view.getScene() == null ? null : view.getScene().getWindow());
        if (!confirmed) {
            return;
        }
        var result = services.resumeService().deleteCvFile(applicantId);
        if (!result.isValid()) {
            DialogControllerFactory.operationFailed("Delete CV Failed", String.join("\n", result.getErrors()),
                    view.getScene() == null ? null : view.getScene().getWindow());
            return;
        }
        DialogControllerFactory.success("CV Deleted", "The uploaded CV file has been removed.",
                view.getScene() == null ? null : view.getScene().getWindow());
        reloadPage();
    }

    private void reloadPage() {
        hideEditor();
        initialize();
    }

    private boolean hasUploadedCv(ResumeInfo resume) {
        return resume.getCvStoredPath() != null && !resume.getCvStoredPath().isBlank()
                && resume.getCvFileName() != null && !resume.getCvFileName().isBlank();
    }

    private String buildResumeLabel(ApplicantProfile profile, ResumeInfo resume) {
        if (hasUploadedCv(resume)) {
            return resume.getCvFileName();
        }
        String name = safe(profile.getFullName()).replaceAll("\\s+", "_");
        if (name.equals("-")) {
            return "Structured_CV.pdf";
        }
        return name + "_CV.pdf";
    }

    private String readableSize(long bytes) {
        if (bytes <= 0) {
            return "0 KB";
        }
        if (bytes < 1024 * 1024) {
            return Math.max(1, bytes / 1024) + " KB";
        }
        return String.format("%.1f MB", bytes / 1024.0 / 1024.0);
    }
}
