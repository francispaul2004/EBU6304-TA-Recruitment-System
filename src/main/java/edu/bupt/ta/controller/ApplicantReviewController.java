package edu.bupt.ta.controller;

import edu.bupt.ta.dto.ApplicantReviewDTO;
import edu.bupt.ta.enums.Role;
import edu.bupt.ta.model.User;
import edu.bupt.ta.service.ServiceRegistry;
import edu.bupt.ta.util.ValidationResult;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.awt.Desktop;
import java.nio.file.Path;
import java.util.Optional;

public class ApplicantReviewController {

    private final ServiceRegistry services;
    private final User user;
    private final String applicationId;

    private final VBox view = new VBox(14);
    private final TextArea decisionNote = new TextArea();
    private ApplicantReviewDTO reviewData;

    public ApplicantReviewController(ServiceRegistry services, User user, String applicationId) {
        this.services = services;
        this.user = user;
        this.applicationId = applicationId;
        initialize();
    }

    public Parent getView() {
        return view;
    }

    private void initialize() {
        ApplicantReviewDTO dto = services.reviewService().getApplicantReviewData(applicationId, user.getUserId(), isAdmin());
        this.reviewData = dto;

        view.setPadding(new Insets(16));
        view.getStyleClass().add("app-surface");

        Label title = new Label(dto.applicantName());
        title.setStyle("-fx-font-size: 40px; -fx-font-weight: 800; -fx-text-fill: #0f172a;");

        Label subtitle = new Label("Applicant ID: " + dto.applicantId());
        subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b;");

        VBox basicInfo = new VBox(8);
        basicInfo.getStyleClass().add("panel-card");
        basicInfo.setPadding(new Insets(14));

        basicInfo.getChildren().addAll(
                info("Technical Skills", String.join(", ", dto.technicalSkills())),
                info("Availability", String.join(", ", dto.availability())),
                info("Match Score", dto.matchScore() + "%"),
            info("Matched Skills", safeJoin(dto.matchedSkills())),
                info("Missing Skills", dto.missingSkills().isEmpty() ? "None" : String.join(", ", dto.missingSkills())),
            info("Match Explanation", blankToDash(dto.matchExplanation())),
                info("Workload", "Current " + dto.currentHours() + "h, Projected " + dto.projectedHours()
                        + "h / Max " + dto.maxWeeklyHours() + "h (" + dto.riskLevel() + ")"),
                info("Statement", dto.statement())
        );

        VBox noteCard = new VBox(8);
        noteCard.getStyleClass().add("panel-card");
        noteCard.setPadding(new Insets(14));

        Label noteLabel = new Label("Decision Note");
        noteLabel.getStyleClass().add("field-label");

        decisionNote.setPromptText("Add observation or justification for the recruitment decision...");
        decisionNote.setPrefRowCount(4);
        decisionNote.setText(dto.decisionNote() == null ? "" : dto.decisionNote());

        Button previewCv = new Button("Preview CV");
        previewCv.getStyleClass().add("secondary-button");
        previewCv.setOnAction(event -> openCvFile(dto.applicantId()));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox noteHeader = new HBox(8, noteLabel, spacer, previewCv);
        noteHeader.setAlignment(Pos.CENTER_LEFT);

        noteCard.getChildren().addAll(noteHeader, decisionNote);

        Button accept = new Button("Accept Candidate");
        accept.getStyleClass().add("primary-button");
        accept.setOnAction(event -> doAccept());
        accept.setMaxWidth(Double.MAX_VALUE);

        Button reject = new Button("Reject Candidate");
        reject.getStyleClass().add("danger-outline");
        reject.setOnAction(event -> doReject());
        reject.setMaxWidth(Double.MAX_VALUE);

        HBox actions = new HBox(12, accept, reject);

        view.getChildren().addAll(title, subtitle, basicInfo, noteCard, actions);
    }

    private VBox info(String title, String value) {
        VBox box = new VBox(2);
        Label t = new Label(title);
        t.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #94a3b8;");
        Label v = new Label(value == null || value.isBlank() ? "-" : value);
        v.setWrapText(true);
        v.setStyle("-fx-font-size: 13px; -fx-text-fill: #334155;");
        box.getChildren().addAll(t, v);
        return box;
    }

    private String safeJoin(java.util.List<String> items) {
        if (items == null || items.isEmpty()) {
            return "None";
        }
        return String.join(", ", items);
    }

    private String blankToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private void doAccept() {
        if ("HIGH".equalsIgnoreCase(reviewData.riskLevel())) {
            DialogControllerFactory.workloadWarning(
                    "Projected hours: " + reviewData.projectedHours() + "h / Max " + reviewData.maxWeeklyHours() + "h.",
                    view.getScene() == null ? null : view.getScene().getWindow());
        }
        boolean confirmed = DialogControllerFactory.confirmAction(
                "Accept Candidate",
                "Accept this applicant and update workload records?",
                view.getScene() == null ? null : view.getScene().getWindow());
        if (!confirmed) {
            return;
        }
        ValidationResult result = services.reviewService()
                .acceptApplication(applicationId, user.getUserId(), decisionNote.getText(), isAdmin());
        showResult("Accept Application", result);
    }

    private void doReject() {
        boolean confirmed = DialogControllerFactory.confirmAction(
                "Reject Candidate",
                "Reject this applicant for the selected job?",
                view.getScene() == null ? null : view.getScene().getWindow());
        if (!confirmed) {
            return;
        }
        ValidationResult result = services.reviewService()
                .rejectApplication(applicationId, user.getUserId(), decisionNote.getText(), isAdmin());
        showResult("Reject Application", result);
    }

    private void showResult(String header, ValidationResult result) {
        if (!result.isValid()) {
            DialogControllerFactory.operationFailed(header, String.join("\n", result.getErrors()),
                    view.getScene() == null ? null : view.getScene().getWindow());
            return;
        }
        DialogControllerFactory.success(header, "Operation completed.",
                view.getScene() == null ? null : view.getScene().getWindow());
        if (view.getScene() != null && view.getScene().getWindow() != null) {
            view.getScene().getWindow().hide();
        }
    }

    private boolean isAdmin() {
        return user.getRole() == Role.ADMIN;
    }

    private void openCvFile(String applicantId) {
        Optional<Path> filePath = services.resumeService().getCvFilePath(applicantId);
        if (filePath.isEmpty()) {
            DialogControllerFactory.info("CV Not Found",
                    "No uploaded CV file exists for this account.",
                    view.getScene() == null ? null : view.getScene().getWindow());
            return;
        }
        try {
            if (!Desktop.isDesktopSupported()) {
                DialogControllerFactory.operationFailed("Open CV Failed",
                        "Desktop open action is not supported in this environment.",
                        view.getScene() == null ? null : view.getScene().getWindow());
                return;
            }
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                desktop.browse(filePath.get().toUri());
            } else {
                desktop.open(filePath.get().toFile());
            }
        } catch (Exception ex) {
            DialogControllerFactory.operationFailed("Open CV Failed",
                    "Unable to open file: " + ex.getMessage(),
                    view.getScene() == null ? null : view.getScene().getWindow());
        }
    }
}
