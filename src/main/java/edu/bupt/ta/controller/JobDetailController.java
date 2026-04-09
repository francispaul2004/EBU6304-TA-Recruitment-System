package edu.bupt.ta.controller;

import edu.bupt.ta.dto.MatchExplanationDTO;
import edu.bupt.ta.enums.ApplicationStatus;
import edu.bupt.ta.enums.JobStatus;
import edu.bupt.ta.model.Job;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

public class JobDetailController {
    private static final DateTimeFormatter DEADLINE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");


    private final VBox view = new VBox(16);
    private final Label titleLabel = new Label("Select a job");
    private final Label moduleLabel = new Label("-");
    private final Label metaHours = chip("Hours: -");
    private final Label metaPositions = chip("Seats: -");
    private final Label metaDeadline = chip("Deadline: -");
    private final Label descLabel = new Label("Choose a job card on the left to preview details.");
    private final Label matchLabel = new Label("AI Match: -");
    private final Label missingLabel = new Label("Missing Skills: -");
    private final Label workloadLabel = new Label("Projected Workload: -");
    private final Button applyButton = new Button("APPLY NOW");

    private Job currentJob;
    private String currentApplicantId;
    private ApplicationStatus currentApplicationStatus;
    private Runnable onApplyAction;
    private Consumer<String> onCancel;

    public JobDetailController() {
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
            titleLabel.setText("Select a job");
            moduleLabel.setText("-");
            metaHours.setText("Hours: -");
            metaPositions.setText("Seats: -");
            metaDeadline.setText("Deadline: -");
            descLabel.setText("Choose a job card on the left to preview details.");
            setMatchExplanation(null);
            applyButton.setDisable(true);
            applyButton.setText("APPLY NOW");
            applyButton.getStyleClass().setAll("button", "primary-button");
            return;
        }

        titleLabel.setText(job.getTitle());
        moduleLabel.setText(job.getModuleCode() + "  |  " + job.getModuleName());
        metaHours.setText("Hours: " + job.getWeeklyHours() + "h/week");
        metaPositions.setText("Seats: " + job.getPositions());
        metaDeadline.setText("Deadline: " + formatDeadline(job.getDeadline()));
        descLabel.setText(job.getDescription());

        updateApplyButton();
    }

    private void updateApplyButton() {
        if (currentApplicationStatus == ApplicationStatus.ACCEPTED) {
            applyButton.setText("ACCEPTED");
            applyButton.setDisable(true);
            applyButton.getStyleClass().setAll("button", "secondary-button");
            applyButton.setStyle("-fx-background-color: #e2e8f0; -fx-text-fill: #64748b; -fx-font-weight: 700;");
            applyButton.setOnAction(null);
            return;
        }

        if (currentApplicationStatus == ApplicationStatus.SUBMITTED) {
            applyButton.setText("CANCEL APPLICATION");
            applyButton.setDisable(false);
            applyButton.getStyleClass().setAll("button", "danger-button");
            applyButton.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: 700;");
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
            applyButton.getStyleClass().setAll("button", "secondary-button");
            applyButton.setStyle("-fx-background-color: #e2e8f0; -fx-text-fill: #64748b; -fx-font-weight: 700;");
            applyButton.setOnAction(null);
            return;
        }

        applyButton.setText("APPLY NOW");
        applyButton.setDisable(false);
        applyButton.getStyleClass().setAll("button", "primary-button");
        applyButton.setStyle("");
        applyButton.setOnAction(event -> {
            if (onApplyAction != null && currentJob != null) {
                onApplyAction.run();
            }
        });
    }

    public void setMatchExplanation(MatchExplanationDTO dto) {
        if (dto == null) {
            matchLabel.setText("AI Match: -");
            missingLabel.setText("Missing Skills: -");
            workloadLabel.setText("Projected Workload: -");
            matchLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #334155;");
            return;
        }

        matchLabel.setText("AI Match: " + dto.score() + "%");
        missingLabel.setText("Missing Skills: " + (dto.missingSkills().isEmpty() ? "None" : String.join(", ", dto.missingSkills())));
        workloadLabel.setText("Projected Workload: " + dto.projectedWorkload() + "h/week");
        if (dto.score() >= 80) {
            matchLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #059669;");
        } else {
            matchLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #b45309;");
        }
    }

    private void initialize() {
        view.setPadding(new Insets(24));
        view.getStyleClass().add("panel-card");
        view.setMinWidth(430);

        titleLabel.setStyle("-fx-font-size: 42px; -fx-font-weight: 800; -fx-text-fill: #0f172a;");
        titleLabel.setWrapText(true);

        moduleLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #00c29f;");

        HBox metaRow = new HBox(10, metaHours, metaPositions, metaDeadline);

        Label sectionDesc = new Label("Job Description");
        sectionDesc.setStyle("-fx-font-size: 12px; -fx-font-weight: 800; -fx-text-fill: #334155; -fx-letter-spacing: 0.5px;");

        descLabel.setWrapText(true);
        descLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #334155; -fx-line-spacing: 2px;");

        VBox matchPanel = new VBox(8, matchLabel, missingLabel, workloadLabel);
        matchPanel.setPadding(new Insets(14));
        matchPanel.getStyleClass().add("soft-card");

        applyButton.getStyleClass().add("primary-button");
        applyButton.setPrefHeight(44);
        applyButton.setMaxWidth(Double.MAX_VALUE);
        applyButton.setAlignment(Pos.CENTER);
        applyButton.setOnAction(event -> {
            if (onApplyAction != null && currentJob != null) {
                onApplyAction.run();
            }
        });

        VBox.setVgrow(descLabel, Priority.ALWAYS);
        view.getChildren().addAll(
                titleLabel,
                moduleLabel,
                metaRow,
                sectionDesc,
                descLabel,
                matchPanel,
                applyButton
        );
    }

    private static Label chip(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #475569; -fx-background-color: #f8fafc; -fx-border-color: #e2e8f0; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 8 10 8 10;");
        return label;
    }

    private String formatDeadline(LocalDateTime deadline) {
        return deadline == null ? "-" : deadline.format(DEADLINE_FORMAT);
    }
}
