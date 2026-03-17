package edu.bupt.ta.controller;

import edu.bupt.ta.dto.ApplicantReviewDTO;
import edu.bupt.ta.model.User;
import edu.bupt.ta.service.ServiceRegistry;
import edu.bupt.ta.util.ValidationResult;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class ApplicantReviewController {

    private final ServiceRegistry services;
    private final User user;
    private final String applicationId;

    private final VBox view = new VBox(10);
    private final TextArea decisionNote = new TextArea();

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
        ApplicantReviewDTO dto = services.reviewService().getApplicantReviewData(applicationId, user.getUserId());

        view.setPadding(new Insets(16));

        Label title = new Label("Applicant Review");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: 800; -fx-text-fill: #0F172A;");

        Label applicant = new Label("Applicant: " + dto.applicantName() + " (" + dto.applicantId() + ")");
        Label skills = new Label("Technical Skills: " + String.join(", ", dto.technicalSkills()));
        Label availability = new Label("Availability: " + String.join(", ", dto.availability()));
        Label match = new Label("Match Score: " + dto.matchScore() + "%");
        Label missing = new Label("Missing Skills: " + String.join(", ", dto.missingSkills()));

        Label workload = new Label("Workload: current " + dto.currentHours() + "h, projected " + dto.projectedHours()
                + "h / max " + dto.maxWeeklyHours() + "h  (" + dto.riskLevel() + ")");
        workload.setStyle(dto.riskLevel().equals("HIGH")
                ? "-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #EF4444;"
                : "-fx-font-size: 13px; -fx-text-fill: #334155;");

        Label statement = new Label("Statement: " + dto.statement());
        statement.setWrapText(true);

        Label noteLabel = new Label("Decision Note");
        decisionNote.setPrefRowCount(3);

        Button accept = new Button("Accept");
        accept.getStyleClass().add("primary-button");
        accept.setOnAction(event -> doAccept());

        Button reject = new Button("Reject");
        reject.getStyleClass().add("secondary-button");
        reject.setOnAction(event -> doReject());

        HBox actions = new HBox(10, accept, reject);
        view.getChildren().addAll(title, applicant, skills, availability, match, missing, workload, statement,
                noteLabel, decisionNote, actions);
    }

    private void doAccept() {
        ValidationResult result = services.reviewService().acceptApplication(applicationId, user.getUserId(), decisionNote.getText());
        showResult("Accept Application", result);
    }

    private void doReject() {
        ValidationResult result = services.reviewService().rejectApplication(applicationId, user.getUserId(), decisionNote.getText());
        showResult("Reject Application", result);
    }

    private void showResult(String header, ValidationResult result) {
        if (!result.isValid()) {
            Alert alert = new Alert(Alert.AlertType.ERROR, String.join("\n", result.getErrors()));
            alert.setHeaderText(header + " failed");
            alert.showAndWait();
            return;
        }
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Operation completed.");
        alert.setHeaderText(header + " success");
        alert.showAndWait();
    }
}
